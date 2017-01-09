/*
 * Copyright (c) 2008, 2009, 2010 Denis Tulskiy
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

package com.tulskiy.musique.audio.formats.ape;

import android.media.AudioFormat;

import com.tulskiy.musique.audio.AudioFileReader;
import com.tulskiy.musique.model.TrackData;

import davaguine.jmac.info.APEFileInfo;
import davaguine.jmac.info.APEHeader;
import davaguine.jmac.info.ID3Tag;
import davaguine.jmac.tools.RandomAccessFile;

/**
 * @Author: Denis Tulskiy
 * @Date: 26.06.2009
 */
public class APEFileReader extends AudioFileReader {
	private APETagProcessor tagProcessor = new APETagProcessor();

	public TrackData readSingle(TrackData trackData) {
		try {
			ID3Tag.setDefaultEncoding(defaultCharset.name());
			RandomAccessFile ras = new RandomAccessFile(trackData.getFile(),
					"r");
			APEHeader header = new APEHeader(ras);
			APEFileInfo fileInfo = new APEFileInfo();
			header.Analyze(fileInfo);
			parseInfo(trackData, fileInfo);

			tagProcessor.readAPEv2Tag(trackData);
			ras.close();
			return trackData;
		} catch (Exception e) {
			System.out.println("Couldn't read file: " + trackData.getFile());
		}
		return null;
	}

	public boolean isFileSupported(String ext) {
		return ext.equalsIgnoreCase("ape");
	}

	private void parseInfo(TrackData trackData, APEFileInfo fileInfo) {

		trackData.setChannels(fileInfo.nChannels);

		int frameSize = trackData.getChannels()
				* AudioFormat.ENCODING_PCM_16BIT;
		trackData.setFrameSize(frameSize);

		trackData.setSampleRate(fileInfo.nSampleRate);
		trackData.setTotalSamples(fileInfo.nTotalBlocks);
		trackData.setStartPosition(0);
		trackData.setCodec("Monkey's Audio");
		trackData.setBitrate(fileInfo.nAverageBitrate);
	}

}
