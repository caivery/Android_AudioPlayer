package com.zlm.audio.player;

import android.media.AudioTrack;

import com.tulskiy.musique.audio.Decoder;
import com.tulskiy.musique.model.TrackData;
import com.tulskiy.musique.system.Codecs;
import com.tulskiy.musique.util.AudioMath;
import com.tulskiy.musique.util.Buffer;

/**
 * 播放器
 * 
 * @author zhangliangming
 * 
 */
public class BasePlayer {
	private TrackData trackData;
	private Decoder decoder;
	private AudioTrack mAudioTrack;
	private Thread playThread;
	private long currentByte;

	private boolean isPlaying = false;
	private boolean isStop = false;
	private long totalFrameBytes;

	private PlayEvent playEvent;

	public BasePlayer() {

	}

	public void open(TrackData trackData) {
		if (trackData == null)
			return;
		this.trackData = trackData;
		decoder = Codecs.getDecoder(trackData);
		if (decoder == null)
			return;
		boolean flag = decoder.open(trackData);
		if (!flag)
			return;

		mAudioTrack = decoder.getAudioTrack();

		long samples = trackData.getTotalSamples();
		totalFrameBytes = AudioMath.samplesToBytes(samples,
				trackData.getFrameSize());

		if (trackData.getStartPosition() >= 0) {
			long seekSample = AudioMath.millisToSamples(
					trackData.getStartPosition(), trackData.getSampleRate());

			decoder.seekSample(seekSample);

			currentByte = AudioMath.samplesToBytes(seekSample,
					trackData.getFrameSize());
		} else {
			currentByte = 0;
		}
	}

	/**
	 * 设置音量
	 * 
	 * @param volume
	 *            0-1
	 */
	public void setStereoVolume(float volume) {
		mAudioTrack.setStereoVolume(volume, volume);// 设置当前音量大小
	}

	public void play() {
		if (trackData == null) {
			// 不支持该格式音频
			if (playEvent != null)
				playEvent.error();
			return;
		}
		if (decoder == null) {
			// 不支持该格式解码
			if (playEvent != null)
				playEvent.error();
			return;
		}
		if (mAudioTrack == null) {
			// 创建失败
			if (playEvent != null)
				playEvent.error();
			return;
		}
		mAudioTrack.play();
		playThread = new Thread(runnable);
		playThread.start();

	}

	/**
	 * 快进
	 * 
	 * @param sample
	 */
	public void seek(long sample) {
		currentByte = sample;
		if (playThread != null) {
			playThread = null;
		}
		open(trackData);
		play();
	}

	/**
	 * 快进
	 * 
	 * @param millis
	 *            时间进度
	 */
	public void seekTo(long millis) {
		long seekBytes = AudioMath.millisToSamples(millis,
				trackData.getSampleRate());
		seek(seekBytes);
	}

	public void stop() {
		isStop = true;
		isPlaying = false;
		if (playThread != null) {
			playThread = null;
		}
	}

	public boolean isPlaying() {
		return isPlaying;
	}

	private Runnable runnable = new Runnable() {

		@Override
		public void run() {
			byte[] buf = new byte[65536];
			int len;
			int BUFFER_SIZE = (int) Math.pow(2, 18);
			Buffer buffer = new Buffer(BUFFER_SIZE);
			int BUFFER_SIZE2 = (int) (Math.pow(2, 15) / 24) * 24;
			byte[] buf2 = new byte[BUFFER_SIZE2];
			buffer.open();
			isPlaying = true;
			while (currentByte < totalFrameBytes) {
				if (isStop) {
					mAudioTrack.stop();
					if (playEvent != null)
						playEvent.stoped();
					return;
				}
				try {

					len = decoder.decode(buf);
					buffer.write(buf, 0, len);

					int len2 = buffer.read(buf2, 0, BUFFER_SIZE2);
					while (len2 == -1) {
						buffer.pollNextTrack();
						len2 = buffer.read(buf2, 0, BUFFER_SIZE2);
					}
					currentByte += len2;
					mAudioTrack.write(buf2, 0, len2);
				} catch (Exception e) {
					mAudioTrack.stop();
					if (e instanceof ArrayIndexOutOfBoundsException) {
						if (playEvent != null) {
							playEvent.finished();
							return;
						}
					}
					e.printStackTrace();
					if (playEvent != null) {
						playEvent.error();
						return;
					}
				}
			}
			mAudioTrack.stop();
			if (playEvent != null)
				playEvent.finished();
		}
	};

	public double getCurrentMillis() {
		if (trackData != null) {
			return AudioMath.bytesToMillis(currentByte,
					trackData.getFrameSize(), trackData.getSampleRate());
		}
		return 0.0D;
	}

	public PlayEvent getPlayEvent() {
		return playEvent;
	}

	public void setPlayEvent(PlayEvent playEvent) {
		this.playEvent = playEvent;
	}

	public interface PlayEvent {
		public void finished();

		public void stoped();

		public void error();
	}
}
