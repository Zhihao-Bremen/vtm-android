/*
 * Copyright 2013 Hannes Janetzek
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
package org.oscim.layers;

import org.oscim.core.MapPosition;
import org.oscim.renderer.overlays.RenderOverlay;
import org.oscim.view.MapView;

public class Layer {
	public Layer(MapView mapView) {
		mMapView = mapView;
	}

	private boolean mEnabled = true;
	protected final MapView mMapView;

	/** RenderOverlay used to draw this layer. To be implemented by sub-classes */
	protected RenderOverlay mLayer;

	public RenderOverlay getLayer() {
		return mLayer;
	}

	/**
	 */
	public void setEnabled(boolean pEnabled) {
		mEnabled = pEnabled;
	}

	/**
	 */
	public boolean isEnabled() {
		return mEnabled;
	}


	/**
	 * Called before each frame render request (on main thread).
	 *
	 * @param mapPosition
	 *            current MapPosition
	 * @param changed
	 *            true when MapPosition has changed since last call
	 */
	public void onUpdate(MapPosition mapPosition, boolean changed) {

	}


	/**
	 * Override to perform clean up of resources before shutdown. By default
	 * does nothing.
	 */
	public void onDetach() {
		// FIXME call to this function is not implemented
	}

	public void destroy() {
		// TODO Auto-generated method stub

	}
}
