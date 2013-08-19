/*
 * Copyright 2013
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
package org.oscim.overlay;

import org.oscim.interactions.AutoZoom;
import org.oscim.interactions.InteractionBuffer;
import org.oscim.interactions.InteractionManager;
import org.oscim.layers.InputLayer;
import org.oscim.view.MapView;

import android.view.MotionEvent;

public class MyOverlay extends InputLayer
{
	private final InteractionManager mInteractionManager;
	private InteractionBuffer buf = null;

	public MyOverlay(final MapView mapView)
	{
		super(mapView);
		super.setEnabled(false);
		mInteractionManager = mapView.getInteractionManager();
	}

//	@Override
//	public void setEnabled(boolean enabled)
//	{
//		super.setEnabled(enabled);
//
//		if (enabled)
//		{
//			AutoZoom.enabled = true;
//			Move.enabled = false;
//			Zoom_Rotation.enabled = false;
//			Tilt.enabled = false;
//		}
//		else
//		{
//			AutoZoom.enabled = false;
//			Move.enabled = true;
//			Zoom_Rotation.enabled = true;
//			Tilt.enabled = true;
//		}
//	}

	@Override
	public boolean onTouchEvent(MotionEvent e)
	{
		if (!super.isEnabled())
		{
			return false;
		}

		int action = e.getAction() & MotionEvent.ACTION_MASK;
		int tag;

		if (action == MotionEvent.ACTION_DOWN)
		{
//			System.out.print("my_DOWN: ");
//			for (int i = 0; i < e.getPointerCount(); i ++)
//			{
//				System.out.print("|" + e.getX(i) + "," + e.getY(i));
//			}
//			System.out.println("|");
//			System.out.println(e.getEventTime());

			buf = new InteractionBuffer(e, mMapView);

			tag = AutoZoom.recognize(e, buf);
			if (tag != 0)
			{
				AutoZoom.start(buf, tag);
				buf.className = AutoZoom.class;
			}
			if (tag == -1)
			{
				System.out.println("auto zoom-out");
			}
			else if (tag == 1)
			{
				System.out.println("auto zoom-in");
			}
			else
			{
				System.out.println("invalid auto zoom");
			}

			return true;
		}
//		else if (action == MotionEvent.ACTION_MOVE)
//		{
//			System.out.print("my_MOVE: ");
//			for (int i = 0; i < e.getPointerCount(); i ++)
//			{
//				System.out.print("|" + e.getX(i) + "," + e.getY(i));
//			}
//			System.out.println("|");
//			System.out.println(e.getEventTime());
//
//			tag = AutoZoom.recognize(e, buf);
//			if (tag == 0)
//			{
//				//System.out.println("invalid auto zoom");
//				//AutoZoom.execute(false);
//			}
//			else
//			{
//				if (tag == -1)
//				{
//					System.out.println("auto zoom-out");
//				}
//				else if (tag == 1)
//				{
//					System.out.println("auto zoom-in");
//				}
//				AutoZoom.execute(true);
//			}
//
//			return true;
//		}
		else if (action == MotionEvent.ACTION_UP)
		{
//			System.out.print("UP: ");
//			for (int i = 0; i < e.getPointerCount(); i ++)
//			{
//				System.out.print("|" + e.getX(i) + "," + e.getY(i));
//			}
//			System.out.println("|");
//			System.out.println(e.getEventTime());

			if (AutoZoom.class.equals(buf.className))
			{
				AutoZoom.stop();

				buf.updateEnd(e);
//				System.out.println("class: " + buf.className);
//				System.out.println("start: " + buf.getTime_start());
//				System.out.println("end: " + buf.getTime_end());

				mInteractionManager.save(new AutoZoom(buf));
			}

			return true;
		}
		else if (action == MotionEvent.ACTION_MOVE ||
				 action == MotionEvent.ACTION_POINTER_DOWN ||
				 action == MotionEvent.ACTION_POINTER_UP)
		{
			return true;
		}
		else
		{
			return false;
		}
	}
}
