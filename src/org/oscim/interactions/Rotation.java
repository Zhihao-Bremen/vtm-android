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
	public static final int NUM_POINTERS = 2;
	private static final double ROTATE_THRESHOLD = Math.PI / 36.0; //5 degree

	public static boolean enabled = true;
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
		if (!Rotation.enabled)
		{
			return false;
		}

		if (e.getPointerCount() != Rotation.NUM_POINTERS)
		{
			return false;
		}
		else if (buf.className == null)
		{
			if (buf.parallel)
			{
				return false;
			}

			if (buf.same_side > 0.0)
			{
				return false;
			}
			else if (buf.same_side == 0.0)
			{
				if (Math.abs(buf.cos_1) >= InteractionBuffer.ZOOM_ROTATION_THRESHOLD ||
				    Math.abs(buf.cos_2) >= InteractionBuffer.ZOOM_ROTATION_THRESHOLD)
				{
					return false;
				}
			}
			else
			{
				if (Math.abs(buf.cos_1) >= InteractionBuffer.ZOOM_ROTATION_THRESHOLD &&
				    Math.abs(buf.cos_2) >= InteractionBuffer.ZOOM_ROTATION_THRESHOLD)
				{
					return false;
				}
			}

			buf.className = Rotation.class;
			return true;
		}
		else if (buf.className == Rotation.class)
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
		if (Math.abs(buf.curRad - buf.preRad) >= ROTATE_THRESHOLD)
		{
//			System.out.println("Rotation");
//			System.out.println("Distance: " + buf.curDistance + "|Rad: " + buf.curRad);
			float x = buf.mapView.getWidth() / 2 - (buf.curX[0] + buf.curX[1]) / 2;
			float y = buf.mapView.getHeight() / 2 - (buf.curY[0] + buf.curY[1]) / 2;
			buf.mapView.getMapViewPosition().rotateMap(buf.curRad - buf.preRad, x, y);
			buf.mapView.redrawMap(true);

			buf.preX[0] = buf.curX[0];
			buf.preX[1] = buf.curX[1];
			buf.preY[0] = buf.curY[0];
			buf.preY[1] = buf.curY[1];
			buf.preDistance = buf.curDistance;
			buf.preRad = buf.curRad;
		}
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
