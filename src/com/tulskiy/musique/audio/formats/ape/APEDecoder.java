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

import java.io.IOException;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

import com.tulskiy.musique.audio.Decoder;
import com.tulskiy.musique.model.TrackData;

import davaguine.jmac.decoder.IAPEDecompress;
import davaguine.jmac.tools.File;
import davaguine.jmac.tools.JMACException;

/**
 * @Author: Denis Tulskiy
 * @Date: 13.06.2009
 */
public class APEDecoder implements Decoder {
	static {
		System.setProperty("jmac.NATIVE", "true");
	}

	private IAPEDecompress decoder;
	private static final int BLOCKS_PER_DECODE = 4096 * 2;
	private int blockAlign;
	private TrackData trackData;

	public boolean open(TrackData trackData) {
		this.trackData = trackData;
		try {
			File apeInputFile = File.createFile(trackData.getFile()
					.getAbsolutePath(), "r");
			decoder = IAPEDecompress.CreateIAPEDecompress(apeInputFile);

			trackData.setSampleRate(decoder.getApeInfoSampleRate());

			blockAlign = decoder.getApeInfoBlockAlign();

			return true;
		} catch (Exception e) {
			e.printStackTrace();
			close();
		}
		return false;
	}

	@Override
	public AudioTrack getAudioTrack() {
		int mFrequency = decoder.getApeInfoSampleRate();
		int mChannel = AudioFormat.CHANNEL_CONFIGURATION_STEREO;
		int mSampBit = AudioFormat.ENCODING_PCM_16BIT;
		// 获得构建对象的最小缓冲区大小
		int minBufSize = AudioTrack.getMinBufferSize(mFrequency, mChannel,
				mSampBit);
		return new AudioTrack(AudioManager.STREAM_MUSIC, mFrequency, mChannel,
				mSampBit, minBufSize * 2, AudioTrack.MODE_STREAM);
	}

	public void seekSample(long sample) {
		try {
			if (decoder.getApeInfoDecompressCurrentBlock() != sample) {
				decoder.Seek((int) sample);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public int decode(byte[] buf) {
		try {
			int blocksDecoded = decoder.GetData(buf, BLOCKS_PER_DECODE);
			trackData.setBitrate(decoder.getApeInfoDecompressCurrentBitRate());

			if (blocksDecoded <= 0)
				return -1;
			return blocksDecoded * blockAlign;
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JMACException e) {
			e.printStackTrace();
		}
		return -1;
	}

	public void close() {
		try {
			if (decoder != null) {
				trackData.setBitrate(decoder.getApeInfoAverageBitrate());
				decoder.getApeInfoIoSource().close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
