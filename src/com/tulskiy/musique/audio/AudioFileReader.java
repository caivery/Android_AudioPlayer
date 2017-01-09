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

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

import org.jaudiotagger.audio.generic.GenericAudioHeader;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.KeyNotFoundException;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagField;
import org.jaudiotagger.tag.mp4.field.Mp4DiscNoField;
import org.jaudiotagger.tag.mp4.field.Mp4TrackField;

import android.media.AudioFormat;

import com.tulskiy.musique.model.TrackData;

/**
 * Author: Denis Tulskiy Date: 25.06.2009
 */
public abstract class AudioFileReader {
	protected static Charset defaultCharset = Charset.forName("iso8859-1");

	protected abstract TrackData readSingle(TrackData trackData);

	public TrackData reload(TrackData trackData) {
		TrackData res = readSingle(trackData);
		if (res.isFile())
			res.setLastModified(res.getFile().lastModified());
		return res;
	}

	public TrackData read(File file) {
		TrackData trackData = new TrackData();
		trackData.setLocation(file.toURI().toString());
		return reload(trackData);
	}

	public abstract boolean isFileSupported(String ext);

	// in case of logic change, review MP3TagReader and APETagProcessor
	protected void copyCommonTagFields(Tag tag, TrackData trackData)
			throws IOException {
		if (tag != null && trackData != null) {
			for (FieldKey key : FieldKey.values()) {
				List<TagField> fields;
				try {
					fields = tag.getFields(key);
				} catch (KeyNotFoundException knfe) {
					// TODO review
					continue;
				} catch (NullPointerException npe) {
					// TODO review workaround for mp4tag (throws nullpointer if
					// no mapping found for generic key)
					continue;
				}
				for (TagField field : fields) {
					// TODO think how to minimize custom check impact
					if (field instanceof Mp4TrackField
							|| field instanceof Mp4DiscNoField) {
						break;
					}
					trackData.addTagFieldValues(key, field.toString());
				}
			}

			// think about the way
			trackData.setCueSheet(tag.getFirst("CUESHEET"));
		}
	}

	protected void copySpecificTagFields(Tag tag, TrackData trackData) {
		// Empty implementation, to be overridden
	}

	protected void copyHeaderFields(GenericAudioHeader header,
			TrackData trackData) {
		if (header != null && trackData != null) {
			trackData.setChannels(header.getChannelNumber());
			trackData.setTotalSamples(header.getTotalSamples());

			int frameSize = trackData.getChannels()
					* AudioFormat.ENCODING_PCM_16BIT;
			trackData.setFrameSize(frameSize);

			trackData.setSampleRate(header.getSampleRateAsNumber());
			trackData.setStartPosition(0);
			trackData.setCodec(header.getFormat());
			trackData.setBitrate((int) header.getBitRateAsNumber());
		}
	}

	public static void setDefaultCharset(Charset charset) {
		defaultCharset = charset;
	}

	public static Charset getDefaultCharset() {
		return defaultCharset;
	}
}
