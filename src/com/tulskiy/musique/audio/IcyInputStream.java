/*
 * Copyright (c) 2008, 2009, 2010, 2011 Denis Tulskiy
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * version 3 along with this work.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.tulskiy.musique.audio;

import java.io.BufferedInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;

import org.jaudiotagger.tag.FieldKey;

import android.annotation.SuppressLint;

import com.tulskiy.musique.model.TrackData;

/**
 * Author: Denis Tulskiy Date: 4/10/11
 */
public class IcyInputStream extends FilterInputStream {

	private TrackData trackData;
	private int metaInt = 0;
	private int bytesRead = 0;
	private URLConnection connection;
	private String contentType;

	public static IcyInputStream create(TrackData trackData) {
		try {
			URLConnection connection = trackData.getLocation().toURL()
					.openConnection();
			connection.setRequestProperty("Icy-Metadata", "1");
			IcyInputStream icyInputStream = new IcyInputStream(
					new BufferedInputStream(connection.getInputStream()));
			icyInputStream.setTrackData(trackData);
			icyInputStream.setConnection(connection);
			icyInputStream.init();
			return icyInputStream;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	private IcyInputStream(InputStream in) {
		super(in);
	}

	private void setConnection(URLConnection connection) {
		this.connection = connection;
	}

	private void setTrackData(TrackData trackData) {
		this.trackData = trackData;
	}

	public String getContentType() {
		return contentType;
	}

	public String readLine() {
		try {
			int ch = read();

			StringBuilder sb = new StringBuilder();
			while (ch != '\n' && ch != '\r' && ch >= 0) {
				sb.append((char) ch);
				ch = read();
			}

			if (ch == '\n' || ch == '\r') {
				// noinspection ResultOfMethodCallIgnored
				read();
			}
			return sb.toString();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	@SuppressLint("NewApi")
	private void init() {
		contentType = connection.getContentType();
		String metaIntString = "0";
		if (contentType.equals("unknown/unknown")) {
			// Java does not parse non-standart headers
			// used by SHOUTCast
			String s = readLine();
			if (!s.equals("ICY 200 OK")) {
				return;
			}

			while (true) {
				s = readLine();

				if (s.isEmpty()) {
					break;
				}

				String[] ss = s.split(":");
				if (ss[0].equals("icy-metaint")) {
					metaIntString = ss[1];
				} else if (ss[0].equals("icy-genre")) {
					trackData.addGenre(ss[1]);
				} else if (ss[0].equals("icy-name")) {
					trackData.addAlbum(ss[1]);
				} else if (ss[0].equals("content-type")) {
					contentType = ss[1];
				}
			}
		} else {
			metaIntString = connection.getHeaderField("icy-metaint");
			trackData.addGenre(connection.getHeaderField("icy-genre"));
			trackData.addAlbum(connection.getHeaderField("icy-name"));
		}
		try {
			metaInt = Integer.parseInt(metaIntString.trim());
		} catch (NumberFormatException e) {
			e.printStackTrace();
			metaInt = 0;
		}
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		if (metaInt > 0) {
			int bytesToMeta = metaInt - bytesRead;
			if (bytesToMeta == 0) {
				readMeta();
			}

			if (bytesToMeta > 0 && bytesToMeta < len) {
				len = bytesToMeta;
			}
		}

		int read = super.read(b, off, len);
		bytesRead += read;
		return read;
	}

	private void readMeta() throws IOException {
		int size = read() * 16;
		if (size > 1) {
			byte[] meta = new byte[size];
			int i = super.read(meta, 0, size);
			String metaString = new String(meta, 0, i, "UTF-8");
			String title = "StreamTitle='";
			if (metaString.startsWith(title)) {
				String[] ss = metaString.substring(title.length(),
						metaString.indexOf(";") - 1).split(" - ");
				if (ss.length > 0) {
					if (ss.length > 1) {
						trackData.setTagFieldValues(FieldKey.ARTIST, ss[0]);
						trackData.setTagFieldValues(FieldKey.TITLE, ss[1]);
					} else {
						trackData.setTagFieldValues(FieldKey.TITLE, ss[1]);
					}
				}
			}
		}
		bytesRead = 0;
	}
}
