/*
 * Copyright 2010, 2011, 2012 mapsforge.org
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
import org.oscim.core.GeoPoint;
import org.oscim.core.PointD;
import org.oscim.core.PointF;
import org.oscim.core.Tile;
import org.oscim.view.MapView;

import android.view.MotionEvent;

public class Move extends Interaction
{
	private static final double MOVE_THRESHOLD = 10.0;

	public static final int NUM_POINTERS = 1;
	private static final int w = Tile.SIZE * 3;
	private static final int h = Tile.SIZE * 3;
	private static final float s = (200 / MapView.dpi);
	private final long time_start, time_end;
	private final ArrayList<PointF> pointer_track;
	private final PointD center_start, center_end;

	public Move(InteractionBuffer buf)
	{
		GeoPoint point;

		assert buf.getPointerNum() == NUM_POINTERS;

		this.time_start = buf.getTime_start();
		this.time_end = buf.getTime_end();
		this.pointer_track = buf.getTrack()[0];
		point = buf.getMapPosition_start().getGeoPoint();
		this.center_start = new PointD(point.getLongitude(), point.getLatitude());
		point = buf.getMapPosition_end().getGeoPoint();
		this.center_end = new PointD(point.getLongitude(), point.getLatitude());
	}

	public static boolean recognize(MotionEvent e, InteractionBuffer buf)
	{
		if (e.getPointerCount() != NUM_POINTERS)
		{
			return false;
		}
		else if (buf.className == null)
		{
			buf.className = Move.class;
			return true;
		}
		else if (buf.className == Move.class)
		{
			return true;
		}
		else
		{
			return false;
		}
	}

	public static void execute(InteractionBuffer buf)
	{
//		System.out.println("Scale");
//		System.out.println(buf.preX[0] + "|" + buf.preY[0]);
//		System.out.println(buf.curX[0] + "|" + buf.curY[0]);
//		System.out.println("disX: " + buf.focusX + "|disY: " + buf.focusY);

		float deltaX = buf.curX[0] - buf.preX[0];
		float deltaY = buf.curY[0] - buf.preY[0];
		double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
		if (distance >= MOVE_THRESHOLD)
		{
			if (Math.sqrt(buf.velocityX * buf.velocityX + buf.velocityY * buf.velocityY) >= 250.0)
			{
				System.out.println("OnFling");
				buf.mapView.getMapViewPosition().animateFling(Math.round(buf.velocityX * s),
				                                              Math.round(buf.velocityY * s),
				                                              -w, w, -h, h);
			}
			else
			{
				buf.mapView.getMapViewPosition().moveMap(deltaX, deltaY);
			}
			buf.mapView.redrawMap(true);

			buf.preX[0] = buf.curX[0];
			buf.preY[0] = buf.curY[0];
//			buf.preDistance = buf.curDistance;
//			buf.preRad = buf.curRad;
		}
	}

	@Override
	public Element log_XML()
	{
		StringBuilder s = new StringBuilder();
		Element interaction = new Element("Interaction");

		interaction.setAttribute("type", "Move");
		interaction.setAttribute("start", String.valueOf(this.time_start));
		interaction.setAttribute("end", String.valueOf(this.time_end));

		Element pointer = new Element("Pointer");
		pointer.setAttribute("id", "1");
		for (int i = 0; i < this.pointer_track.size(); i ++)
		{
			s.append(this.pointer_track.get(i).x).append(" ").append(this.pointer_track.get(i).y).append(",");
		}
		s.deleteCharAt(s.length()-1);
		pointer.setText(s.toString());
		interaction.addContent(pointer);

		Element center = new Element("Center");
		s.delete(0, s.length());
		s.append(this.center_start.x).append(" ").append(this.center_start.y);
		s.append(",");
		s.append(this.center_end.x).append(" ").append(this.center_end.y);
		center.setText(s.toString());
		interaction.addContent(center);

		return interaction;
	}

	public long getTime_start()
	{
		return this.time_start;
	}

	public long getTime_end()
	{
		return this.time_end;
	}

	public ArrayList<PointF> getTrack()
	{
		return this.pointer_track;
	}

	public PointD getCenter_start()
	{
		return this.center_start;
	}

	public PointD getCenter_end()
	{
		return this.center_end;
	}
}
