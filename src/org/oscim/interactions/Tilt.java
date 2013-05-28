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

public class Tilt extends Interaction
{
	public static final float TILT_THRESHOLD = 1f;
	public static final int NUM_POINTERS = 3;
	private final long time_start, time_end;
	private final ArrayList<PointF>[] pointer_track;
	private final float tilt_start, tilt_end;

	public Tilt(InteractionBuffer buf)
	{
		assert buf.getPointerNum() == NUM_POINTERS;

		this.time_start = buf.getTime_start();
		this.time_end = buf.getTime_end();
		this.pointer_track = buf.getTrack();
		this.tilt_start = buf.getMapPosition_start().tilt;
		this.tilt_end = buf.getMapPosition_end().tilt;
	}

	public static boolean recognize(MotionEvent e, InteractionBuffer buf)
	{
		if (buf.className != null && buf.className != Tilt.class)
		{
			return false;
		}

		if (e.getPointerCount() != NUM_POINTERS)
		{
			return false;
		}

		float X[] = new float[3];
		float Y[] = new float[3];

		X[0] = e.getX(0);
		X[1] = e.getX(1);
		X[2] = e.getX(2);
		Y[0] = e.getY(0);
		Y[1] = e.getY(1);
		Y[2] = e.getY(2);

		float mx1 = X[0] - buf.X[0];
		float my1 = Y[0] - buf.Y[0];
		float mx2 = X[1] - buf.X[1];
		float my2 = Y[1] - buf.Y[1];
		float mx3 = X[2] - buf.X[2];
		float my3 = Y[2] - buf.Y[2];
		if (Math.abs(my1) < TILT_THRESHOLD || Math.abs(my2) < TILT_THRESHOLD || Math.abs(my3) < TILT_THRESHOLD)
		{
			return false;
		}

//		double rad1 = Math.atan2(my1, mx1);
//		double rad2 = Math.atan2(my2, mx2);
//		double rad3 = Math.atan2(my3, mx3);
//		if (Math.abs(rad1 - rad2) > Rotation.ROTATE_THRESHOLD ||
//			Math.abs(rad1 - rad3) > Rotation.ROTATE_THRESHOLD ||
//			Math.abs(rad2 - rad3) > Rotation.ROTATE_THRESHOLD)
//		{
//			return false;
//		}

//		float dx = X[0] - X[1];
//		float dy = Y[0] - Y[1];
//		float slope = 0;
//		if (dx != 0)
//		{
//			slope = dy / dx;
//		}
//		if (Math.abs(slope) > 1)
//		{
//			return false;
//		}

		buf.X[0] = X[0];
		buf.X[1] = X[1];
		buf.X[2] = X[2];
		buf.Y[0] = Y[0];
		buf.Y[1] = Y[1];
		buf.Y[2] = Y[2];

		buf.tilt = my1 + my2 + my3;

		return true;
	}

	public static void execute(InteractionBuffer buf)
	{
		System.out.println("Tilt: " + buf.tilt);
		buf.mapView.getMapViewPosition().tiltMap(buf.tilt / 15);
		buf.mapView.redrawMap(true);
	}

	@Override
	public Element log_XML()
	{
		StringBuilder s = new StringBuilder();
		Element interaction = new Element("Interaction");
		Element pointer, tilt;
		ArrayList<PointF> track;

		interaction.setAttribute("type", "Tilt");
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

		tilt = new Element("Tilt");
		s.delete(0, s.length());
		s.append(this.tilt_start).append(",").append(this.tilt_end);
		tilt.setText(s.toString());
		interaction.addContent(tilt);

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

	public float getTilt_start()
	{
		return this.tilt_start;
	}

	public float getTilt_end()
	{
		return this.tilt_end;
	}
}
