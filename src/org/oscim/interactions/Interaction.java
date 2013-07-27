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

import org.jdom2.Element;

import android.view.MotionEvent;

public abstract class Interaction
{
//	private static final int NUM_POINTERS = 0;
//	private ArrayList<PointF> pointer_track[];
//	private long time_start, time_end;
//	private MapPosition position_start, position_end;

	/**
	 * this method need to been overridden and is to recognize, to which class the given {@link MotionEvent} belongs.
	 *
	 * @return whether the {@link MotionEvent} is the corresponding interaction.
	 */
	public static boolean recognize()
	{
		return false;
	}

	/**
	 * this method need to been overridden and is to execute the corresponding interaction.
	 *
	 */
	public static void execute()
	{
	}

	public abstract Element log_XML();

//	(ArrayList<PointF> pointer_track[],
//			long time_start, long time_end,
//			MapPosition position_start, MapPosition position_end)
//	{
//		if ( pointer_track.length == Interaction.NUM_POINTERS )
//		{
//			this.pointer_track = pointer_track;
//			this.time_start = time_start;
//			this.time_end = time_end;
//			this.position_start = position_start;
//			this.position_end = position_end;
//		}
//		else
//		{
//			Log.d("Interaction", "creation failed because of the difference size");
//		}
//	}

//	public ArrayList<PointF> getPointer_track(int index)
//	{
//		if (index >= Interaction.NUM_POINTERS)
//		{
//			return null;
//		}
//
//		return this.pointer_track[index];
//	}
}
