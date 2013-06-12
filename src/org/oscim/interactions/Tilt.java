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
	private static final float TILT_THRESHOLD = 2.0f;

	public static final int NUM_POINTERS = 2;
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
		if (buf.finished)
		{
			//System.out.println("1");
			return false;
		}

		if (buf.className != null && buf.className != Tilt.class)
		{
			//System.out.println("2");
			return false;
		}

		if (e.getPointerCount() != NUM_POINTERS)
		{
			//System.out.println("3");
			return false;
		}

		float my1 = buf.curY[0] - buf.preY[0];
		float my2 = buf.curY[1] - buf.preY[1];
		if (Math.abs(my1) < TILT_THRESHOLD || Math.abs(my2) < TILT_THRESHOLD)
		{
			//System.out.println("4");
			return false;
		}

		if (Math.tan(buf.curRad) > 1)
		{
			//System.out.println("5");
			return false;
		}

		if (!buf.parallel)
		{
			//System.out.println("6");
			return false;
		}

		buf.tilt = (my1 + my2) / 2;

		if (buf.className == null)
		{
			buf.className = Tilt.class;
		}

		return true;
	}

	public static void execute(InteractionBuffer buf)
	{
//		System.out.println("Tilt");
//		System.out.println("Distance: " + buf.curDistance + "|Rad: " + buf.curRad);
		buf.mapView.getMapViewPosition().tiltMap(buf.tilt / 5);
		buf.mapView.redrawMap(true);

		buf.preX[0] = buf.curX[0];
		buf.preX[1] = buf.curX[1];
		buf.preY[0] = buf.curY[0];
		buf.preY[1] = buf.curY[1];
		buf.preDistance = buf.curDistance;
		buf.preRad = buf.curRad;
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
