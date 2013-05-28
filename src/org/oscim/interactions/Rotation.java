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
import org.oscim.core.PointF;

import android.view.MotionEvent;

public class Rotation extends Interaction
{
	public static final double ROTATE_THRESHOLD = 0.02;
	public static final int NUM_POINTERS = 2;
	private final long time_start, time_end;
	private final ArrayList<PointF>[] pointer_track;
	private final float angle_start, angle_end;

	public Rotation(InteractionBuffer buf)
	{
		assert buf.getPointerNum() == NUM_POINTERS;
		this.time_start = buf.getTime_start();
		this.time_end = buf.getTime_end();
		this.pointer_track = buf.getTrack();
		this.angle_start = buf.getMapPosition_start().angle;
		this.angle_end = buf.getMapPosition_end().angle;
	}

	public static boolean recognize(MotionEvent e, InteractionBuffer buf)
	{
		if (buf.className != null && buf.className != Rotation.class)
		{
			return false;
		}

		if (e.getPointerCount() != NUM_POINTERS)
		{
			return false;
		}

		buf.X[0] = e.getX(0);
		buf.X[1] = e.getX(1);
		buf.Y[0] = e.getY(0);
		buf.Y[1] = e.getY(1);

		double dx = buf.X[0] - buf.X[1];
		double dy = buf.Y[0] - buf.Y[1];

		double rad = Math.atan2(dy, dx);
		if (Math.abs(rad - buf.preRad) < Rotation.ROTATE_THRESHOLD)
		{
			return false;
		}

		buf.curDistace = Math.sqrt(dx * dx + dy * dy);
		buf.curRad = rad;
		buf.focusX = buf.mapView.getWidth() / 2 - (buf.X[0] + buf.X[1]) / 2;
		buf.focusY = buf.mapView.getHeight() / 2 - (buf.Y[0] + buf.Y[1]) / 2;

		return true;
	}

	public static void execute(InteractionBuffer buf)
	{
		System.out.println("Rotation: Distance: " + buf.curDistace + "|Rad: " + buf.curRad);
		double deltaRad = buf.curRad - buf.preRad;
		buf.mapView.getMapViewPosition().rotateMap(deltaRad, buf.focusX, buf.focusY);
		buf.mapView.redrawMap(true);

		buf.preDistance = buf.curDistace;
		buf.preRad = buf.curRad;
	}

	@Override
	public Element log_XML()
	{
		StringBuilder s = new StringBuilder();
		Element interaction = new Element("Interaction");
		Element pointer, rotation;
		ArrayList<PointF> track;

		interaction.setAttribute("type", "Rotation");
		interaction.setAttribute("start", String.valueOf(this.time_start));
		interaction.setAttribute("end", String.valueOf(this.time_end));

		for (int i = 0; i < NUM_POINTERS; i ++)
		{
			pointer = new Element("Pointer");
			pointer.setAttribute("id", String.valueOf(i + 1));
			s.delete(0, s.length());
			track = this.pointer_track[i];
			for (int j = 0; j < track.size(); j ++)
			{
				s.append(track.get(j).x).append(" ").append(track.get(j).y).append(",");
			}
			s.deleteCharAt(s.length()-1);
			pointer.setText(s.toString());
			interaction.addContent(pointer);
		}

		rotation = new Element("Rotation");
		s.delete(0, s.length());
		s.append(this.angle_start).append(",").append(this.angle_end);
		rotation.setText(s.toString());
		interaction.addContent(rotation);

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

	public ArrayList<PointF>[] getTrack()
	{
		return this.pointer_track;
	}

	public float getRotation_start()
	{
		return this.angle_start;
	}

	public float getRotation_end()
	{
		return this.angle_end;
	}
}
