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
package org.oscim.interactions;

import org.jdom2.Element;
import org.oscim.core.MapPosition;
import org.oscim.view.MapView;

import android.view.MotionEvent;

public class AutoZoom extends Interaction
{
	public static final int NUM_POINTERS = 1;
	public static final float AUTO_ZOOM_OUT = 0.99f;
	public static final float AUTO_ZOOM_IN = 1.01f;

	//public static boolean enabled = true;
	private static long speed = 10;
	private static char mode = 'R'; //default mode is right-hand
	private static AutoZoomThread myThread;
	private final long time_start, time_end;

	public AutoZoom(InteractionBuffer buf)
	{
		this.time_start = buf.getTime_start();
		this.time_end = buf.getTime_end();
	}

	/**
	 * @return -1 automatic zoom-out;
	 *          0 invalid automatic zoom;
	 *         +1 automatic zoom-in.
	 */
	public static int recognize(MotionEvent e, InteractionBuffer buf)
	{
//		if (!AutoZoom.enabled)
//		{
//			return 0;
//		}

//		System.out.println("Height: " + buf.mapView.getHeight());
//		System.out.println("Width: " + buf.mapView.getWidth());

//		if ( ( (e.getX() <= 100) || (e.getX() >= buf.mapView.getWidth() - 100) ) &&
//		     (e.getY() >= buf.mapView.getHeight() - 100) )
//		{
//			return 1;
//		}

		int tag = 0;
		if (e.getY(0) >= buf.mapView.getHeight() - 100)
		{
			if (e.getX(0) <= 100)
			{
				if (mode == 'L')
				{
					tag = 1;
				}
				else
				{
					tag = -1;
				}
			}

			if (e.getX(0) >= buf.mapView.getWidth() - 100)
			{
				if (mode == 'L')
				{
					tag = -1;
				}
				else
				{
					tag = 1;
				}
			}
		}

		return tag;
	}

	public static void start(InteractionBuffer buf, int tag)
	{
		AutoZoom.myThread = new AutoZoomThread(buf, tag);

		AutoZoom.myThread.start();
	}

//	public static void execute(boolean bool)
//	{
//		if (bool)
//		{
//			if (myThread.)
//			{}
//		}
//		else
//		{
//			if ()
//			{}
//		}
//	}

	public static void stop()
	{
		if (AutoZoom.myThread != null)
		{
			AutoZoom.myThread.onStop();
		}
	}

	@Override
	public Element log_XML()
	{
		Element interaction = new Element("Interaction");

		interaction.setAttribute("type", "AutoZoom");
		interaction.setAttribute("start", String.valueOf(this.time_start));
		interaction.setAttribute("end", String.valueOf(this.time_end));

		return interaction;
	}

	public static void setMode(String mode)
	{
		if (mode.equals("LEFT"))
		{
			AutoZoom.mode = 'L';
		}
		else
		{
			AutoZoom.mode = 'R';
		}
	}

	public static char getMode()
	{
		return AutoZoom.mode;
	}

	public static void setSpeed(long time)
	{
		AutoZoom.speed = time;
	}

	public static long getSpeed()
	{
		return AutoZoom.speed;
	}

	public long getTime_start()
	{
		return this.time_start;
	}

	public long getTime_end()
	{
		return this.time_end;
	}

}

class AutoZoomThread extends Thread
{
//	private final Object pauseLock;
//	private boolean isPause;
	private boolean isRun;
	private float scale_forward, scale_backward;
	private final long sleepTime;
	private final MapView mapView;
	private final MapPosition original_pos = new MapPosition();
	private final MapPosition current_pos = new MapPosition();

	public AutoZoomThread(InteractionBuffer buf, int tag)
	{
		if (tag < 0)
		{
			this.scale_forward = AutoZoom.AUTO_ZOOM_OUT;
			this.scale_backward = 1 / this.scale_forward;
		}
		else if (tag > 0)
		{
			this.scale_forward = AutoZoom.AUTO_ZOOM_IN;
			this.scale_backward = 1 / this.scale_forward;
		}
		else
		{
			this.scale_forward = this.scale_backward = 1.0f;
		}
		this.sleepTime = AutoZoom.getSpeed();
		this.mapView = buf.mapView;
		this.mapView.getMapViewPosition().getMapPosition(this.original_pos);
//		this.pauseLock = new Object();
//		this.isPause = false;
		this.isRun = false;
	}

//	public void onPause()
//	{
//		synchronized (pauseLock)
//		{
//			this.isPause = true;
//		}
//	}

//	public void onResume()
//	{
//		synchronized (pauseLock)
//		{
//			this.isPause = false;
//			this.pauseLock.notifyAll();
//		}
//	}

	public void onStop()
	{
		this.isRun = false;
		System.out.println("set stop");
	}

	@Override
	public void run()
	{
		boolean changed;
		int n;

		this.isRun = true;
		n = 0;
		System.out.println("AutoZoom has started..." + this.isRun);
		this.mapView.getMapViewPosition().getMapPosition(this.current_pos);
		System.out.println("ZoomLevel_" + n + ": " + this.current_pos.zoomLevel);
		System.out.println("Scale_" + n + ": " + this.current_pos.scale);
		do
		{
			n ++;
			changed = this.mapView.getMapViewPosition().scaleMap(this.scale_forward, 0, 0);
			this.mapView.redrawMap(true);
			this.mapView.getMapViewPosition().getMapPosition(this.current_pos);
			System.out.println("AutoZoom is running in part 1..." + this.isRun);
			System.out.println("ZoomLevel_" + n + ": " + this.current_pos.zoomLevel);
			System.out.println("Scale_" + n + ": " + this.current_pos.scale);
			try
			{
				Thread.sleep(this.sleepTime);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		while (this.isRun && changed);

		while (this.isRun)
		{
			System.out.println("AutoZoom is running in part 2..." + this.isRun);
		}

		System.out.println("original ZoomLevel: " + this.original_pos.zoomLevel);
		System.out.println("original Scale: " + this.original_pos.scale);

		n = 0;
		this.mapView.getMapViewPosition().getMapPosition(this.current_pos);
		System.out.println("ZoomLevel_" + n + ": " + this.current_pos.zoomLevel);
		System.out.println("Scale_" + n + ": " + this.current_pos.scale);
		while ( !this.original_pos.equals(this.current_pos) )
		{
			n ++;
			this.mapView.getMapViewPosition().scaleMap(this.scale_backward, 0, 0);
			this.mapView.redrawMap(true);
			this.mapView.getMapViewPosition().getMapPosition(this.current_pos);
			System.out.println("AutoZoom is running in part 3..." + this.isRun);
			System.out.println("ZoomLevel_" + n + ": " + this.current_pos.zoomLevel);
			System.out.println("Scale_" + n + ": " + this.current_pos.scale);
			try
			{
				Thread.sleep(1);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		System.out.println("AutoZoom has stoped..." + this.isRun);
	}
}
