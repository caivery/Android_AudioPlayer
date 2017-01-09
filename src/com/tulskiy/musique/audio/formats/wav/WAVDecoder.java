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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

import com.tulskiy.musique.audio.Decoder;
import com.tulskiy.musique.model.TrackData;

/**
 * @Author: Denis Tulskiy
 * @Date: 30.06.2009
 */
public class WAVDecoder implements Decoder {
	private InputStream audioInputStream;
	private TrackData inputFile;
	private AudioTrack audioTrack;

	public boolean open(TrackData trackData) {
		try {
			this.inputFile = trackData;
			audioInputStream = new FileInputStream(trackData.getFile());
			//
			int mFrequency = trackData.getSampleRate();
			int mChannel = AudioFormat.CHANNEL_CONFIGURATION_STEREO;
			int mSampBit = AudioFormat.ENCODING_PCM_16BIT;
			// 获得构建对象的最小缓冲区大小
			int minBufSize = AudioTrack.getMinBufferSize(mFrequency, mChannel,
					mSampBit);
			audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, mFrequency,
					mChannel, mSampBit, minBufSize * 2, AudioTrack.MODE_STREAM);

			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public AudioTrack getAudioTrack() {
		return audioTrack;
	}

	public void seekSample(long sample) {
		open(inputFile);
		try {
			long toSkip = sample * inputFile.getFrameSize();
			long skipped = 0;
			while (skipped < toSkip) {
				long b = audioInputStream.skip(toSkip - skipped);
				if (b == 0)
					break;
				skipped += b;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public int decode(byte[] buf) {
		try {
			return audioInputStream.read(buf, 0, 512);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return -1;
	}

	public void close() {
		try {
			if (audioInputStream != null)
				audioInputStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
