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
	private static final double ROTATE_THRESHOLD = Math.PI / 36.0; //5 degree

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
		if (buf.finished)
		{
			//System.out.println("1");
			return false;
		}

		if (buf.className != null && buf.className != Rotation.class)
		{
			//System.out.println("2");
			return false;
		}

		if (e.getPointerCount() != NUM_POINTERS)
		{
			//System.out.println("3");
			return false;
		}

		if (Math.abs(buf.curRad - buf.preRad) < ROTATE_THRESHOLD)
		{
			//System.out.println("4");
			return false;
		}

		if (buf.parallel)
		{
			//System.out.println("5");
			return false;
		}

		double D_0 = buf.A * buf.curX[0] + buf.B * buf.curY[0] + buf.C;
		double D_1 = buf.A * buf.curX[1] + buf.B * buf.curY[1] + buf.C;
		if (D_0 * D_1 > 0.0)
		{
			//System.out.println("6");
			return false;
		}

		if ( (Math.abs(buf.cos_1) >= InteractionBuffer.ZOOM_ROTATION_THRESHOLD && Math.abs(buf.cos_2) >= InteractionBuffer.ZOOM_ROTATION_THRESHOLD) ||
			 (buf.cos_1 == Double.NaN && Math.abs(buf.cos_2) >= InteractionBuffer.ZOOM_ROTATION_THRESHOLD) ||
		     (buf.cos_2 == Double.NaN && Math.abs(buf.cos_1) >= InteractionBuffer.ZOOM_ROTATION_THRESHOLD) )
		{
			//System.out.println("7");
			return false;
		}

		buf.focusX = buf.mapView.getWidth() / 2 - (buf.curX[0] + buf.curX[1]) / 2;
		buf.focusY = buf.mapView.getHeight() / 2 - (buf.curY[0] + buf.curY[1]) / 2;

		if (buf.className == null)
		{
			buf.className = Rotation.class;
		}

		return true;
	}

	public static void execute(InteractionBuffer buf)
	{
//		System.out.println("Rotation");
//		System.out.println("Distance: " + buf.curDistance + "|Rad: " + buf.curRad);
		buf.mapView.getMapViewPosition().rotateMap(buf.curRad - buf.preRad, buf.focusX, buf.focusY);
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
