package com.zlm.audio.ui;

import java.util.List;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.tulskiy.musique.model.TrackData;
import com.zlm.audio.player.BasePlayer;
import com.zlm.audio.player.BasePlayer.PlayEvent;
import com.zlm.player.ui.R;

public class PlayListAdapter extends BaseAdapter {

	private Context context;
	private List<TrackData> datas;
	private Handler mHandler;
	private LayoutInflater mInflater;

	private BasePlayer basePlayer;

	private Thread playThread;

	private TrackData trackData;

	public PlayListAdapter(Context context, List<TrackData> datas,
			Handler mHandler) {
		this.context = context;
		this.datas = datas;
		this.mHandler = mHandler;
		mInflater = LayoutInflater.from(context);
	}

	@Override
	public int getCount() {
		return datas.size();
	}

	@Override
	public Object getItem(int arg0) {
		return datas.get(arg0);
	}

	@Override
	public long getItemId(int arg0) {
		return arg0;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		ViewHolder viewHolder = null;
		if (null == convertView) {

			convertView = mInflater.inflate(R.layout.listview_item, null);
			viewHolder = new ViewHolder(convertView);
			convertView.setTag(viewHolder);
		} else {
			viewHolder = (ViewHolder) convertView.getTag();
		}
		final TrackData trackData = datas.get(position);
		viewHolder.getFileTextView().setTextColor(Color.BLACK);
		viewHolder.getFileTextView().setText(trackData.getFileName());
		viewHolder.getItemBG().setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				playMusic(trackData);
			}
		});
		return convertView;
	}

	protected void playMusic(TrackData trackData) {
		this.trackData = trackData;
		if (playThread != null)
			playThread = null;
		if (basePlayer != null) {
			basePlayer.stop();
			basePlayer = null;
		} else {
			playMusicInfo(trackData);
		}
	}

	private void playMusicInfo(TrackData trackData) {
		basePlayer = new BasePlayer();
		basePlayer.open(trackData);
		basePlayer.setPlayEvent(new PlayEvent() {

			@Override
			public void stoped() {

			}

			@Override
			public void finished() {

			}

			@Override
			public void error() {

			}
		});
		basePlayer.play();
		if (playThread == null)
			playThread = new Thread(runnable);
		playThread.start();
	}

	private Runnable runnable = new Runnable() {

		@Override
		public void run() {
			while (true) {
				if (basePlayer != null && basePlayer.isPlaying()) {
					Message msg = new Message();
					trackData.setStartPosition((int) basePlayer
							.getCurrentMillis());
					msg.obj = trackData;
					mHandler.sendMessage(msg);
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}
	};

	class ViewHolder {
		View view;
		TextView fileTextView;
		RelativeLayout itemBG;

		ViewHolder(View view) {
			this.view = view;
		}

		TextView getFileTextView() {
			if (fileTextView == null) {
				fileTextView = (TextView) view.findViewById(R.id.audioTextView);
			}
			return fileTextView;
		}

		RelativeLayout getItemBG() {
			if (itemBG == null) {
				itemBG = (RelativeLayout) view.findViewById(R.id.itemBG);
			}
			return itemBG;
		}
	}
}
