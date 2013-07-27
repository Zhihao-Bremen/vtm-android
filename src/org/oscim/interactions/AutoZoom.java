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
import org.oscim.view.MapView;

import android.view.MotionEvent;

public class AutoZoom extends Interaction
{
	public static final int NUM_POINTERS = 1;
	public static final float AUTO_ZOOM_OUT = 0.99f;
	public static final float AUTO_ZOOM_IN = 1.01f;

	public static boolean enabled = true;
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
		if (!AutoZoom.enabled)
		{
			return 0;
		}

//		System.out.println("Height: " + buf.mapView.getHeight());
//		System.out.println("Width: " + buf.mapView.getWidth());

//		if ( ( (e.getX() <= 100) || (e.getX() >= buf.mapView.getWidth() - 100) ) &&
//		     (e.getY() >= buf.mapView.getHeight() - 100) )
//		{
//			return 1;
//		}

		if (e.getY(0) >= buf.mapView.getHeight() - 100)
		{
			if (e.getX(0) <= 100)
			{
				return -1;
			}

			if (e.getX(0) >= buf.mapView.getWidth() - 100)
			{
				return 1;
			}
		}

		return 0;
	}

	public static void start(InteractionBuffer buf, int tag)
	{
		AutoZoom.myThread = new AutoZoomThread(buf, tag);

		AutoZoom.myThread.start();
	}

	public static void execute(boolean bool)
	{
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
	}

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
	private final Object pauseLock;
	private boolean isPause, isRun;
	private float scale;
	private final MapView mapView;

	public AutoZoomThread(InteractionBuffer buf, int tag)
	{
		if (tag < 0)
		{
			this.scale = AutoZoom.AUTO_ZOOM_OUT;
		}
		else if (tag > 0)
		{
			this.scale = AutoZoom.AUTO_ZOOM_IN;
		}
		else
		{
			this.scale = 1.0f;
		}
		this.mapView = buf.mapView;
		this.pauseLock = new Object();
		this.isPause = false;
		this.isRun = false;
	}

	public void onPause()
	{
		synchronized (pauseLock)
		{
			this.isPause = true;
		}
	}

	public void onResume()
	{
		synchronized (pauseLock)
		{
			this.isPause = false;
			this.pauseLock.notifyAll();
		}
	}

	public void onStop()
	{
		this.isRun = false;
		//System.out.println("set stop");
	}

	@Override
	public void run()
	{
		boolean changed;

		this.isRun = true;
		//System.out.println("AutoZoom has started..." + this.isRun);
		do
		{
			changed = this.mapView.getMapViewPosition().scaleMap(this.scale, 0, 0);
			this.mapView.redrawMap(true);
			//System.out.println("AutoZoom is running..." + this.isRun);
			try
			{
				Thread.sleep(100);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		while (this.isRun && changed);
		this.isRun = false;
		//System.out.println("AutoZoom has stoped..." + this.isRun);
	}
}
