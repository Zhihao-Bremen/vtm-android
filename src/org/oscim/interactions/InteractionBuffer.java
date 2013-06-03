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

import org.oscim.core.MapPosition;
import org.oscim.core.PointF;
import org.oscim.view.MapView;

import android.os.SystemClock;
import android.view.MotionEvent;

public final class InteractionBuffer
{
	private int num_pointers;
	private ArrayList<PointF> pointer_track[];
	private long time_start, time_end;
	private final MapPosition position_start, position_end;

	public final MapView mapView;
	public Class<? extends Interaction> className;
	public boolean finished;
	public float preX[], curX[], preY[], curY[], focusX, focusY, tilt;
	public double preDistance, curDistance, preRad, curRad;

	public InteractionBuffer(MotionEvent e, MapView mapView)
	{
		this.className = null;
		this.finished = false;
		this.num_pointers = e.getPointerCount();
		this.pointer_track = new ArrayList[this.num_pointers];
		this.preX = new float[this.num_pointers];
		this.preY = new float[this.num_pointers];
		this.curX = new float[this.num_pointers];
		this.curY = new float[this.num_pointers];
		for(int i = 0; i < this.num_pointers; i ++)
		{
			this.pointer_track[i] = new ArrayList<PointF>();
			this.preX[i] = e.getX(i);
			this.preY[i] = e.getY(i);
			this.pointer_track[i].add(new PointF(this.preX[i], this.preY[i]));
		}
		if (this.num_pointers == 2)
		{
			double dx = preX[0] - preX[1];
			double dy = preY[0] - preY[1];
			this.preDistance = Math.sqrt(dx * dx + dy * dy);
			this.preRad = Math.atan2(dy, dx);
		}
		this.time_start = System.currentTimeMillis() - SystemClock.uptimeMillis() + e.getEventTime();
		this.time_end = -1;
		this.mapView = mapView;
		this.position_start = new MapPosition();
		this.mapView.getMapViewPosition().getMapPosition(this.position_start);
		this.position_end = new MapPosition();
		this.position_end.copy(position_start);
	}

	public void update(MotionEvent e)
	{
		if (!this.finished)
		{
			assert this.num_pointers == e.getPointerCount();

			for(int i = 0; i < this.num_pointers; i ++)
			{
				this.curX[i] = e.getX(i);
				this.curY[i] = e.getY(i);
				this.pointer_track[i].add(new PointF(this.curX[i], this.curY[i]));
			}
			if (this.num_pointers == 2)
			{
				double dx = curX[0] - curX[1];
				double dy = curY[0] - curY[1];
				this.curDistance = Math.sqrt(dx * dx + dy * dy);
				this.curRad = Math.atan2(dy, dx);
			}
		}
	}

	public void updateMulti(MotionEvent e)
	{
		if (this.className == null)
		{
			this.num_pointers = e.getPointerCount();
			this.pointer_track = new ArrayList[this.num_pointers];
			this.preX = new float[this.num_pointers];
			this.preY = new float[this.num_pointers];
			this.curX = new float[this.num_pointers];
			this.curY = new float[this.num_pointers];
			for(int i = 0; i < this.num_pointers; i ++)
			{
				this.pointer_track[i] = new ArrayList<PointF>();
				this.preX[i] = e.getX(i);
				this.preY[i] = e.getY(i);
				this.pointer_track[i].add(new PointF(this.preX[i], this.preY[i]));
			}
			if (this.num_pointers == 2)
			{
				double dx = preX[0] - preX[1];
				double dy = preY[0] - preY[1];
				this.preDistance = Math.sqrt(dx * dx + dy * dy);
				this.preRad = Math.atan2(dy, dx);
			}
			this.time_start = System.currentTimeMillis() - SystemClock.uptimeMillis() + e.getEventTime();
		}
		else
		{
			if (!this.finished)
			{
				this.time_end = System.currentTimeMillis() - SystemClock.uptimeMillis() + e.getEventTime();
				this.mapView.getMapViewPosition().getMapPosition(this.position_end);
				this.finished = true;
			}
		}
	}

	public void updateEnd(MotionEvent e)
	{
		if (!this.finished)
		{
			this.time_end = System.currentTimeMillis() - SystemClock.uptimeMillis() + e.getEventTime();
			this.mapView.getMapViewPosition().getMapPosition(this.position_end);
			this.finished = true;
		}
	}

	public int getPointerNum()
	{
		return this.num_pointers;
	}

	public ArrayList<PointF>[] getTrack()
	{
		return this.pointer_track;
	}

	public long getTime_start()
	{
		return this.time_start;
	}

	public long getTime_end()
	{
		return this.time_end;
	}

	public MapPosition getMapPosition_start()
	{
		return this.position_start;
	}

	public MapPosition getMapPosition_end()
	{
		return this.position_end;
	}
}
