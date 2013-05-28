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

import android.view.MotionEvent;

public class Move extends Interaction
{
	public static final int NUM_POINTERS = 1;
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
		if (buf.className != null && buf.className != Move.class)
		{
			return false;
		}

		if (e.getPointerCount() != NUM_POINTERS)
		{
			return false;
		}

		buf.focusX = e.getX(0) - buf.X[0];
		buf.focusY = e.getY(0) - buf.Y[0];

		buf.X[0] = e.getX(0);
		buf.Y[0] = e.getY(0);

		return true;
	}

	public static void execute(InteractionBuffer buf)
	{
		System.out.println("Move: " + buf.focusX + ", " + buf.focusY);
		buf.mapView.getMapViewPosition().moveMap(buf.focusX, buf.focusY);
		buf.mapView.redrawMap(true);
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
