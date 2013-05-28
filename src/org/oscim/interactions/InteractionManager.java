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
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.XMLOutputter;

import android.util.Log;
//import org.jdom2.Element;

public final class InteractionManager
{
	//private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.GERMANY);

	private final ArrayList<Interaction> interactionList;
	private final String id;
	private final double lon, lat;

	// Constructs an empty action manager with an initial capacity of ten.
	public InteractionManager(String deviceId, double lon, double lat)
	{
		this.interactionList = new ArrayList();
		this.id = deviceId;
		this.lon = lon;
		this.lat = lat;
	}

	public boolean save(Interaction interaction)
	{
		if (interaction == null)
		{
			Log.d("InteractionManager", "save failed because of null interaction");
			return false;
		}

		return this.interactionList.add(interaction);
	}

	public Interaction getInteraction(int index)
	{
		if (index >= this.interactionList.size())
		{
			return null;
		}
		return this.interactionList.get(index);
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

//	public boolean importFromXML(String path)
//	{
//		File file;
//		SAXBuilder saxBuilder;
//		Document document;
//		Element root, interaction;
//		List interactions, pointers;
//		ArrayList<PointF> pointers_move[];
//		long time_start, time_end;
//		PointF pointers_start[], pointers_end[];
//		String coords[], temp[];
//
//		file = new File(path);
//		saxBuilder = new SAXBuilder();
//		try
//		{
//			document = saxBuilder.build(file);
//		} catch (Exception e)
//		{
//			Log.d("InteractionManager", "file " + path + " cannot be imported");
//			e.printStackTrace();
//			return false;
//		}
//		root = document.getRootElement();
//		interactions = root.getChildren("Interaction");
//
//		for (int i = 0; i < interactions.size(); i++)
//		{
//			interaction = (Element) interactions.get(i);
//			time_start = Long.parseLong(interaction.getAttributeValue("start"));
//			time_end = Long.parseLong(interaction.getAttributeValue("end"));
//
//			pointers = interaction.getChildren("Pointer");
//			pointers_start = new PointF[pointers.size()];
//			pointers_move = new ArrayList[pointers.size()];
//			pointers_end = new PointF[pointers.size()];
//			for (int j = 0; j < pointers.size(); j++)
//			{
//				coords = ((Element) pointers.get(j)).getText().split(" --> ");
//
//				temp = coords[0].substring(1, coords[0].length() - 1).split(", ");
//				pointers_start[j] = new PointF(Float.parseFloat(temp[0]), Float.parseFloat(temp[1]));
//
//				temp = coords[1].substring(1, coords[1].length() - 1).split(", ");
//				pointers_end[j] = new PointF(Float.parseFloat(temp[0]), Float.parseFloat(temp[1]));
//			}
//
//			//interactionList.add(new Interaction(pointers.size(), pointers_start, pointers_move, pointers_end, time_start, time_end));
//		}
//
//		return true;
//	}

	public boolean exportToXML(String path)
	{
		File file;
		Document document;
		Element root, interaction;
		XMLOutputter xmlOutputter;
		Interaction data;

		file = new File(path);
		root = new Element("InteractionManager");
		root.setAttribute("device", this.id);
		root.setAttribute("lat", String.valueOf(this.lat));
		root.setAttribute("lon", String.valueOf(this.lon));
		document = new Document(root);
		xmlOutputter = new XMLOutputter();

		for (int i = 0; i < this.interactionList.size(); i++)
		{
			data = this.interactionList.get(i);

			interaction = data.log_XML();
			root.addContent(interaction);
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
