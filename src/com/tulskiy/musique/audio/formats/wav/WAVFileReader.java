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

package com.tulskiy.musique.audio.formats.wav;

import java.io.File;

import org.jaudiotagger.tag.FieldKey;

import android.media.AudioFormat;

import com.tulskiy.musique.audio.AudioFileReader;
import com.tulskiy.musique.model.TrackData;
import com.tulskiy.musique.util.Util;

/**
 * @Author: Denis Tulskiy
 * @Date: 30.06.2009
 */
public class WAVFileReader extends AudioFileReader {

	public TrackData readSingle(TrackData trackData) {
		File file = trackData.getFile();

		String title = Util.removeExt(file.getName());
		trackData.setTagFieldValues(FieldKey.TITLE, title);
		try {

			BaseWAVFileReader fileReader = new BaseWAVFileReader();
			fileReader.openFile(trackData.getFile().getPath());
			WavFileHeader audioHeader = fileReader.getmWavFileHeader();

			trackData.setStartPosition(0);
			trackData.setSampleRate(audioHeader.mSampleRate);
			trackData.setChannels(audioHeader.mNumChannel);
			//
			long totalSamples = Math.round(audioHeader.mSubChunk2Size * 8
					* 1.00
					/ (audioHeader.mNumChannel * audioHeader.mBitsPerSample));
			trackData.setTotalSamples(totalSamples);

			int frameSize = trackData.getChannels()
					* AudioFormat.ENCODING_PCM_16BIT;
			trackData.setFrameSize(frameSize);

			trackData.setCodec(Util.getFileExt(file).toUpperCase());
			trackData.setBitrate(audioHeader.mBiteRate);
		} catch (Exception e) {
			System.out.println("Couldn't read file: " + trackData.getFile());
		}
		return trackData;
	}

	public boolean isFileSupported(String ext) {
		return ext.equalsIgnoreCase("wav") || ext.equalsIgnoreCase("au")
				|| ext.equalsIgnoreCase("aiff");
	}

}
