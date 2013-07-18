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
	public static final double PARALLEL_THRESHOLD = Math.cos(Math.PI / 9); //20 degree
	public static final double ZOOM_ROTATION_THRESHOLD = Math.cos(Math.PI / 4); //45 degree

	private int num_pointers;
	private ArrayList<PointF> pointer_track[];
	private long time_start, time_end;
	private final MapPosition position_start, position_end;

	public final MapView mapView;
	public Class<? extends Interaction> className;
	public boolean parallel;
	public int zoom_rotation; //1: zoom; -1: rotation; 0: otherwise.
	public float preX[], curX[], preY[], curY[], velocityX, velocityY;
	public double preDistance, curDistance,
	              preRad, curRad,
	              A, B, C, same_side,
	              basisX, basisY, basisL,
	              vectorX_1, vectorY_1, cos_1, vectorL_1,
	              vectorX_2, vectorY_2, cos_2, vectorL_2;

	public InteractionBuffer(MotionEvent e, MapView mapView)
	{
		assert e.getPointerCount() == 1;

		this.className = null;
		this.num_pointers = 1;
		this.pointer_track = new ArrayList[1];
		this.preX = new float[1];
		this.preY = new float[1];
		this.curX = new float[1];
		this.curY = new float[1];
		this.pointer_track[0] = new ArrayList<PointF>();
		this.preX[0] = e.getX(0);
		this.preY[0] = e.getY(0);
		this.pointer_track[0].add(new PointF(this.preX[0], this.preY[0]));

//		System.out.println("num: " + this.num_pointers);
//		for (int i = 0; i < this.num_pointers; i ++)
//		{
//			System.out.print("track " + i + ": ");
//			ArrayList<PointF> temp = this.pointer_track[i];
//			for (int k = 0; k < temp.size(); k ++)
//			{
//				System.out.print("|" + temp.get(k).x + "," + temp.get(k).y);
//			}
//			System.out.println("|");
//		}
//		for (int i = 0; i < this.num_pointers; i ++)
//		{
//			System.out.println(this.preX[i] + "," + this.preY[i]);
//		}

		if (this.num_pointers == 1)
		{
			this.velocityX = 0.0f;
			this.velocityY = 0.0f;
		}
		else if (this.num_pointers == 2)
		{
			this.basisX = preX[0] - preX[1];
			this.basisY = preY[0] - preY[1];
			this.basisL = this.preDistance = Math.sqrt(this.basisX * this.basisX + this.basisY * this.basisY);
			this.preRad = Math.atan2(this.basisY, this.basisX);
			this.A = - this.basisY;
			this.B = this.basisX;
			this.C = preX[1] * preY[0] - preX[0] * preY[1];
			if (this.basisX == 0)
			{
				this.basisY = Math.abs(this.basisY);
			}
			else if (this.basisX < 0)
			{
				this.basisX = -this.basisX;
				this.basisY = -this.basisY;
			}

//			System.out.println("basic vector: " + this.basisX + ", " + this.basisY);
//			System.out.println("basic length: " + this.basisL);
		}

		this.time_start = System.currentTimeMillis() - SystemClock.uptimeMillis() + e.getEventTime();
		//this.time_end = -1;
		this.mapView = mapView;
		this.position_start = new MapPosition();
		this.mapView.getMapViewPosition().getMapPosition(this.position_start);
		this.position_end = new MapPosition();
		//this.position_end.copy(position_start);
	}

	public void update(MotionEvent e)
	{
		assert this.num_pointers == e.getPointerCount();

		for(int i = 0; i < this.num_pointers; i ++)
		{
			this.curX[i] = e.getX(i);
			this.curY[i] = e.getY(i);
			this.pointer_track[i].add(new PointF(this.curX[i], this.curY[i]));
		}

//		System.out.println("num: " + this.num_pointers);
//		for (int i = 0; i < this.num_pointers; i ++)
//		{
//			System.out.print("track " + i + ": ");
//			ArrayList<PointF> temp = this.pointer_track[i];
//			for (int k = 0; k < temp.size(); k ++)
//			{
//				System.out.print("|" + temp.get(k).x + "," + temp.get(k).y);
//			}
//			System.out.println("|");
//		}
//		for (int i = 0; i < this.num_pointers; i ++)
//		{
//			System.out.println(this.curX[i] + "," + this.curY[i]);
//		}
		if (this.num_pointers == 1)
		{
			this.velocityX = (this.curX[0] - this.pointer_track[0].get(0).x) / (e.getEventTime() - e.getDownTime()) * 1000.0f;
			this.velocityY = (this.curY[0] - this.pointer_track[0].get(0).y) / (e.getEventTime() - e.getDownTime()) * 1000.0f;

			System.out.println("velocityX: " + velocityX);
			System.out.println("velocityY: " + velocityY);
		}
		else if (this.num_pointers == 2)
		{
			if (this.className == null)
			{
				PointF point;

				point = this.pointer_track[0].get(0);
				this.vectorX_1 = curX[0] - point.x;
				this.vectorY_1 = curY[0] - point.y;
				this.vectorL_1= Math.sqrt(this.vectorX_1 * this.vectorX_1 + this.vectorY_1 * this.vectorY_1);
				if (this.vectorL_1 == 0.0)
				{
					this.cos_1 = Double.NaN;
				}
				else
				{
					this.cos_1 = (this.basisX * this.vectorX_1 + this.basisY * this.vectorY_1) / (this.basisL * this.vectorL_1);
				}

				point = this.pointer_track[1].get(0);
				this.vectorX_2 = curX[1] - point.x;
				this.vectorY_2 = curY[1] - point.y;
				this.vectorL_2 = Math.sqrt(this.vectorX_2 * this.vectorX_2 + this.vectorY_2 * this.vectorY_2);
				if (this.vectorL_2 == 0.0)
				{
					this.cos_2 = Double.NaN;
				}
				else
				{
					this.cos_2 = (this.basisX * this.vectorX_2 + this.basisY * this.vectorY_2) / (this.basisL * this.vectorL_2);
				}

				double cos = (this.vectorX_1 * this.vectorX_2 + this.vectorY_1 * this.vectorY_2) / (this.vectorL_1 * this.vectorL_2);

				if (cos > PARALLEL_THRESHOLD)
				{
					this.parallel = true;
				}
				else
				{
					this.parallel = false;
				}

				double D_0 = this.A * this.curX[0] + this.B * this.curY[0] + this.C;
				double D_1 = this.A * this.curX[1] + this.B * this.curY[1] + this.C;
				this.same_side = D_0 * D_1;
			}

			double dx = curX[0] - curX[1];
			double dy = curY[0] - curY[1];
			this.curDistance = Math.sqrt(dx * dx + dy * dy);
			this.curRad = Math.atan2(dy, dx);

//			System.out.println("vector 1: " + this.vectorX_1 + ", " + this.vectorY_1);
//			System.out.println("con 1: " + this.cos_1);
//			System.out.println("vector 2: " + this.vectorX_2 + ", " + this.vectorY_2);
//			System.out.println("con 2: " + this.cos_2);
//			System.out.println("preRad: " + this.preRad);
//			System.out.println("curRad: " + this.curRad);
		}
	}

	public void updateMulti(MotionEvent e, int n)
	{
			this.num_pointers += n;
			this.className = null;
			this.pointer_track = new ArrayList[this.num_pointers];
			this.preX = new float[this.num_pointers];
			this.preY = new float[this.num_pointers];
			this.curX = new float[this.num_pointers];
			this.curY = new float[this.num_pointers];
			int num = 0;
			for(int i = 0; i < e.getPointerCount(); i ++)
			{
				if (n == -1 && i == e.getActionIndex())
				{
					continue;
				}
				this.pointer_track[num] = new ArrayList<PointF>();
				this.preX[num] = e.getX(i);
				this.preY[num] = e.getY(i);
				this.pointer_track[num].add(new PointF(this.preX[num], this.preY[num]));

				num ++;
			}

//			System.out.println("num: " + this.num_pointers);
//			for (int i = 0; i < this.num_pointers; i ++)
//			{
//				System.out.print("track " + i + ": ");
//				ArrayList<PointF> temp = this.pointer_track[i];
//				for (int k = 0; k < temp.size(); k ++)
//				{
//					System.out.print("|" + temp.get(k).x + "," + temp.get(k).y);
//				}
//				System.out.println("|");
//			}
//			for (int i = 0; i < this.num_pointers; i ++)
//			{
//				System.out.println(this.preX[i] + "," + this.preY[i]);
//			}

			if (this.num_pointers == 2)
			{
				this.basisX = preX[0] - preX[1];
				this.basisY = preY[0] - preY[1];
				this.basisL = this.preDistance = Math.sqrt(this.basisX * this.basisX + this.basisY * this.basisY);
				this.preRad = Math.atan2(this.basisY, this.basisX);
				this.A = - this.basisY;
				this.B = this.basisX;
				this.C = preX[1] * preY[0] - preX[0] * preY[1];
				if (this.basisX == 0)
				{
					this.basisY = Math.abs(this.basisY);
				}
				else if (this.basisX < 0)
				{
					this.basisX = -this.basisX;
					this.basisY = -this.basisY;
				}

//				System.out.println("basic vector: " + this.basisX + ", " + this.basisY);
//				System.out.println("basic length: " + this.basisL);
			}
			this.time_start = System.currentTimeMillis() - SystemClock.uptimeMillis() + e.getEventTime();
			this.mapView.getMapViewPosition().getMapPosition(this.position_start);
	}

	public void updateEnd(MotionEvent e)
	{
		this.time_end = System.currentTimeMillis() - SystemClock.uptimeMillis() + e.getEventTime();
		this.mapView.getMapViewPosition().getMapPosition(this.position_end);
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
