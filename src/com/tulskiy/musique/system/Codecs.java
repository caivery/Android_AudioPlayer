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

package com.tulskiy.musique.system;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Set;
import java.util.logging.Logger;

import com.tulskiy.musique.audio.Decoder;
import com.tulskiy.musique.audio.Encoder;
import com.tulskiy.musique.audio.IcyInputStream;
import com.tulskiy.musique.audio.formats.ape.APEDecoder;
import com.tulskiy.musique.audio.formats.ape.APEEncoder;
import com.tulskiy.musique.audio.formats.flac.FLACDecoder;
import com.tulskiy.musique.audio.formats.flac.FLACEncoder;
import com.tulskiy.musique.audio.formats.mp3.MP3Decoder;
import com.tulskiy.musique.audio.formats.wav.WAVDecoder;
import com.tulskiy.musique.audio.formats.wav.WAVEncoder;
import com.tulskiy.musique.model.TrackData;
import com.tulskiy.musique.util.Util;

/**
 * Author: Denis Tulskiy Date: 24.06.2009
 */
public class Codecs {
	private static HashMap<String, Decoder> decoders = new HashMap<String, Decoder>();
	private static HashMap<String, Encoder> encoders = new HashMap<String, Encoder>();
	private static final Logger logger = Logger.getLogger(Codecs.class
			.getName());

	static {
		decoders.put("mp3", new MP3Decoder());
		decoders.put("wav", new WAVDecoder());
		decoders.put("flac", new FLACDecoder());
		decoders.put("ape", new APEDecoder());

		encoders.put("wav", new WAVEncoder());
		encoders.put("ape", new APEEncoder());
		encoders.put("flac", new FLACEncoder());
	}

	public static Decoder getDecoder(TrackData trackData) {
		URI location = trackData.getLocation();
		if (location == null) {
			return null;
		}
		if (trackData.isStream()) {
			IcyInputStream inputStream = IcyInputStream.create(trackData);
			String contentType = inputStream.getContentType().trim();
			try {
				inputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

			if ("audio/mpeg".equals(contentType)) {
				return decoders.get("mp3");
			}

			if ("application/ogg".equals(contentType)) {
				return decoders.get("ogg");
			}

			if ("audio/aac".equals(contentType)) {
				return decoders.get("aac");
			}
			logger.warning("Unsupported ContentType: " + contentType);
			return null;
		}
		String ext = Util.getFileExt(location.toString()).toLowerCase();
		return decoders.get(ext);
	}

	public static Decoder getNewDecoder(TrackData trackData) {
		try {
			return getDecoder(trackData).getClass().newInstance();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}

		return null;
	}

	public static Encoder getEncoder(String format) {
		return encoders.get(format);
	}

	public static Set<String> getFormats() {
		return decoders.keySet();
	}
}
