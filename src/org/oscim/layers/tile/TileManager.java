/*
 * Copyright 2013 Hannes Janetzek
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

package org.oscim.layers.tile;

import static org.oscim.layers.tile.MapTile.STATE_LOADING;
import static org.oscim.layers.tile.MapTile.STATE_NEW_DATA;
import static org.oscim.layers.tile.MapTile.STATE_NONE;

import java.util.ArrayList;
import java.util.Arrays;

import org.oscim.core.MapPosition;
import org.oscim.core.Tile;
import org.oscim.renderer.BufferObject;
import org.oscim.renderer.GLRenderer;
import org.oscim.renderer.layer.TextItem;
import org.oscim.utils.FastMath;
import org.oscim.utils.ScanBox;
import org.oscim.utils.quadtree.QuadTree;
import org.oscim.utils.quadtree.QuadTreeIndex;
import org.oscim.view.MapView;
import org.oscim.view.MapViewPosition;

import android.util.Log;

/**
 * @TODO
 *       - prefetching to cache file
 *       - this class should probably not be in 'renderer' -> tilemap?
 *       - make it general for reuse in tile-overlays
 */
public class TileManager {
	private static final int CACHE_TILES_MAX = 250;

	static final String TAG = TileManager.class.getSimpleName();
	private final static int MIN_ZOOMLEVEL = 2;

	private final int mMaxZoom;

	// limit number tiles with new data not uploaded to GL
	// TODO this should depend on the number of tiles displayed
	private static final int MAX_TILES_IN_QUEUE = 40;
	// cache limit threshold
	private static final int CACHE_THRESHOLD = 30;

	private final MapView mMapView;
	private final MapViewPosition mMapViewPosition;

	private boolean mInitialized;

	// cache for all tiles
	private MapTile[] mTiles;

	// actual number of tiles in mTiles
	private int mTilesCount;

	// current end position in mTiles
	private int mTilesSize;

	// counter for tiles with new data not uploaded to GL
	private volatile int mTilesForUpload;

	// new tile jobs for MapWorkers
	private final ArrayList<MapTile> mJobs;

	// counter to check whether current TileSet has changed
	private  int mUpdateSerial;

	// lock for TileSets while updating MapTile locks
	private final Object mTilelock = new Object();

	// need to keep track of TileSets to clear on reset...
	private final ArrayList<TileSet> mTileSets = new ArrayList<TileSet>(4);

	private TileSet mCurrentTiles;
	/* package */TileSet mNewTiles;


	private final QuadTreeIndex<MapTile> mIndex = new QuadTreeIndex<MapTile>(){

		@Override
		public MapTile create(int x, int y, int z) {
			QuadTree<MapTile> t = super.add(x, y, z);
			t.item = new MapTile(x, y, (byte)z);
			t.item.rel = t;

			return t.item;
		}

		@Override
		public void remove(MapTile t) {
			if (t.rel == null) {
				Log.d(TAG, "BUG already removed " + t);
				return;
			}

			super.remove(t.rel);

			t.rel.item = null;
			t.rel = null;
		}
	};

	private final float[] mBoxCoords = new float[8];
	private final TileLayer<?> mTileLayer;


	public TileManager(MapView mapView, TileLayer<?> tileLayer, int maxZoom) {
		mMapView = mapView;
		mTileLayer = tileLayer;
		mMaxZoom = maxZoom;
		mMapViewPosition = mapView.getMapViewPosition();
		mJobs = new ArrayList<MapTile>();
		mTiles = new MapTile[CACHE_TILES_MAX];

		mTilesSize = 0;
		mTilesForUpload = 0;

		mUpdateSerial = 0;

		mInitialized = false;
	}

	public void destroy() {
		mInitialized = false;
		// there might be some leaks in here
		// ... free static pools
	}

	public synchronized void init(int width, int height) {

		// sync with GLRender thread
		// ... and labeling thread?
		GLRenderer.drawlock.lock();

		if (mInitialized) {
			// pass VBOs and VertexItems back to pools
			for (int i = 0; i < mTilesSize; i++)
				clearTile(mTiles[i]);
		}
		//else {
		// mInitialized is set when surface changed
		// and VBOs might be lost
		//	VertexPool.init();
		//}

		// clear cache index
		//QuadTree.init();

		// clear references to cached MapTiles
		Arrays.fill(mTiles, null);
		mTilesSize = 0;
		mTilesCount = 0;

		// clear all references to previous tiles
		for (TileSet td : mTileSets) {
			Arrays.fill(td.tiles, null);
			td.cnt = 0;
		}

		// set up TileSet large enough to hold current tiles
		int num = Math.max(width, height);
		int size = Tile.SIZE >> 1;
		int numTiles = (num * num) / (size * size) * 4;
		mNewTiles = new TileSet(numTiles);
		mCurrentTiles = new TileSet(numTiles);
		Log.d(TAG, "max tiles: " + numTiles);

		mInitialized = true;

		GLRenderer.drawlock.unlock();
	}

	/**
	 * 1. Update mCurrentTiles TileSet of currently visible tiles.
	 * 2. Add not yet loaded (or loading) tiles to JobQueue.
	 * 3. Manage cache
	 *
	 * @param pos
	 *            current MapPosition
	 */
	public synchronized void update(MapPosition pos) {
		// clear JobQueue and set tiles to state == NONE.
		// one could also append new tiles and sort in JobQueue
		// but this has the nice side-effect that MapWorkers dont
		// start with old jobs while new jobs are calculated, which
		// should increase the chance that they are free when new
		// jobs come in.
		mTileLayer.setJobs(null);

		// load some tiles more than currently visible (* 0.75)
		double scale = pos.scale * 0.9f;

		int tileZoom = FastMath.clamp(pos.zoomLevel, MIN_ZOOMLEVEL, mMaxZoom);

		mMapViewPosition.getMapViewProjection(mBoxCoords);

		// scan visible tiles. callback function calls 'addTile'
		// which updates mNewTiles
		mNewTiles.cnt = 0;
		mScanBox.scan(pos.x, pos.y, scale, tileZoom, mBoxCoords);

		MapTile[] newTiles = mNewTiles.tiles;
		MapTile[] curTiles = mCurrentTiles.tiles;

		boolean changed = (mNewTiles.cnt != mCurrentTiles.cnt);

		Arrays.sort(mNewTiles.tiles, 0, mNewTiles.cnt, TileSet.coordComparator);

		if (!changed) {
			for (int i = 0, n = mNewTiles.cnt; i < n; i++) {
				if (newTiles[i] != curTiles[i]) {
					changed = true;
					break;
				}
			}
		}

		if (changed) {
			synchronized (mTilelock) {
				// lock new tiles
				for (int i = 0, n = mNewTiles.cnt; i < n; i++)
					newTiles[i].lock();

				// unlock previous tiles
				for (int i = 0, n = mCurrentTiles.cnt; i < n; i++) {
					curTiles[i].unlock();
					curTiles[i] = null;
				}

				// make new tiles current
				TileSet tmp = mCurrentTiles;
				mCurrentTiles = mNewTiles;
				mNewTiles = tmp;

				mUpdateSerial++;
			}

			// request rendering as tiles changed
			mMapView.render();
		}

		/* Add tile jobs to queue */
		if (mJobs.isEmpty())
			return;

		MapTile[] jobs = new MapTile[mJobs.size()];
		jobs = mJobs.toArray(jobs);
		updateTileDistances(jobs, jobs.length, pos);

		// sets tiles to state == LOADING
		mTileLayer.setJobs(jobs);
		mJobs.clear();

		/* limit cache items */
		int remove = mTilesCount - CACHE_TILES_MAX;

		if (remove > CACHE_THRESHOLD ||
				mTilesForUpload > MAX_TILES_IN_QUEUE)

			limitCache(pos, remove);
	}


	/**
	 * Retrive a TileSet of current tiles.
	 * Tiles remain locked in cache until the set is unlocked by either passing
	 * it again to this function or to releaseTiles. If passed TileSet is null
	 * it will be allocated.
	 */
	public TileSet getActiveTiles(TileSet td) {
		if (mCurrentTiles == null)
			return td;

		if (td != null && td.serial == mUpdateSerial)
			return td;

		// dont flip new/currentTiles while copying
		synchronized (mTilelock) {
			MapTile[] newTiles = mCurrentTiles.tiles;
			int cnt = mCurrentTiles.cnt;

			// lock tiles (and their proxies) to not be removed from cache
			for (int i = 0; i < cnt; i++)
				newTiles[i].lock();

			MapTile[] nextTiles;

			if (td == null) {
				td = new TileSet(newTiles.length);
				mTileSets.add(td);
			}

			nextTiles = td.tiles;

			// unlock previously active tiles
			for (int i = 0, n = td.cnt; i < n; i++)
				nextTiles[i].unlock();

			// copy newTiles to nextTiles
			System.arraycopy(newTiles, 0, nextTiles, 0, cnt);

			td.serial = mUpdateSerial;
			td.cnt = cnt;
		}

		return td;
	}

	//	/**
	//	 * @param tiles ...
	//	 */
	//	public void releaseTiles(TileSet tiles) {
	//
	//	}

	/* package */MapTile addTile(int x, int y, int zoomLevel) {
		MapTile tile;

		//tile = QuadTree.getTile(x, y, zoomLevel);
		tile = mIndex.getTile(x, y, zoomLevel);

		if (tile == null) {
			tile = mIndex.create(x, y, zoomLevel);
			mJobs.add(tile);
			addToCache(tile);
		} else if (!tile.isActive()) {
			mJobs.add(tile);
		}

		if (zoomLevel > 2) {
			boolean add = false;

			// prefetch parent
			MapTile p = tile.rel.parent.item;

			if (p == null) {
				p = mIndex.create(x >> 1, y >> 1, zoomLevel - 1);

				addToCache(p);
				add = true;
			}

			if (add || !p.isActive()) {
				// hack to not add tile twice
				p.state = STATE_LOADING;
				mJobs.add(p);
			}

			if (zoomLevel > 3) {
				// prefetch grand  parent
				p = tile.rel.parent.parent.item;
				add = false;
				if (p == null) {
					p = mIndex.create(x >> 2, y >> 2, zoomLevel - 2);
					addToCache(p);
					add = true;
				}

				if (add || !p.isActive()) {
					p.state = STATE_LOADING;
					mJobs.add(p);
				}
			}
		}

		return tile;
	}

	private void addToCache(MapTile tile) {

		if (mTilesSize == mTiles.length) {
			if (mTilesSize > mTilesCount) {
				//Log.d(TAG, "repack: " + mTiles.length + " / " + mTilesCount);
				TileDistanceSort.sort(mTiles, 0, mTilesSize);
				mTilesSize = mTilesCount;
			}

			if (mTilesSize == mTiles.length) {
				Log.d(TAG, "realloc tiles " + mTilesSize);
				MapTile[] tmp = new MapTile[mTiles.length + 20];
				System.arraycopy(mTiles, 0, tmp, 0, mTilesCount);
				mTiles = tmp;
			}
		}

		mTiles[mTilesSize++] = tile;
		mTilesCount++;
	}

	private void clearTile(MapTile t) {
		if (t == null)
			return;

		if (t.layers != null) {
			// TODO move this to layers clear
			if (t.layers.vbo != null) {
				BufferObject.release(t.layers.vbo);
				t.layers.vbo = null;
			}

			t.layers.clear();
			t.layers = null;
		}

		TextItem.pool.releaseAll(t.labels);

		mIndex.remove(t);

		//QuadTree.remove(t);
		t.state = STATE_NONE;

		mTilesCount--;
	}

	private static void updateTileDistances(Object[] tiles, int size, MapPosition mapPosition) {
		// TODO there is probably  a better quad-tree distance function

		int zoom = mapPosition.zoomLevel;
		double x = mapPosition.x;
		double y = mapPosition.y;

		// half tile size at current zoom-level
		final double h = 1.0 / (2 << zoom);

		//long center = (long)(h * (1 << zoom));

		for (int i = 0; i < size; i++) {
			MapTile t = (MapTile) tiles[i];
			if (t == null)
				continue;

			int diff = (t.zoomLevel - zoom);
			double dx, dy, scale;

			if (diff == 0) {
				dx = (t.x + h) - x;
				dy = (t.y + h) - y;
				scale = 0.5f;
			} else if (diff > 0) {
				// tile zoom level is greater than current
				// NB: distance increase by the factor 2
				// with each zoom-level, so that children
				// will be kept less likely than parent tiles.
				double dh = 1.0 / (2 << t.zoomLevel);
				dx = (t.x + dh) - x;
				dy = (t.y + dh) - y;
				// add tilesize/2 with each zoom-level
				// so that children near the current
				// map position but a few levels above
				// will also be removed
				//dz = diff * h;
				scale = (1 << diff);
			} else {
				diff = -diff;
				// tile zoom level is smaller than current
				double dh = 1.0 / (2 << t.zoomLevel);

				dx = (t.x + dh) - x;
				dy = (t.y + dh) - y;
				scale = 0.5f * (1 << diff);
			}
			if (dx < 0)
				dx = -dx;

			if (dy < 0)
				dy = -dy;

			t.distance = (float) ((dx + dy) * scale * 1024);
		}
	}

	private void limitCache(MapPosition mapPosition, int remove) {
		MapTile[] tiles = mTiles;
		int size = mTilesSize;

		// count tiles that have new data
		mTilesForUpload = 0;
		int newTileCnt = 0;

		// remove tiles that were never loaded
		for (int i = 0; i < size; i++) {
			MapTile t = tiles[i];
			if (t == null)
				continue;

			if (t.state == STATE_NEW_DATA)
				newTileCnt++;

			// make sure tile cannot be used by GL or MapWorker Thread
			if ((t.state != 0) || t.isLocked()) {
				continue;
			}
			clearTile(t);
			tiles[i] = null;
			remove--;
		}

		if (remove > 10 || newTileCnt > MAX_TILES_IN_QUEUE) {
			updateTileDistances(tiles, size, mapPosition);

			TileDistanceSort.sort(tiles, 0, size);

			// sorting also repacks the 'sparse' filled array
			// so end of mTiles is at mTilesCount now
			size = mTilesSize = mTilesCount;

			for (int i = size - 1; i >= 0 && remove > 0; i--) {
				MapTile t = tiles[i];
				if (t.isLocked()) {
					// dont remove tile used by GLRenderer, or somewhere else
					Log.d(TAG, "limitCache: tile still locked " + t + " " + t.distance);
					// try again in next run.
					//locked = true;
					//break;
				} else if (t.state == STATE_LOADING) {
					// NOTE:  when set loading to false the tile could be
					// added to load queue again while still processed in
					// MapTileLoader => need tile.cancel flag.
					// t.isLoading = false;
					Log.d(TAG, "limitCache: cancel loading " + t + " " + t.distance);
				} else {
					if (t.state == STATE_NEW_DATA)
						newTileCnt--;

					remove--;
					clearTile(t);
					tiles[i] = null;
				}
			}

			remove = (newTileCnt - MAX_TILES_IN_QUEUE) + 10;
			//int r = remove;
			for (int i = size - 1; i >= 0 && remove > 0; i--) {
				MapTile t = tiles[i];
				if (t != null && t.state == STATE_NEW_DATA) {
					if (!t.isLocked()) {
						clearTile(t);
						tiles[i] = null;
						remove--;
						newTileCnt--;
					}
				}
			}

			mTilesForUpload += newTileCnt;
			//Log.d(TAG, "cleanup load queue " + tilesForUpload + "/" + r + " - " + remove);
		}
	}

	/**
	 * called from MapWorker Thread when tile is loaded by MapTileLoader
	 *
	 * @param tile
	 *            Tile ready for upload to GL
	 * @return ... caller does not care
	 */
	public synchronized boolean passTile(MapTile tile) {

		if (tile.state != STATE_LOADING) {
			// - should rather be STATE_FAILED
			// no one should be able to use this tile now, MapTileLoader passed
			// it, GL-Thread does nothing until newdata is set.
			//Log.d(TAG, "passTile: failed loading " + tile);
			return true;
		}

		tile.state = STATE_NEW_DATA;
		mTilesForUpload++;

		// locked means the tile is visible or referenced by
		// a tile that might be visible.
		if (tile.isLocked())
			mMapView.render();

		return true;
	}

	private final ScanBox mScanBox = new ScanBox() {

		@Override
		protected void setVisible(int y, int x1, int x2) {
			MapTile[] tiles = mNewTiles.tiles;
			int cnt = mNewTiles.cnt;
			int maxTiles = tiles.length;

			int xmax = 1 << mZoom;

			for (int x = x1; x < x2; x++) {
				MapTile tile = null;

				if (cnt == maxTiles) {
					Log.d(TAG, "reached maximum tiles " + maxTiles);
					break;
				}
				int xx = x;

				if (x < 0 || x >= xmax) {
					// flip-around date line
					if (x < 0)
						xx = xmax + x;
					else
						xx = x - xmax;

					if (xx < 0 || xx >= xmax)
						continue;
				}

				// check if tile is already added
				for (int i = 0; i < cnt; i++)
					if (tiles[i].tileX == xx && tiles[i].tileY == y) {
						tile = tiles[i];
						break;
					}

				if (tile == null) {
					tile = addTile(xx, y, mZoom);
					tiles[cnt++] = tile;
				}
			}
			mNewTiles.cnt = cnt;
		}
	};
}
