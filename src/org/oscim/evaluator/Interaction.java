/*
 * Copyright 2012
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
package org.oscim.evaluator;

import java.util.ArrayList;

import org.oscim.core.MapPosition;
import org.oscim.core.PointF;

import android.util.Log;

public class Interaction
{
	private int numOfPointers;
	private PointF pointers_start[], pointers_end[];
	private ArrayList<PointF> pointers_move[];
	private long time_start, time_end;
	private MapPosition position_start, position_end;
	private double lat, lon;

	public Interaction(int num,
			PointF pointers_start[], ArrayList<PointF> pointers_move[], PointF pointers_end[],
			long time_start, long time_end,
			MapPosition position_start, MapPosition position_end,
			double lat, double lon)
	{
		if ((pointers_start.length == num) && (pointers_end.length == num))
		{
			this.numOfPointers = num;
			this.pointers_start = pointers_start;
			this.pointers_move = pointers_move;
			this.pointers_end = pointers_end;
			this.time_start = time_start;
			this.time_end = time_end;
			this.position_start = position_start;
			this.position_end = position_end;
			this.lat = lat;
			this.lon = lon;
		}
		else
		{
			Log.d("Interaction", "creation failed because of the difference size");
		}
	}

	public int getNumOfPointers()
	{
		return this.numOfPointers;
	}

	public PointF getPointer_start(int index)
	{
		if (index >= numOfPointers)
		{
			return null;
		}

		return this.pointers_start[index];
	}

	public ArrayList<PointF> getPointer_move(int index)
	{
		if (index >= numOfPointers)
		{
			return null;
		}

		return this.pointers_move[index];
	}

	public PointF getPointer_end(int index)
	{
		if (index >= numOfPointers)
		{
			return null;
		}

		return this.pointers_end[index];
	}

	public MapPosition getMapPosition_start()
	{
		return this.position_start;
	}

	public MapPosition getMapPosition_end()
	{
		return this.position_end;
	}

	public long getStarttime()
	{
		return this.time_start;
	}

	public long getEndtime()
	{
		return this.time_end;
	}

	public double getLat()
	{
		return this.lat;
	}

	public double getLon()
	{
		return this.lon;
	}
}
