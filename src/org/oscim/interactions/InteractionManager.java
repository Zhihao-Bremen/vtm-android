/*
 * Copyright 2012
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.XMLOutputter;
import org.oscim.core.MapPosition;
import org.oscim.core.PointF;
import org.oscim.utils.StringUtils;

import android.util.Log;
import android.view.MotionEvent;

public final class InteractionManager
{
	//private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.GERMANY);

	private final ArrayList<Interaction> interactionList;
	private String ID;
	private double lat, lon;

	// Constructs an empty action manager with an initial capacity of ten.
	public InteractionManager(String deviceId)
	{
		this.interactionList = new ArrayList<Interaction>();
		try
		{
			this.ID = StringUtils.hashEncrypt(deviceId, "SHA-1", "US-ASCII");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	// Constructs an empty action manager with the specified initial capacity.
	public InteractionManager(int initialCapacity, String deviceId)
	{
		this.interactionList = new ArrayList<Interaction>(initialCapacity);
		try
		{
			this.ID = StringUtils.hashEncrypt(deviceId, "SHA-1", "US-ASCII");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public static Interaction analyze(MotionEvent e)
	{
		return null;
	}

	public void save(Interaction interaction)
	{
		if (interaction == null)
		{
			Log.d("InteractionManager", "save failed because of null interaction");
		}
		else
		{
			this.interactionList.add(interaction);
		}
	}

	public Interaction getInteraction(int index)
	{
		if (index >= this.interactionList.size())
		{
			return null;
		}
		return this.interactionList.get(index - 1);
	}

	public int getNumOfActions()
	{
		return this.interactionList.size();
	}

	public double getLat()
	{
		return this.lat;
	}

	public double getLon()
	{
		return this.lon;
	}

	//	public void showAction(int index)
	//	{
	//		if (index < interactionList.size())
	//		{
	//			Interaction i = interactionList.get(index);
	//			Log.d("InteractionManager",
	//					SDF.format(new Date(i.getStarttime())) + " / "
	//							+ SDF.format(new Date(i.getEndtime())));
	//
	//			PointF start, end;
	//			for (int n = 0; n < i.getNumOfPointers(); n++)
	//			{
	//				start = i.getPointer_start(n);
	//				end = i.getPointer_end(n);
	//				Log.d("Interaction", "Pointer " + String.valueOf(n + 1) + ": (" + start.x + ", "
	//						+ start.y + ") --> (" + end.x + ", " + end.y + ")");
	//			}
	//		}
	//		else
	//		{
	//			Log.d("InteractionManager", "Warning: This index " + index + " is invaild.");
	//		}
	//	}
	//
	//	public void showAllActions()
	//	{
	//		Interaction i;
	//		Iterator<Interaction> it = interactionList.iterator();
	//
	//		while (it.hasNext())
	//		{
	//			i = it.next();
	//			Log.d("InteractionManager",
	//					SDF.format(new Date(i.getStarttime())) + " / "
	//							+ SDF.format(new Date(i.getEndtime())));
	//
	//			PointF start, end;
	//			for (int n = 0; n < i.getNumOfPointers(); n++)
	//			{
	//				start = i.getPointer_start(n);
	//				end = i.getPointer_end(n);
	//				Log.d("InteractionManager", "Pointer " + String.valueOf(n + 1) + ": (" + start.x
	//						+ ", " + start.y + ") --> (" + end.x + ", " + end.y + ")");
	//			}
	//		}
	//	}

	public boolean importFromXML(String path)
	{
		File file;
		SAXBuilder saxBuilder;
		Document document;
		Element root, interaction;
		List interactions, pointers;
		ArrayList<PointF> pointers_move[];
		long time_start, time_end;
		PointF pointers_start[], pointers_end[];
		String coords[], temp[];

		file = new File(path);
		saxBuilder = new SAXBuilder();
		try
		{
			document = saxBuilder.build(file);
		} catch (Exception e)
		{
			Log.d("InteractionManager", "file " + path + " cannot be imported");
			e.printStackTrace();
			return false;
		}
		root = document.getRootElement();
		interactions = root.getChildren("Interaction");

		for (int i = 0; i < interactions.size(); i++)
		{
			interaction = (Element) interactions.get(i);
			time_start = Long.parseLong(interaction.getAttributeValue("start"));
			time_end = Long.parseLong(interaction.getAttributeValue("end"));

			pointers = interaction.getChildren("Pointer");
			pointers_start = new PointF[pointers.size()];
			pointers_move = new ArrayList[pointers.size()];
			pointers_end = new PointF[pointers.size()];
			for (int j = 0; j < pointers.size(); j++)
			{
				coords = ((Element) pointers.get(j)).getText().split(" --> ");

				temp = coords[0].substring(1, coords[0].length() - 1).split(", ");
				pointers_start[j] = new PointF(Float.parseFloat(temp[0]), Float.parseFloat(temp[1]));

				temp = coords[1].substring(1, coords[1].length() - 1).split(", ");
				pointers_end[j] = new PointF(Float.parseFloat(temp[0]), Float.parseFloat(temp[1]));
			}

			//interactionList.add(new Interaction(pointers.size(), pointers_start, pointers_move, pointers_end, time_start, time_end));
		}

		return true;
	}

	public boolean export_XML(String path)
	{
		File file;
		Document document;
		Element root, interaction, pointer, center, zoomLevel, scale, rotation, tilt;
		XMLOutputter xmlOutputter;
		Interaction data;
		ArrayList<PointF> moveList;
		MapPosition position_start, position_end;
		StringBuilder s = new StringBuilder();

		file = new File(path);
		root = new Element("InteractionManager");
		root.setAttribute("device", this.ID);
		root.setAttribute("lat", String.valueOf(this.lat));
		root.setAttribute("lon", String.valueOf(this.lon));
		document = new Document(root);
		xmlOutputter = new XMLOutputter();

		for (int i = 0; i < this.interactionList.size(); i++)
		{
			data = this.interactionList.get(i);

			interaction = new Element("Interaction");
			interaction.setAttribute("start", String.valueOf(data.getStarttime()));
			interaction.setAttribute("end", String.valueOf(data.getEndtime()));
			root.addContent(interaction);

			for (int j = 0; j < data.getNumOfPointers(); j++)
			{
				pointer = new Element("Pointer");
				pointer.setAttribute("id", String.valueOf(j + 1));

				s.delete(0, s.length());
//				s.append(data.getPointer_start(j).x).append(" ").append(data.getPointer_start(j).y)
//						.append(",");
				moveList = data.getPointer_track(j);
				for (int k = 0; k < moveList.size(); k++)
				{
					s.append(moveList.get(k).x).append(" ").append(moveList.get(k).y).append(",");
				}
//				s.append(data.getPointer_end(j).x).append(" ").append(data.getPointer_end(j).y);

				pointer.setText(s.toString());
				interaction.addContent(pointer);
			}

			position_start = data.getMapPosition_start();
			position_end = data.getMapPosition_end();

			center = new Element("Center");
			s.delete(0, s.length());
			//s.append(position_start.lat).append(" ").append(position_start.lon);
			s.append(",");
			//s.append(position_end.lat).append(" ").append(position_end.lon);
			center.setText(s.toString());
			interaction.addContent(center);

			zoomLevel = new Element("ZoomLevel");
			s.delete(0, s.length());
			s.append(position_start.zoomLevel).append(",").append(position_end.zoomLevel);
			zoomLevel.setText(s.toString());
			interaction.addContent(zoomLevel);

			scale = new Element("Scale");
			s.delete(0, s.length());
			s.append(position_start.scale).append(",").append(position_end.scale);
			scale.setText(s.toString());
			interaction.addContent(scale);

			rotation = new Element("Rotation");
			s.delete(0, s.length());
			s.append(position_start.angle).append(",").append(position_end.angle);
			rotation.setText(s.toString());
			interaction.addContent(rotation);

			tilt = new Element("Tilt");
			s.delete(0, s.length());
			s.append(position_start.tilt).append(",").append(position_end.tilt);
			tilt.setText(s.toString());
			interaction.addContent(tilt);
		}

		try
		{
			xmlOutputter.output(document, new FileOutputStream(file));
		} catch (Exception e)
		{
			Log.d("InteractionManager", "data cannot be exported");
			e.printStackTrace();
			return false;
		}

		Log.d("InteractionManager", "export is completed");
		return true;
	}

	public void sendToServer(String path, String host, int port)
	{
		new FileSender(path, host, port).start();
	}

	private class FileSender extends Thread
	{
		File file;
		String host;
		int port;

		public FileSender(String path, String host, int port)
		{
			this.file = new File(path);
			this.host = host;
			this.port = port;
		}

		@Override
		public void run()
		{
			if (!sendFile())
			{
				Log.d("InteractionManager", "sending is failed");
			}
			else
			{
				Log.d("InteractionManager", "file has been sent");
			}
		}

		private boolean sendFile()
		{
			FileInputStream in = null;
			ZipOutputStream out = null;
			Socket socket = null;
			byte[] buffer = new byte[1024];

			try
			{
				if (!file.exists())
				{
					Log.d("InteractionManager", file.getAbsolutePath() + " can not be found");
					return false;
				}
				in = new FileInputStream(file);

				socket = new Socket(host, port);
				//System.out.println("socket is created");
				out = new ZipOutputStream(socket.getOutputStream());
				out.putNextEntry(new ZipEntry("interactions.xml"));

				while (true)
				{
					int readedBytes = in.read(buffer);
					if (readedBytes == -1)
					{
						break;
					}
					out.write(buffer, 0, readedBytes);
					out.flush();
				}
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			} finally {
				try
				{
					if (in != null)
					{
						in.close();
					}
					if (out != null)
					{
						out.closeEntry();
						out.close();
					}
					if (socket != null)
					{
						socket.close();
					}
				} catch (Exception e)
				{
					e.printStackTrace();
				}
			}

			//file.delete();
			return true;
		}
	}

}
