/*
 * Copyright 2012 Hannes Janetzek
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
package org.oscim.view.mapgenerator;

import org.oscim.core.Tile;

/**
 * 
 */
public class JobTile extends Tile implements Comparable<JobTile> {
	// public final static int LOADING = 1;
	// public final static int NEWDATA = 1 << 1;
	// public final static int READY = 1 << 2;
	// public final static int AVAILABLE = 1 << 1 | 1 << 2;
	// public final static int CANCELED = 1 << 3;
	// public int state;

	/**
	 * tile is in JobQueue
	 */
	public boolean isLoading;

	/**
	 * distance from map center.
	 */
	public float distance;

	/**
	 * @param tileX
	 *            ...
	 * @param tileY
	 *            ...
	 * @param zoomLevel
	 *            ..
	 */
	public JobTile(int tileX, int tileY, byte zoomLevel) {
		super(tileX, tileY, zoomLevel);
	}

	@Override
	public int compareTo(JobTile o) {
		if (this.distance < o.distance) {
			return -1;
		}
		if (this.distance > o.distance) {
			return 1;
		}
		return 0;
	}
}