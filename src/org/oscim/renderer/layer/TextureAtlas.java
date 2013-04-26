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

// ported from:
/* ============================================================================
 * Freetype GL - A C OpenGL Freetype engine
 * Platform:    Any
 * WWW:         http://code.google.com/p/freetype-gl/
 * ----------------------------------------------------------------------------
 * Copyright 2011,2012 Nicolas P. Rougier. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY NICOLAS P. ROUGIER ''AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
 * EVENT SHALL NICOLAS P. ROUGIER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are
 * those of the authors and should not be interpreted as representing official
 * policies, either expressed or implied, of Nicolas P. Rougier.
 * ============================================================================
 *
 * This source is based on the article by Jukka Jylanki :
 * "A Thousand Ways to Pack the Bin - A Practical Approach to
 * Two-Dimensional Rectangle Bin Packing", February 27, 2010.
 *
 * More precisely, this is an implementation of the Skyline Bottom-Left
 * algorithm based on C++ sources provided by Jukka Jylanki at:
 * http://clb.demon.fi/files/RectangleBinPack/
 *
 *  ============================================================================
 */
package org.oscim.renderer.layer;

import org.oscim.utils.pool.Inlist;

import android.graphics.Bitmap;

public class TextureAtlas {
	/** Allocated slots */
	public Slot mSlots;
	private Rect mRects;

	/** Width (in pixels) of the underlying texture */
	final int mWidth;

	/** Height (in pixels) of the underlying texture */
	final int mHeight;

	/** Depth (in bytes) of the underlying texture */
	final int mDepth;

	/** Allocated surface size */
	int mUsed;

	/** Texture identity (OpenGL) */
	int mId;

	/** Atlas data */
	Bitmap mData;

	public static class Slot extends Inlist<Slot> {
		public int x, y, w;

		public Slot(int x, int y, int w) {
			this.x = x;
			this.y = y;
			this.w = w;
		}
	}

	public static class Rect extends Inlist<Rect> {
		public int x, y, w, h;
	}

	private TextureAtlas(int width, int height, int depth) {
		mWidth = width;
		mHeight = height;
		mDepth = depth;
		mSlots = new Slot(1, 1, width - 2);
	}

	public Rect getRegion(int width, int height) {
		int y, bestHeight, bestWidth;
		Slot slot, prev;
		Rect r = new Rect();
		r.w = width;
		r.h = height;

		bestHeight = Integer.MAX_VALUE;
		bestWidth = Integer.MAX_VALUE;

		Slot bestSlot = null;

		for (slot = mSlots; slot != null; slot = slot.next) {
			// fit width
			if ((slot.x + width) > (mWidth - 1))
				continue;

			// fit height
			y = slot.y;
			int widthLeft = width;

			Slot fit = slot;
			while (widthLeft > 0) {
				if (fit.y > y)
					y = fit.y;

				if ((y + height) > (mHeight - 1)) {
					y = -1;
					break;
				}
				widthLeft -= fit.w;

				fit = fit.next;
			}

			if (y < 0)
				continue;

			int h = y + height;
			if ((h < bestHeight) || ((h == bestHeight) && (slot.w < bestWidth))) {
				bestHeight = h;
				bestSlot = slot;
				bestWidth = slot.w;
				r.x = slot.x;
				r.y = y;
			}
		}

		if (bestSlot == null)
			return null;

		Slot curSlot = new Slot(r.x, r.y + height, width);
		mSlots = Inlist.prependRelative(mSlots, curSlot, bestSlot);

		// split
		for (prev = curSlot; prev.next != null;) {
			slot = prev.next;

			int shrink = (prev.x + prev.w) - slot.x;

			if (shrink <= 0)
				break;

			slot.x += shrink;
			slot.w -= shrink;
			if (slot.w > 0)
				break;

			// erease slot
			prev.next = slot.next;
		}

		// merge
		for (slot = mSlots; slot.next != null;) {
			Slot next = slot.next;

			if (slot.y == next.y) {
				slot.w += next.w;

				// erease 'next' slot
				slot.next = next.next;
			} else {
				slot = next;
			}
		}

		mUsed += width * height;

		mRects = Inlist.push(mRects, r);
		return r;
	}

	public void clear() {
		mRects = null;
		mSlots = new Slot(1, 1, mWidth - 2);
	}

	public static TextureAtlas create(int width, int height, int depth) {
		if (!(depth == 1 || depth == 3 || depth == 4))
			throw new IllegalArgumentException("invalid depth");

		return new TextureAtlas(width, height, depth);
	}
}
