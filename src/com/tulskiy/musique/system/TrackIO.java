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

import java.util.ArrayList;

import android.annotation.SuppressLint;

import com.tulskiy.musique.audio.AudioFileReader;
import com.tulskiy.musique.audio.AudioTagWriter;
import com.tulskiy.musique.audio.formats.ape.APEFileReader;
import com.tulskiy.musique.audio.formats.ape.APETagWriter;
import com.tulskiy.musique.audio.formats.flac.FLACFileReader;
import com.tulskiy.musique.audio.formats.mp3.MP3FileReader;
import com.tulskiy.musique.audio.formats.mp3.MP3TagWriter;
import com.tulskiy.musique.audio.formats.wav.WAVFileReader;
import com.tulskiy.musique.model.TrackData;
import com.tulskiy.musique.util.Util;

/**
 * Author: Denis Tulskiy Date: Jun 22, 2010
 */
public class TrackIO {
	private static ArrayList<AudioFileReader> readers;
	private static ArrayList<AudioTagWriter> writers;

	static {
		readers = new ArrayList<AudioFileReader>();
		readers.add(new MP3FileReader());
		readers.add(new APEFileReader());
		readers.add(new WAVFileReader());
		readers.add(new FLACFileReader());

		writers = new ArrayList<AudioTagWriter>();
		writers.add(new MP3TagWriter());
		writers.add(new APETagWriter());

	}

	public static AudioFileReader getAudioFileReader(String fileName) {
		String ext = Util.getFileExt(fileName);
		for (AudioFileReader reader : readers) {
			if (reader.isFileSupported(ext))
				return reader;
		}

		return null;
	}

	@SuppressLint("DefaultLocale")
	public static AudioTagWriter getAudioFileWriter(String fileName) {
		String ext = Util.getFileExt(fileName).toLowerCase();
		for (AudioTagWriter writer : writers) {
			if (writer.isFileSupported(ext))
				return writer;
		}

		return null;
	}

	public static void write(TrackData trackData) {
		if (trackData.isFile()) {
			AudioTagWriter writer = TrackIO.getAudioFileWriter(trackData
					.getFile().getName());
			if (writer != null)
				try {
					writer.write(trackData);
				} catch (com.tulskiy.musique.audio.TagWriteException e) {
					e.printStackTrace();
				}
		}
	}
}
