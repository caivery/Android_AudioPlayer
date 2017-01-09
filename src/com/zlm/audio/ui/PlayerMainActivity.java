package com.zlm.audio.ui;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.tulskiy.musique.audio.AudioFileReader;
import com.tulskiy.musique.model.TrackData;
import com.tulskiy.musique.system.TrackIO;
import com.zlm.player.ui.R;

public class PlayerMainActivity extends Activity {

	private SeekBar seekBar;

	private TextView progressTextView;

	private TextView durationTextView;

	private ListView playList;

	private PlayListAdapter adapter;

	private Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			// TrackData  track = (TrackData ) msg.obj;
			// if (track != null) {
			//
			// seekBar.setMax((int) track.getTrackData Data().getDuration());
			// seekBar.setProgress((int) audioInfo.getPlayedProgress());
			//
			// progressTextView.setText(MediaUtil.formatTime((int) audioInfo
			// .getPlayedProgress()));
			// durationTextView.setText(audioInfo.getDurationStr());
			// }
		}

	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		init();
		new Thread() {

			@Override
			public void run() {
				loadData();
			}

		}.start();
	}

	private void init() {
		seekBar = (SeekBar) findViewById(R.id.seekBar);
		progressTextView = (TextView) findViewById(R.id.progressTextView);
		durationTextView = (TextView) findViewById(R.id.durationTextView);

		playList = (ListView) findViewById(R.id.playlist);
	}

	private void loadData() {

		new AsyncTask<String, Integer, List<TrackData >>() {

			@Override
			protected List<TrackData > doInBackground(String... arg0) {
				List<TrackData > datas = new ArrayList<TrackData >();
				File audioFile = new File(
						Environment.getExternalStorageDirectory()
								+ "/haplayer/audio/");
				File[] files = audioFile.listFiles();
				for (int i = 0; i < files.length; i++) {
					File f = files[i];

					AudioFileReader audioFileReader = TrackIO
							.getAudioFileReader(f.getName());
					if (audioFileReader != null)
						datas.add(audioFileReader.read(f));

				}
				return datas;

			}

			@Override
			protected void onPostExecute(List<TrackData > result) {
				adapter = new PlayListAdapter(getApplicationContext(), result,
						mHandler);

				playList.setAdapter(adapter);
			}
		}.execute("");
	}

	interface ClickEvent {
		public void seekTo(int progress);

	}
}
