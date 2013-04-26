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
package org.oscim.utils;

import java.security.MessageDigest;
import java.util.Locale;

public class StringUtils
{
	public static String hashEncrypt(String input, String algorithm, String charsetName)
	{
		MessageDigest md;
		byte[] result = null;
		String output;

		try
		{
			md = MessageDigest.getInstance(algorithm);
			result = md.digest(input.getBytes(charsetName));
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		output = byteToHex(result);

		return output;
	}

	public static String byteToHex(byte[] input)
	{
		StringBuilder output = new StringBuilder();
		String temp;

		if (input == null || input.length <= 0)
		{
			return null;
		}

		for (int i = 0; i < input.length; i ++)
		{
			temp = Integer.toHexString(input[i] & 0xFF);
			if (temp.length() == 1)
			{
				output.append('0');
			}
			output.append(temp);
	    }

		return output.toString().toUpperCase(Locale.ENGLISH);
	}
}
