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
package org.oscim.interactions;

import java.util.ArrayList;

import org.jdom2.Element;
import org.oscim.core.MapPosition;
import org.oscim.core.PointF;

import android.util.Log;
import android.view.MotionEvent;

public abstract class Interaction
{
	private static final int NUM_POINTERS = 0;
	private ArrayList<PointF> pointer_track[];
	private long time_start, time_end;
	private MapPosition position_start, position_end;

	public abstract Interaction recognize(MotionEvent e);

	public abstract boolean execute();

	public abstract Element log_XML();

	public Interaction(ArrayList<PointF> pointer_track[],
			long time_start, long time_end,
			MapPosition position_start, MapPosition position_end)
	{
		if ( pointer_track.length == Interaction.NUM_POINTERS )
		{
			this.pointer_track = pointer_track;
			this.time_start = time_start;
			this.time_end = time_end;
			this.position_start = position_start;
			this.position_end = position_end;
		}
		else
		{
			Log.d("Interaction", "creation failed because of the difference size");
		}
	}

	public int getNumOfPointers()
	{
		return Interaction.NUM_POINTERS;
	}

	public ArrayList<PointF> getPointer_track(int index)
	{
		if (index >= Interaction.NUM_POINTERS)
		{
			return null;
		}

		return this.pointer_track[index];
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
}
