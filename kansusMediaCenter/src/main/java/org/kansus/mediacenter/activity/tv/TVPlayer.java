package org.kansus.mediacenter.activity.tv;

import io.vov.vitamio.MediaPlayer;
import io.vov.vitamio.MediaPlayer.OnCompletionListener;
import io.vov.vitamio.MediaPlayer.OnPreparedListener;
import io.vov.vitamio.widget.MediaController;
import io.vov.vitamio.widget.VideoView;

import org.kansus.mediacenter.R;
import org.kansus.mediacenter.load.ImageLoader;
import org.kansus.mediacenter.social.FacebookSharing;
import org.kansus.mediacenter.social.TwitterSharing;
import org.kansus.mediacenter.widget.StreamController;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
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
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

@SuppressWarnings("unused")
public class TVPlayer extends Activity implements OnPreparedListener, OnCompletionListener, MediaController.OnHiddenListener,
		MediaController.OnShownListener, OnClickListener, MediaPlayer.OnInfoListener, MediaPlayer.OnBufferingUpdateListener {

	private String mName;
	private String mLocation;
	private String mSite;
	private String mStream;
	private String mThumbnail;

	private VideoView mVideoView;
	private StreamController mStreamController;
	private static View mVolumeBrightnessLayout;
	private ImageView mOperationBg;
	private ImageView mOperationPercent;
	private ImageView mFacebookButton;
	private ImageView mTwitterButton;
	private TextView mStationName;
	private ImageView mStationLogo;
	private ProgressBar mProgressBar;

	private AudioManager mAudioManager;
	private int mMaxVolume;
	private int mVolume = -1;
	private float mBrightness = -1f;
	private int mLayout = VideoView.VIDEO_LAYOUT_ZOOM;
	
	private TwitterSharing mTwitter;
	private FacebookSharing mFacebook;

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

		setContentView(R.layout.tv_player);
		mFacebook = new FacebookSharing(this);
		mTwitter = new TwitterSharing(this);
		mVideoView = (VideoView) findViewById(R.id.surface_view);
		mVideoView.setOnCompletionListener(this);

		mImageLoader = new ImageLoader(getApplicationContext());
		mStationLogo = (ImageView) findViewById(R.id.tv_logo_iv);
		mImageLoader.DisplayImage(mThumbnail, mStationLogo);

		mVolumeBrightnessLayout = findViewById(R.id.operation_volume_brightness);
		mOperationBg = (ImageView) findViewById(R.id.operation_bg);
		mOperationPercent = (ImageView) findViewById(R.id.operation_percent);

		mFacebookButton = (ImageView) findViewById(R.id.facebook_iv);
		mFacebookButton.setOnClickListener(this);
		mTwitterButton = (ImageView) findViewById(R.id.twitter_iv);
		mTwitterButton.setOnClickListener(this);

		mProgressBar = (ProgressBar) findViewById(R.id.progressBar);

		mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		mMaxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		mVideoView.setVideoURI(Uri.parse(mStream));

		mStationName = (TextView) findViewById(R.id.tv_name_tv);
		mStationName.setText(mName);
		mStationName.setOnClickListener(this);

		mStreamController = new StreamController(this, 3000);
		mStreamController.setOnHiddenListener(this);
		mStreamController.setOnShownListener(this);
		mVideoView.setVideoQuality(MediaPlayer.VIDEOQUALITY_HIGH);
		mVideoView.setMediaController(mStreamController);
		mVideoView.requestFocus();
		mVideoView.setOnPreparedListener(this);
		mVideoView.setOnInfoListener(this);
		mVideoView.setOnBufferingUpdateListener(this);

		mGestureDetector = new GestureDetector(this, new MyGestureListener());
	}

	public void getDataFromExtras() {
		Intent intent = getIntent();
		mName = intent.getExtras().getString("name");
		mLocation = intent.getExtras().getString("location");
		mSite = intent.getExtras().getString("site");
		mStream = intent.getExtras().getString("stream");
		mThumbnail = intent.getExtras().getString("thumbnail");
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (mVideoView != null)
			mVideoView.pause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_right);
		if (mVideoView != null)
			mVideoView.resume();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mVideoView != null)
			mStreamController.hide();
			mVideoView.stopPlayback();
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
		mBrightness = -1f;

		mDismissHandler.removeMessages(0);
		mDismissHandler.sendEmptyMessageDelayed(0, 500);
	}

	private class MyGestureListener extends SimpleOnGestureListener {

		@Override
		public boolean onDoubleTap(MotionEvent e) {
			if (mLayout == VideoView.VIDEO_LAYOUT_ZOOM)
				mLayout = VideoView.VIDEO_LAYOUT_ORIGIN;
			else
				mLayout++;
			if (mVideoView != null)
				mVideoView.setVideoLayout(mLayout, 0);
			return true;
		}

		@SuppressWarnings("deprecation")
		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
			float mOldX = e1.getX(), mOldY = e1.getY();
			int y = (int) e2.getRawY();
			Display disp = getWindowManager().getDefaultDisplay();
			int windowWidth = disp.getWidth();
			int windowHeight = disp.getHeight();

			if (mOldX > windowWidth * 4.0 / 5)
				onVolumeSlide((mOldY - y) / windowHeight);
			else if (mOldX < windowWidth / 5.0)
				onBrightnessSlide((mOldY - y) / windowHeight);

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
		lp.width = findViewById(R.id.operation_full).getLayoutParams().width * index / mMaxVolume;
		mOperationPercent.setLayoutParams(lp);
	}

	private void onBrightnessSlide(float percent) {
		if (mBrightness < 0) {
			mBrightness = getWindow().getAttributes().screenBrightness;
			if (mBrightness <= 0.00f)
				mBrightness = 0.50f;
			if (mBrightness < 0.01f)
				mBrightness = 0.01f;

			mOperationBg.setImageResource(R.drawable.video_brightness_bg);
			mVolumeBrightnessLayout.setVisibility(View.VISIBLE);
		}
		WindowManager.LayoutParams lpa = getWindow().getAttributes();
		lpa.screenBrightness = mBrightness + percent;
		if (lpa.screenBrightness > 1.0f)
			lpa.screenBrightness = 1.0f;
		else if (lpa.screenBrightness < 0.01f)
			lpa.screenBrightness = 0.01f;
		getWindow().setAttributes(lpa);

		ViewGroup.LayoutParams lp = mOperationPercent.getLayoutParams();
		lp.width = (int) (findViewById(R.id.operation_full).getLayoutParams().width * lpa.screenBrightness);
		mOperationPercent.setLayoutParams(lp);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		if (mVideoView != null)
			mVideoView.setVideoLayout(mLayout, 0);
		super.onConfigurationChanged(newConfig);
	}

	@Override
	public void onCompletion(MediaPlayer player) {
		mStationLogo.setVisibility(View.VISIBLE);
	}

	@Override
	public void onShown() {
		mFacebookButton.setVisibility(View.VISIBLE);
		mTwitterButton.setVisibility(View.VISIBLE);
		mStationName.setVisibility(View.VISIBLE);
	}

	@Override
	public void onHidden() {
		mFacebookButton.setVisibility(View.GONE);
		mTwitterButton.setVisibility(View.GONE);
		mStationName.setVisibility(View.GONE);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.tv_name_tv:
			if (mStationName.getVisibility() == View.VISIBLE) {
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
					mFacebook.shareTV(mName);
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
					mTwitter.shareTV(mName);
				}
			});
			t2.start();
			break;
		}
	}

	@Override
	public void onPrepared(MediaPlayer mediaPlayer) {
		mProgressBar.setVisibility(View.GONE);
		mStationLogo.setVisibility(View.GONE);
		mediaPlayer.setPlaybackSpeed(1.0f);
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

	@Override
	public boolean onInfo(MediaPlayer mp, int what, int extra) {
		switch (what) {
			case MediaPlayer.MEDIA_INFO_BUFFERING_START:
				Log.d("XXX", "MEDIA_INFO_BUFFERING_START");
				if (mVideoView.isPlaying()) {
					mVideoView.pause();
				}
				break;
			case MediaPlayer.MEDIA_INFO_BUFFERING_END:
				Log.d("XXX", "MEDIA_INFO_BUFFERING_END");
				break;
			case MediaPlayer.MEDIA_INFO_DOWNLOAD_RATE_CHANGED:
				Log.d("XXX", "MEDIA_INFO_DOWNLOAD_RATE_CHANGED: " + extra + "kb/s" + "  ");
				break;
		}
		return true;
	}

	@Override
	public void onBufferingUpdate(MediaPlayer mp, int percent) {
		Log.d("XXX", "onBufferingUpdate: " + percent + "%");
	}
}
