package org.kansus.mediacenter.activity.radio;

import org.kansus.mediacenter.R;
import org.kansus.mediacenter.load.ImageLoader;
import org.kansus.mediacenter.social.FacebookSharing;
import org.kansus.mediacenter.social.TwitterSharing;
import org.kansus.mediacenter.widget.AudioPlayer;
import org.kansus.mediacenter.widget.StreamController;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.Display;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import io.vov.vitamio.MediaPlayer;

@SuppressWarnings("unused")
public class RadioPlayer extends Activity implements OnClickListener {

	private String mName;
	private String mLocation;
	private String mSite;
	private String mStream;
	private String mThumbnail;

	private AudioPlayer mAudioPlayer;
	private StreamController mStreamController;
	private static View mVolumeBrightnessLayout;
	private ImageView mOperationBg;
	private ImageView mOperationPercent;
	private ImageView mFacebookButton;
	private ImageView mTwitterButton;
	private TextView mStationName;
	private ImageView mStationLogo;
	
	private TwitterSharing mTwitter;
	private FacebookSharing mFacebook;

	private AudioManager mAudioManager;
	private int mMaxVolume;
	private int mVolume = -1;

	private GestureDetector mGestureDetector;
	private ImageLoader mImageLoader;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		Intent intent = getIntent();
		getDataFromExtras();
		if (TextUtils.isEmpty(mStream))
			mStream = "mms://gruporbs-atlantida-fm-sc.wm.llnwd.net/gruporbs_Atlantida_FM_SC?channel=256";
		else if (intent.getData() != null)
			mStream = intent.getData().toString();

		setContentView(R.layout.radio_player);
		mFacebook = new FacebookSharing(this);
		mTwitter = new TwitterSharing(this);
		mAudioPlayer = new AudioPlayer(this);

		mImageLoader = new ImageLoader(getApplicationContext());
		mStationLogo = (ImageView) findViewById(R.id.radio_logo_iv);
		mImageLoader.DisplayImage(mThumbnail, mStationLogo);

		mVolumeBrightnessLayout = findViewById(R.id.operation_volume_brightness);
		mOperationBg = (ImageView) findViewById(R.id.operation_bg);
		mOperationPercent = (ImageView) findViewById(R.id.operation_percent);

		mFacebookButton = (ImageView) findViewById(R.id.facebook_iv);
		mFacebookButton.setOnClickListener(this);
		mTwitterButton = (ImageView) findViewById(R.id.twitter_iv);
		mTwitterButton.setOnClickListener(this);

		mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		mMaxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		mAudioPlayer.setAudioURI(Uri.parse(mStream));

		mStationName = (TextView) findViewById(R.id.radio_name_tv);
		mStationName.setText(mName);
		mStationName.setOnClickListener(this);

		final ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);
		mStreamController = new StreamController(this, 0);
		mAudioPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {

			@Override
			public void onPrepared(MediaPlayer paramMediaPlayer) {
				mStreamController.setMediaPlayer(mAudioPlayer);
				mStreamController.setAnchorView(findViewById(R.id.main_layout));
				progressBar.setVisibility(View.GONE);

				Handler handler = new Handler();
				handler.post(new Runnable() {
					public void run() {
						mStreamController.setEnabled(true);
						mStreamController.show(0);
					}
				});
			}
		});

		mGestureDetector = new GestureDetector(this, new MyGestureListener());
	}
	
	@Override
	protected void onResume() {
		overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_right);
		super.onResume();
	}

	public void getDataFromExtras() {
		Intent intent = getIntent();
		mName = intent.getExtras().getString("name");
		mLocation = intent.getExtras().getString("location");
		mSite = intent.getExtras().getString("site");
		mStream = intent.getExtras().getString("stream");
		mThumbnail = intent.getExtras().getString("thumbnail");
	}

	/*
	 * @Override protected void onPause() { super.onPause(); if (mAudioPlayer !=
	 * null) mAudioPlayer.pause(); }
	 */

	/*
	 * @Override protected void onResume() { super.onResume(); if (mAudioPlayer
	 * != null) mAudioPlayer.resume(); }
	 */

	/*
	 * @Override protected void onStop() { super.onStop();
	 * mMediaController.hide(); mAudioPlayer.stopPlayback(); }
	 */

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mAudioPlayer != null) {
			mStreamController.hide();
			mAudioPlayer.stop();
			mAudioPlayer.stopPlayback();
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (mGestureDetector.onTouchEvent(event))
			return true;

		switch (event.getAction() & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_UP:
			endGesture();
			break;
		}

		return super.onTouchEvent(event);
	}

	private void endGesture() {
		mVolume = -1;

		mDismissHandler.removeMessages(0);
		mDismissHandler.sendEmptyMessageDelayed(0, 500);
	}

	private class MyGestureListener extends SimpleOnGestureListener {

		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
			float mOldX = e1.getX(), mOldY = e1.getY();
			int y = (int) e2.getRawY();
			Display disp = getWindowManager().getDefaultDisplay();
			Point size = new Point();
			disp.getSize(size);
			int windowWidth = size.x;
			int windowHeight = size.y;

			if (mOldX > windowWidth * 4.0 / 5)
				onVolumeSlide((mOldY - y) / windowHeight);

			return super.onScroll(e1, e2, distanceX, distanceY);
		}
	}

	private static Handler mDismissHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			mVolumeBrightnessLayout.setVisibility(View.GONE);
		}
	};

	private void onVolumeSlide(float percent) {
		if (mVolume == -1) {
			mVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
			if (mVolume < 0)
				mVolume = 0;

			mOperationBg.setImageResource(R.drawable.video_volumn_bg);
			mVolumeBrightnessLayout.setVisibility(View.VISIBLE);
		}

		int index = (int) (percent * mMaxVolume) + mVolume;
		if (index > mMaxVolume)
			index = mMaxVolume;
		else if (index < 0)
			index = 0;

		mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, index, 0);

		ViewGroup.LayoutParams lp = mOperationPercent.getLayoutParams();
		lp.width = findViewById(R.id.operation_full).getWidth() * index / mMaxVolume;
		mOperationPercent.setLayoutParams(lp);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.radio_name_tv:
			if (mStationName.getVisibility() == View.VISIBLE && !TextUtils.isEmpty(mSite)) {
				Intent i = new Intent(Intent.ACTION_VIEW);
				i.setData(Uri.parse(mSite));
				startActivity(i);
				overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_left);
			}
			break;
		case R.id.facebook_iv:
			Thread t = new Thread(new Runnable() {

				@Override
				public void run() {
					if (!mFacebook.getAccessToken()) {
						mFacebook.getAuthorizationIfNeeded();
					}
					mFacebook.shareRadio(mName);
				}
			});
			t.start();
			break;
		case R.id.twitter_iv:
			Thread t2 = new Thread(new Runnable() {

				@Override
				public void run() {
					if (!mTwitter.getAccessToken()) {
						mTwitter.getAuthorizationIfNeeded();
					}
					mTwitter.shareRadio(mName);
				}
			});
			t2.start();
			break;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.radio_tv_player, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {

		return super.onMenuItemSelected(featureId, item);
	}
}
