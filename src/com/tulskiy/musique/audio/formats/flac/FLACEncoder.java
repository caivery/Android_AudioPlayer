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

package com.tulskiy.musique.audio.formats.flac;

import java.io.File;
import java.io.IOException;

import javaFlacEncoder.EncodingConfiguration;
import javaFlacEncoder.FLACFileOutputStream;
import javaFlacEncoder.StreamConfiguration;

import com.tulskiy.musique.audio.Encoder;
import com.tulskiy.musique.model.TrackData;
import com.tulskiy.musique.util.AudioMath;

/**
 * Author: Denis Tulskiy Date: 5/28/11
 */
public class FLACEncoder implements Encoder {

	private javaFlacEncoder.FLACEncoder encoder;
	private FLACFileOutputStream outputStream;
	private int[] buffer = new int[65536];
	private TrackData trackData;
	private int unencodedSamples;

	public boolean open(File outputFile, TrackData trackData) {
		try {
			this.trackData = trackData;
			encoder = new javaFlacEncoder.FLACEncoder();
			StreamConfiguration streamConfiguration = new StreamConfiguration();
			streamConfiguration.setBitsPerSample(trackData.getBps());
			streamConfiguration.setChannelCount(trackData.getChannels());
			streamConfiguration.setSampleRate((int) trackData.getSampleRate());
			encoder.setStreamConfiguration(streamConfiguration);

			EncodingConfiguration encodingConfiguration = new EncodingConfiguration();
			encoder.setEncodingConfiguration(encodingConfiguration);

			outputStream = new FLACFileOutputStream(
					outputFile.getAbsolutePath());
			encoder.setOutputStream(outputStream);
			encoder.openFLACStream();
			unencodedSamples = 0;
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public void encode(byte[] buf, int len) {
		try {
			int length = AudioMath.convertBuffer(buf, buffer, len,
					trackData.getBps());
			int samples = length / trackData.getChannels();
			encoder.addSamples(buffer, samples);
			unencodedSamples += samples;
			unencodedSamples -= encoder.encodeSamples(unencodedSamples, false);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void close() {
		try {
			encoder.encodeSamples(unencodedSamples, true);
			outputStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
