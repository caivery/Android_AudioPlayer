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

package com.tulskiy.musique.audio.formats.mp3;

import java.io.IOException;
import java.util.List;

import org.jaudiotagger.audio.mp3.LameFrame;
import org.jaudiotagger.audio.mp3.MP3AudioHeader;
import org.jaudiotagger.audio.mp3.MP3File;
import org.jaudiotagger.audio.mp3.XingFrame;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.KeyNotFoundException;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagField;
import org.jaudiotagger.tag.id3.ID3v1Tag;
import org.jaudiotagger.tag.id3.ID3v24Frame;
import org.jaudiotagger.tag.id3.ID3v24Tag;
import org.jaudiotagger.tag.id3.framebody.AbstractFrameBodyTextInfo;
import org.jaudiotagger.tag.id3.framebody.FrameBodyCOMM;
import org.jaudiotagger.tag.id3.framebody.FrameBodyPOPM;
import org.jaudiotagger.tag.id3.framebody.FrameBodyTPOS;
import org.jaudiotagger.tag.id3.framebody.FrameBodyTRCK;
import org.jaudiotagger.tag.id3.valuepair.TextEncoding;

import com.tulskiy.musique.audio.AudioFileReader;
import com.tulskiy.musique.audio.formats.ape.APETagProcessor;
import com.tulskiy.musique.model.FieldValues;
import com.tulskiy.musique.model.TrackData;
import com.tulskiy.musique.util.Util;

import davaguine.jmac.info.ID3Tag;

/**
 * @Author: Denis Tulskiy
 * @Date: 26.06.2009
 */
public class MP3FileReader extends AudioFileReader {
	private static final int GAPLESS_DELAY = 529;

	private APETagProcessor apeTagProcessor = new APETagProcessor();

	public TrackData readSingle(TrackData trackData) {
		TextEncoding.getInstanceOf()
				.setDefaultNonUnicode(defaultCharset.name());
		ID3Tag.setDefaultEncoding(defaultCharset.name());
		MP3File mp3File = null;
		try {
			mp3File = new MP3File(trackData.getFile(), MP3File.LOAD_ALL, true);
		} catch (Exception ignored) {
			System.out.println("Couldn't read file: " + trackData.getFile());
		}

		ID3v24Tag v24Tag = null;
		if (mp3File != null) {
			try {
				v24Tag = mp3File.getID3v2TagAsv24();
				if (v24Tag != null) {
					copyCommonTagFields(v24Tag, trackData);
					copySpecificTagFields(v24Tag, trackData);
				}

				ID3v1Tag id3v1Tag = mp3File.getID3v1Tag();
				if (id3v1Tag != null) {
					copyCommonTagFields(id3v1Tag, trackData);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

			MP3AudioHeader mp3AudioHeader = mp3File.getMP3AudioHeader();
			copyHeaderFields(mp3AudioHeader, trackData);

			long totalSamples = trackData.getTotalSamples();
			int enc_delay = GAPLESS_DELAY;

			XingFrame xingFrame = mp3AudioHeader.getXingFrame();
			if (xingFrame != null) {
				LameFrame lameFrame = xingFrame.getLameFrame();
				if (lameFrame != null) {
					long length = totalSamples;
					enc_delay += lameFrame.getEncDelay();
					int enc_padding = lameFrame.getEncPadding() - GAPLESS_DELAY;
					if (enc_padding < length)
						length -= enc_padding;

					if (totalSamples > length)
						totalSamples = length;
				} else {
					totalSamples += GAPLESS_DELAY;
				}
			}

			totalSamples -= enc_delay;
			trackData.setTotalSamples(totalSamples);
		}

		// TODO review correctness of reading APETag only in case ID3 is missed
		// for example, maybe useful to read and set those fields
		// that are missed in ID3 but presented in APE
		if (v24Tag == null) {
			try {
				apeTagProcessor.readAPEv2Tag(trackData);
			} catch (Exception ignored) {
			}
		}

		return trackData;
	}

	public boolean isFileSupported(String ext) {
		return ext.equalsIgnoreCase("mp3");
	}

	@Override
	protected void copyCommonTagFields(Tag tag, TrackData trackData)
			throws IOException {
		if (tag instanceof ID3v24Tag) {
			ID3v24Tag v24Tag = (ID3v24Tag) tag;
			for (FieldKey key : FieldKey.values()) {
				setMusiqueTagFieldValues(trackData, key, v24Tag);
			}
		} else if (tag instanceof ID3v1Tag) {
			ID3v1Tag id3v1Tag = (ID3v1Tag) tag;
			for (FieldKey key : FieldKey.values()) {
				String val = id3v1Tag.getFirst(key);
				if (!Util.isEmpty(val)) {
					FieldValues tagFieldValues = trackData
							.getTagFieldValues(key);
					if (tagFieldValues == null || tagFieldValues.isEmpty())
						trackData.setTagFieldValues(key, val);
				}
			}
		}
	}

	// @Override
	// protected void copySpecificTagFields(Tag tag, TrackData trackData) {
	// ID3v24Tag v24Tag = (ID3v24Tag) tag;
	// }

	// TODO review (T?? [but not TXXX] are only supported at the moment)
	private void setMusiqueTagFieldValues(TrackData trackData, FieldKey key,
			ID3v24Tag tag) {
		List<TagField> fields;

		try {
			fields = tag.getFields(key);
		} catch (KeyNotFoundException ignored) {
			return;
		}

		for (TagField field : fields) {
			ID3v24Frame frame = (ID3v24Frame) field;
			if (frame.getBody() instanceof FrameBodyTRCK) {
				FrameBodyTRCK body = (FrameBodyTRCK) frame.getBody();
				if (FieldKey.TRACK.equals(key)) {
					trackData.addTrack(body.getTrackNo());
				} else if (FieldKey.TRACK_TOTAL.equals(key)) {
					trackData.addTrackTotal(body.getTrackTotal());
				}
			} else if (frame.getBody() instanceof FrameBodyTPOS) {
				FrameBodyTPOS body = (FrameBodyTPOS) frame.getBody();
				if (FieldKey.DISC_NO.equals(key)) {
					trackData.addDisc(body.getDiscNo());
				} else if (FieldKey.DISC_TOTAL.equals(key)) {
					trackData.addDiscTotal(body.getDiscTotal());
				}
			} else if (frame.getBody() instanceof FrameBodyCOMM) {
				FrameBodyCOMM body = (FrameBodyCOMM) frame.getBody();
				trackData.addComment(body.getText());
			} else if (frame.getBody() instanceof FrameBodyPOPM) {
				FrameBodyPOPM body = (FrameBodyPOPM) frame.getBody();
				trackData.addRating(String.valueOf(body.getRating()));
			} else if (frame.getBody() instanceof AbstractFrameBodyTextInfo) {
				AbstractFrameBodyTextInfo body = (AbstractFrameBodyTextInfo) frame
						.getBody();
				for (int i = 0; i < body.getNumberOfValues(); i++) {
					trackData.addTagFieldValues(key, body.getValueAtIndex(i));
				}
			}
		}
	}

}
