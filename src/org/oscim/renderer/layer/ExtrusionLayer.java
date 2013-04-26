/*
 * Copyright 2012, 2013 Hannes Janetzek
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.oscim.renderer.layer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

import org.oscim.core.GeometryBuffer;
import org.oscim.core.MapElement;
import org.oscim.core.Tile;
import org.oscim.renderer.BufferObject;
import org.oscim.renderer.GLRenderer;
import org.oscim.utils.LineClipper;
import org.oscim.view.MapView;

import android.opengl.GLES20;
import android.util.Log;

/**
 * @author Hannes Janetzek
 *         FIXME check if polygon has self intersections or 0/180 degree
 *         angles! or bad things might happen in Triangle
 */
public class ExtrusionLayer extends Layer {
	private final static String TAG = ExtrusionLayer.class.getName();
	private static final float S = GLRenderer.COORD_SCALE;
	private final VertexItem mVertices;
	private VertexItem mCurVertices;
	private final VertexItem mIndices[], mCurIndices[];
	private LineClipper mClipper;

	// indices for:
	// 0. even sides, 1. odd sides, 2. roof, 3. roof outline
	public int mIndiceCnt[] = { 0, 0, 0, 0 };
	public int mNumIndices = 0;
	public int mNumVertices = 0;

	public int mIndicesBufferID;
	public int mVertexBufferID;
	private BufferObject mIndiceBO;
	private BufferObject mVertexBO;

	//private final static int IND_EVEN_SIDE = 0;
	//private final static int IND_ODD_SIDE = 1;
	private final static int IND_ROOF = 2;
	private final static int IND_OUTLINE = 3;

	public boolean compiled = false;
	private final float mGroundResolution;

	public ExtrusionLayer(int level, float groundResolution) {
		this.type = Layer.EXTRUSION;
		this.level = level;

		mGroundResolution = groundResolution;
		mVertices = mCurVertices = VertexItem.pool.get();

		mIndices = new VertexItem[4];
		mCurIndices = new VertexItem[4];
		for (int i = 0; i < 4; i++)
			mIndices[i] = mCurIndices[i] = VertexItem.pool.get();

		mClipper = new LineClipper(0, 0, Tile.SIZE, Tile.SIZE);
	}

	public void addBuildings(MapElement element) {

		short[] index = element.index;
		float[] points = element.points;

		float height = element.height;
		float minHeight = element.minHeight;

		// 12m default
		if (height == 0)
			height = 12 * 100;

		// 10 cm steps
		float sfactor = 1 / 10f;
		height *= sfactor;
		minHeight *= sfactor;

		// match height with ground resultion
		// (meter per pixel)
		height /= mGroundResolution;
		minHeight /= mGroundResolution;

		// my preference
		height *= 0.85;
		minHeight *= 0.85;

		int length = 0, ipos = 0, ppos = 0;

		boolean complexOutline = false;
		int geomIndexPos = 0;
		int geomPointPos = 0;

		boolean simpleOutline = true;

		// current vertex id
		int startVertex = mNumVertices;

		for (int n = index.length; ipos < n; ipos++, ppos += length) {
			length = index[ipos];

			// end marker
			if (length < 0)
				break;

			// start next polygon
			if (length == 0) {
				if (complexOutline)
					addRoof(startVertex, element, geomIndexPos, geomPointPos);

				startVertex = mNumVertices;
				simpleOutline = true;
				complexOutline = false;
				continue;
			}

			// check: drop last point from explicitly closed rings
			int len = length;
			if (!MapView.enableClosePolygons) {
				len -= 2;
			} else if (points[ppos] == points[ppos + len - 2]
					&& points[ppos + 1] == points[ppos + len - 1]) {
				// vector-tile-map does not produce implicty closed
				// polygons (yet)
				len -= 2;
			}

			// need at least three points
			if (len < 6)
				continue;

			// check if polygon contains inner rings
			if (simpleOutline && (ipos < n - 1) && (index[ipos + 1] > 0))
				simpleOutline = false;

			boolean convex = addOutline(points, ppos, len, minHeight, height, simpleOutline);

			if (simpleOutline && (convex || len <= 8))
				addRoofSimple(startVertex, len);
			else if (!complexOutline)  {
				complexOutline = true;
				// keep start postion of polygon and defer roof building
				// as it modifies the geometry array.
				geomIndexPos = ipos;
				geomPointPos = ppos;
			}
		}
		if (complexOutline)
			addRoof(startVertex, element, geomIndexPos, geomPointPos);
	}

	private void addRoofSimple(int startVertex, int len) {

		// roof indices for convex shapes
		int i = mCurIndices[IND_ROOF].used;
		short[] indices = mCurIndices[IND_ROOF].vertices;
		short first = (short) (startVertex + 1);

		for (int k = 0; k < len - 4; k += 2) {
			if (i == VertexItem.SIZE) {
				mCurIndices[IND_ROOF].used = VertexItem.SIZE;
				mCurIndices[IND_ROOF].next = VertexItem.pool.get();
				mCurIndices[IND_ROOF] = mCurIndices[2].next;
				indices = mCurIndices[IND_ROOF].vertices;
				i = 0;
			}
			indices[i++] = first;
			indices[i++] = (short) (first + k + 2);
			indices[i++] = (short) (first + k + 4);
		}
		mCurIndices[IND_ROOF].used = i;
	}

	private void addRoof(int startVertex, GeometryBuffer geom, int ipos, int ppos) {
		short[] index = geom.index;
		float[] points = geom.points;

		int len = 0;
		int rings = 0;

		// get sum of points in polygon
		for (int i = ipos, n = index.length; i < n && index[i] > 0; i++) {
			len += index[i];
			rings++;
		}

		// triangulate up to 600 points (limited only by prepared buffers)
		// some buildings in paris have even more...
		if (len > 1200) {
			Log.d(TAG, ">>> skip building : " + len + " <<<");
			return;
		}

		int used = triangulate(points, ppos, len, index, ipos, rings,
				startVertex + 1, mCurIndices[IND_ROOF]);

		if (used > 0) {
			// get back to the last item added..
			VertexItem it = mIndices[IND_ROOF];
			while (it.next != null)
				it = it.next;
			mCurIndices[IND_ROOF] = it;
		}
	}

	private boolean addOutline(float[] points, int pos, int len, float minHeight, float height,
			boolean convex) {

		// add two vertices for last face to make zigzag indices work
		boolean addFace = (len % 4 != 0);
		int vertexCnt = len + (addFace ? 2 : 0);

		short h = (short) height;
		short mh = (short) minHeight;

		float cx = points[pos + len - 2];
		float cy = points[pos + len - 1];
		float nx = points[pos + 0];
		float ny = points[pos + 1];

		// vector to next point
		float vx = nx - cx;
		float vy = ny - cy;
		// vector from previous point
		float ux, uy;

		float a = (float) Math.sqrt(vx * vx + vy * vy);
		short color1 = (short) ((1 + vx / a) * 127);
		short fcolor = color1;
		short color2 = 0;

		int even = 0;
		int changeX = 0;
		int changeY = 0;

		// vertex offset for all vertices in layer
		int vOffset = mNumVertices;

		short[] vertices = mCurVertices.vertices;
		int v = mCurVertices.used;

		mClipper.clipStart((int) nx, (int) ny);

		for (int i = 2, n = vertexCnt + 2; i < n; i += 2, v += 8) {
			cx = nx;
			cy = ny;

			ux = vx;
			uy = vy;

			/* add bottom and top vertex for each point */
			if (v == VertexItem.SIZE) {
				mCurVertices.used = VertexItem.SIZE;
				mCurVertices.next = VertexItem.pool.get();
				mCurVertices = mCurVertices.next;
				vertices = mCurVertices.vertices;
				v = 0;
			}

			// set coordinate
			vertices[v + 0] = vertices[v + 4] = (short) (cx * S);
			vertices[v + 1] = vertices[v + 5] = (short) (cy * S);

			// set height
			vertices[v + 2] = mh;
			vertices[v + 6] = h;

			// get direction to next point
			if (i < len) {
				nx = points[pos + i + 0];
				ny = points[pos + i + 1];
			} else if (i == len) {
				nx = points[pos + 0];
				ny = points[pos + 1];
			} else { // if (addFace)
				short c = (short) (color1 | fcolor << 8);
				vertices[v + 3] = vertices[v + 7] = c;
				v += 8;
				break;
			}

			vx = nx - cx;
			vy = ny - cy;

			// set lighting (by direction)
			a = (float) Math.sqrt(vx * vx + vy * vy);
			color2 = (short) ((1 + vx / a) * 127);

			short c;
			if (even == 0)
				c = (short) (color1 | color2 << 8);
			else
				c = (short) (color2 | color1 << 8);

			vertices[v + 3] = vertices[v + 7] = c;
			color1 = color2;

			/* check if polygon is convex */
			if (convex) {
				// TODO simple polys with only one concave arc
				// could be handled without special triangulation
				if ((ux < 0 ? 1 : -1) != (vx < 0 ? 1 : -1))
					changeX++;
				if ((uy < 0 ? 1 : -1) != (vy < 0 ? 1 : -1))
					changeY++;

				if (changeX > 2 || changeY > 2)
					convex = false;
			}

			/* check if face is within tile */
			if (mClipper.clipNext((int) nx, (int) ny) == 0) {
				even = (even == 0 ? 1 : 0);
				continue;
			}

			/* add ZigZagQuadIndices(tm) for sides */
			short vert = (short) (vOffset + (i - 2));
			short s0 = vert++;
			short s1 = vert++;
			short s2 = vert++;
			short s3 = vert++;

			// connect last to first (when number of faces is even)
			if (!addFace && i == len) {
				s2 -= len;
				s3 -= len;
			}

			short[] indices = mCurIndices[even].vertices;
			// index id relative to mCurIndices item
			int ind = mCurIndices[even].used;

			if (ind == VertexItem.SIZE) {
				mCurIndices[even].next = VertexItem.pool.get();
				mCurIndices[even] = mCurIndices[even].next;
				indices = mCurIndices[even].vertices;
				ind = 0;
			}

			indices[ind + 0] = s0;
			indices[ind + 1] = s2;
			indices[ind + 2] = s1;

			indices[ind + 3] = s1;
			indices[ind + 4] = s2;
			indices[ind + 5] = s3;

			mCurIndices[even].used += 6;
			even = (even == 0 ? 1 : 0);

			/* add roof outline indices */
			VertexItem it = mCurIndices[IND_OUTLINE];
			if (it.used == VertexItem.SIZE) {
				it.next = VertexItem.pool.get();
				it = mCurIndices[IND_OUTLINE] = it.next;
			}
			it.vertices[it.used++] = s1;
			it.vertices[it.used++] = s3;
		}

		mCurVertices.used = v;
		mNumVertices += vertexCnt;
		return convex;
	}

	@Override
	public void compile(ShortBuffer sbuf) {

		if (mNumVertices == 0 || compiled)
			return;

		mVertexBO = BufferObject.get(0);
		mIndiceBO = BufferObject.get(0);
		mIndicesBufferID = mIndiceBO.id;
		mVertexBufferID = mVertexBO.id;

		// upload indices
		sbuf.clear();
		mNumIndices = 0;
		for (int i = 0; i < 4; i++) {
			for (VertexItem vi = mIndices[i]; vi != null; vi = vi.next) {
				sbuf.put(vi.vertices, 0, vi.used);
				mIndiceCnt[i] += vi.used;
			}
			mNumIndices += mIndiceCnt[i];
		}

		sbuf.flip();
		mIndiceBO.size = mNumIndices * 2;
		GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, mIndicesBufferID);
		GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER,
				mIndiceBO.size, sbuf, GLES20.GL_DYNAMIC_DRAW);

		// upload vertices
		sbuf.clear();
		for (VertexItem vi = mVertices; vi != null; vi = vi.next)
			sbuf.put(vi.vertices, 0, vi.used);

		sbuf.flip();
		mVertexBO.size = mNumVertices * 4 * 2;
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVertexBufferID);
		GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER,
				mVertexBO.size, sbuf, GLES20.GL_DYNAMIC_DRAW);

		GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

		for (VertexItem i : mIndices)
			VertexItem.pool.releaseAll(i);

		VertexItem.pool.releaseAll(mVertices);

		mClipper = null;

		compiled = true;
	}

	@Override
	protected void clear() {
		if (compiled) {
			BufferObject.release(mIndiceBO);
			BufferObject.release(mVertexBO);
			mIndiceBO = null;
			mVertexBO = null;
			//GLES20.glDeleteBuffers(2, mVboIds, 0);
		} else {
			VertexItem.pool.releaseAll(mVertices);
			for (VertexItem i : mIndices)
				VertexItem.pool.releaseAll(i);
		}
	}

	private static boolean initialized = false;
	private static ShortBuffer sBuf;

	public static synchronized int triangulate(float[] points, int ppos, int plen, short[] index,
			int ipos, int rings, int vertexOffset, VertexItem item) {

		if (!initialized) {
			// FIXME also cleanup on shutdown!
			sBuf = ByteBuffer.allocateDirect(1800 * 2).order(ByteOrder.nativeOrder())
					.asShortBuffer();

			initialized = true;
		}

		sBuf.clear();
		sBuf.put(index, ipos, rings);

		int numTris = triangulate(points, ppos, plen, rings, sBuf, vertexOffset);

		int numIndices = numTris * 3;
		sBuf.limit(numIndices);
		sBuf.position(0);

		for (int k = 0, cnt = 0; k < numIndices; k += cnt) {

			if (item.used == VertexItem.SIZE) {
				item.next = VertexItem.pool.get();
				item = item.next;
			}

			cnt = VertexItem.SIZE - item.used;

			if (k + cnt > numIndices)
				cnt = numIndices - k;

			sBuf.get(item.vertices, item.used, cnt);
			item.used += cnt;
		}

		return numIndices;
	}

	/**
	 * @param points an array of x,y coordinates
	 * @param pos position in points array
	 * @param len number of points * 2 (i.e. values to read)
	 * @param numRings number of rings in polygon == outer(1) + inner rings
	 * @param io input: number of points in rings - times 2!
	 *            output: indices of triangles, 3 per triangle :) (indices use
	 *            stride=2, i.e. 0,2,4...)
	 * @param ioffset offset used to add offset to indices
	 * @return number of triangles in io buffer
	 */
	public static native int triangulate(float[] points, int pos, int len, int numRings, ShortBuffer io,
			int ioffset);

	static {
		System.loadLibrary("triangle");
	}
}
