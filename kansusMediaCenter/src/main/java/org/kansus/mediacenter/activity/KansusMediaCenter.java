package org.kansus.mediacenter.activity;

import org.kansus.mediacenter.R;
import org.kansus.mediacenter.activity.image.GalleryChooser;
import org.kansus.mediacenter.activity.music.KansusMusic;
import org.kansus.mediacenter.activity.radio.MyRadios;
import org.kansus.mediacenter.activity.radio.RadioLibrary;
import org.kansus.mediacenter.activity.radio.RadioRecordings;
import org.kansus.mediacenter.activity.tv.MyTVs;
import org.kansus.mediacenter.activity.tv.TVLibrary;
import org.kansus.mediacenter.activity.tv.TVRecordings;
import org.kansus.mediacenter.activity.video.VideoDownloader;
import org.kansus.mediacenter.social.FacebookSharing;
import org.kansus.mediacenter.social.TwitterSharing;
import org.kansus.mediacenter.util.ImageManager;
import org.kansus.mediacenter.util.Util;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Activity inicial da aplicação.
 * 
 * @author Charles
 */
public class KansusMediaCenter extends Activity implements OnClickListener {

	// Pixel values
	int dp_64;
	int dp_92;
	LayoutParams buttonParams;

	// Atributos para identificar o menu atualmente selecionado
	static final int MUSIC_MENU = 0;
	static final int VIDEO_MENU = 1;
	static final int PICTURE_MENU = 2;
	static final int RADIO_MENU = 3;
	static final int TV_MENU = 4;
	static final int OPTIONS_MENU = 5;
	int selected_menu = 0;

	// Efeitos sonoros
	MediaPlayer startup;
	MediaPlayer select;
	MediaPlayer choose;
	MediaPlayer cancel;

	// Botões principais
	Button mMusicBtn;
	Button mVideoBtn;
	Button mPictureBtn;
	Button mRadioBtn;
	Button mTVBtn;
	Button mOptionsBtn;
	Button mTwitterBtn;
	Button mFacebookBtn;

	// Botões secundários
	Button[] mMusicButtons;
	Button[] mVideoButtons;
	Button[] mPictureButtons;
	Button[] mRadioButtons;
	Button[] mTVButtons;
	Button[] mOptionsButtons;

	TextView mMenuTitle;
	LinearLayout mMenuLayout;

	// Animações
	Animation slideInAnimation;
	Animation growFromMiddleAnimation;

	// Social Sharing
	FacebookSharing facebook;
	TwitterSharing twitter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		//if (!LibsChecker.checkVitamioLibs(this))
		//	return;

		dp_64 = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 88, getResources().getDisplayMetrics());
		dp_92 = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 92, getResources().getDisplayMetrics());
		buttonParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

		getFields();
		createButtons();
		setButtonsOnClickListener();
		loadSounds();
		loadAnimations();

		twitter = new TwitterSharing(this);
		facebook = new FacebookSharing(this);

		// Seleção inicial
		selected_menu = MUSIC_MENU;
		showMusicButtons();

		overridePendingTransition(0, R.anim.slide_in_vertical);

		startup.start();
	}

	@Override
	protected void onStop() {
		super.onStop();
		unloadContent();
	}

	@Override
	protected void onRestart() {
		super.onRestart();
		loadAnimations();
		loadSounds();
	}

	@Override
	public void onResume() {
		super.onResume();
		overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_right);
		facebook.getFacebookInstance().extendAccessTokenIfNeeded(this, null);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		facebook.getFacebookInstance().authorizeCallback(requestCode, resultCode, data);
	}

	public void getFields() {
		mMusicBtn = (Button) findViewById(R.id.m_music_btn);
		mVideoBtn = (Button) findViewById(R.id.m_video_btn);
		mPictureBtn = (Button) findViewById(R.id.m_pictures_btn);
		mRadioBtn = (Button) findViewById(R.id.m_radio_btn);
		mTVBtn = (Button) findViewById(R.id.m_tv_btn);
		mOptionsBtn = (Button) findViewById(R.id.m_options_btn);
		mTwitterBtn = (Button) findViewById(R.id.m_twitter_btn);
		mFacebookBtn = (Button) findViewById(R.id.m_facebook_btn);
		mMenuTitle = (TextView) findViewById(R.id.m_menu_title);
		mMenuLayout = (LinearLayout) findViewById(R.id.m_menu_layout);
	}

	public void setButtonsOnClickListener() {
		mMusicBtn.setOnClickListener(this);
		mVideoBtn.setOnClickListener(this);
		mPictureBtn.setOnClickListener(this);
		mRadioBtn.setOnClickListener(this);
		mTVBtn.setOnClickListener(this);
		mOptionsBtn.setOnClickListener(this);
		mTwitterBtn.setOnClickListener(this);
		mFacebookBtn.setOnClickListener(this);
	}

	public void createButtons() {
		createMusicButtons();
		createVideoButtons();
		createPictureButtons();
		createRadioButtons();
		createTVButtons();
		createOptionsButtons();
	}

	public void createMusicButtons() {
		mMusicButtons = new Button[2];
		for (int i = 0; i < 2; i++) {
			mMusicButtons[i] = new Button(getBaseContext());
			mMusicButtons[i].setOnClickListener(this);
		}

		// Musics button
		configButton(mMusicButtons[0], R.drawable.ic_musics, R.string.music, 0);

		// Playlist button
		configButton(mMusicButtons[1], R.drawable.ic_playlists, R.string.playlists, 0);
	}

	public void createVideoButtons() {
		mVideoButtons = new Button[3];
		for (int i = 0; i < 3; i++) {
			mVideoButtons[i] = new Button(getBaseContext());
			mVideoButtons[i].setOnClickListener(this);
		}

		// Videos button
		configButton(mVideoButtons[0], R.drawable.ic_videos, R.string.videos, 0);

		// Download button
		configButton(mVideoButtons[1], R.drawable.ic_youtube, R.string.download, 0);

		// Capture button
		configButton(mVideoButtons[2], R.drawable.ic_capture_video, R.string.capture_video, 0);
	}

	public void createPictureButtons() {
		mPictureButtons = new Button[3];
		for (int i = 0; i < 3; i++) {
			mPictureButtons[i] = new Button(getBaseContext());
			mPictureButtons[i].setOnClickListener(this);
		}

		// Pictures button
		configButton(mPictureButtons[0], R.drawable.ic_pictures, R.string.pictures, 0);

		// Slideshow button
		configButton(mPictureButtons[1], R.drawable.ic_slideshow, R.string.slideshow_legend, 0);

		// Capture button
		configButton(mPictureButtons[2], R.drawable.ic_capture_photo, R.string.capture_photo, 0);
	}

	public void createRadioButtons() {
		mRadioButtons = new Button[3];
		for (int i = 0; i < 3; i++) {
			mRadioButtons[i] = new Button(getBaseContext());
			mRadioButtons[i].setOnClickListener(this);
		}

		// Listen button
		configButton(mRadioButtons[0], R.drawable.ic_listen, R.string.listen, 0);

		// My Radios button
		configButton(mRadioButtons[1], R.drawable.ic_radio_stations, R.string.my_radios, 0);

		// Recordings button
		configButton(mRadioButtons[2], R.drawable.ic_recordings, R.string.radio_recordings, 0);
	}

	public void createTVButtons() {
		mTVButtons = new Button[3];
		for (int i = 0; i < 3; i++) {
			mTVButtons[i] = new Button(getBaseContext());
			mTVButtons[i].setOnClickListener(this);
		}

		// Watch button
		configButton(mTVButtons[0], R.drawable.ic_watch, R.string.watch, 0);

		// Channels button
		configButton(mTVButtons[1], R.drawable.ic_channels, R.string.my_tvs, 0);

		// Recordings button
		configButton(mTVButtons[2], R.drawable.ic_recordings, R.string.tv_recordings, 0);
	}

	public void createOptionsButtons() {
		mOptionsButtons = new Button[5];
		for (int i = 0; i < 5; i++) {
			mOptionsButtons[i] = new Button(getBaseContext());
			mOptionsButtons[i].setOnClickListener(this);
		}

		// Settings button
		configButton(mOptionsButtons[0], R.drawable.ic_settings, R.string.settings, 0);

		// Help button
		configButton(mOptionsButtons[1], R.drawable.ic_help, R.string.help, 0);

		// Contact button
		configButton(mOptionsButtons[2], R.drawable.ic_contact_us, R.string.contact_us, 0);

		// Bug Report button
		configButton(mOptionsButtons[3], R.drawable.ic_bug_report, R.string.bug_report, 0);

		// About button
		configButton(mOptionsButtons[4], R.drawable.ic_about, R.string.about, 0);
	}

	public void showMusicButtons() {
		mMenuLayout.removeAllViews();
		for (Button btn : mMusicButtons) {
			mMenuLayout.addView(btn);
			btn.startAnimation(slideInAnimation);
		}
	}

	public void showVideoButtons() {
		mMenuLayout.removeAllViews();
		for (Button btn : mVideoButtons) {
			mMenuLayout.addView(btn);
			btn.startAnimation(slideInAnimation);
		}
	}

	public void showPictureButtons() {
		mMenuLayout.removeAllViews();
		for (Button btn : mPictureButtons) {
			mMenuLayout.addView(btn);
			btn.startAnimation(slideInAnimation);
		}
	}

	public void showRadioButtons() {
		mMenuLayout.removeAllViews();
		for (Button btn : mRadioButtons) {
			mMenuLayout.addView(btn);
			btn.startAnimation(slideInAnimation);
		}
	}

	public void showTVButtons() {
		mMenuLayout.removeAllViews();
		for (Button btn : mTVButtons) {
			mMenuLayout.addView(btn);
			btn.startAnimation(slideInAnimation);
		}
	}

	public void showOptionsButtons() {
		mMenuLayout.removeAllViews();
		for (Button btn : mOptionsButtons) {
			mMenuLayout.addView(btn);
			btn.startAnimation(slideInAnimation);
		}
	}

	public void updateTextViews() {
		switch (selected_menu) {
		case MUSIC_MENU: {
			mMenuTitle.setText(R.string.music);
			break;
		}
		case VIDEO_MENU: {
			mMenuTitle.setText(R.string.video);
			break;
		}
		case PICTURE_MENU: {
			mMenuTitle.setText(R.string.picture);
			break;
		}
		case RADIO_MENU: {
			mMenuTitle.setText(R.string.radio);
			break;
		}
		case TV_MENU: {
			mMenuTitle.setText(R.string.tv);
			break;
		}
		case OPTIONS_MENU: {
			mMenuTitle.setText(R.string.options);
			break;
		}
		}
		mMenuTitle.startAnimation(slideInAnimation);
	}

	public void loadSounds() {
		startup = MediaPlayer.create(this, R.raw.startup);
		select = MediaPlayer.create(this, R.raw.select);
		choose = MediaPlayer.create(this, R.raw.press);
		cancel = MediaPlayer.create(this, R.raw.cancel);
	}

	public void loadAnimations() {
		slideInAnimation = AnimationUtils.loadAnimation(getBaseContext(), R.anim.slide_in_left);
		growFromMiddleAnimation = AnimationUtils.loadAnimation(getBaseContext(), R.anim.grow_from_middle);
	}

	public void unloadContent() {
		slideInAnimation = null;
		growFromMiddleAnimation = null;
		startup.release();
		select.release();
		choose.release();
		cancel.release();
		select = null;
		choose = null;
		cancel = null;
	}

	@Override
	public void onClick(View v) {
		if (v == mMusicBtn) {
			selected_menu = MUSIC_MENU;
			select.start();
			mMusicBtn.startAnimation(growFromMiddleAnimation);
			updateTextViews();
			showMusicButtons();
		} else if (v == mVideoBtn) {
			selected_menu = VIDEO_MENU;
			select.start();
			mVideoBtn.startAnimation(growFromMiddleAnimation);
			updateTextViews();
			showVideoButtons();
		} else if (v == mPictureBtn) {
			selected_menu = PICTURE_MENU;
			select.start();
			mPictureBtn.startAnimation(growFromMiddleAnimation);
			updateTextViews();
			showPictureButtons();
		} else if (v == mRadioBtn) {
			selected_menu = RADIO_MENU;
			select.start();
			mRadioBtn.startAnimation(growFromMiddleAnimation);
			updateTextViews();
			showRadioButtons();
		} else if (v == mTVBtn) {
			selected_menu = TV_MENU;
			select.start();
			mTVBtn.startAnimation(growFromMiddleAnimation);
			updateTextViews();
			showTVButtons();
		} else if (v == mOptionsBtn) {
			selected_menu = OPTIONS_MENU;
			select.start();
			mOptionsBtn.startAnimation(growFromMiddleAnimation);
			updateTextViews();
			showOptionsButtons();
		} else if (v == mOptionsButtons[0]) {
			select.start();
			Intent i = new Intent(this, Settings.class);
			startActivity(i);
			overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_left);
			mOptionsButtons[0].startAnimation(growFromMiddleAnimation);
		} else if (v == mTwitterBtn) {
			select.start();
			mTwitterBtn.startAnimation(growFromMiddleAnimation);
			Thread t = new Thread(new Runnable() {

				@Override
				public void run() {
					if (!twitter.getAccessToken()) {
						twitter.getAuthorizationIfNeeded();
					}
					twitter.shareTV("History Channel");
				}
			});
			t.start();
		} else if (v == mFacebookBtn) {
			select.start();
			mFacebookBtn.startAnimation(growFromMiddleAnimation);
			Thread t = new Thread(new Runnable() {

				@Override
				public void run() {
					if (!facebook.getAccessToken()) {
						facebook.getAuthorizationIfNeeded();
					}
					facebook.shareApp();
				}
			});
			t.start();
		} else if (v == mMusicButtons[0]) {
			// Music Library
			choose.start();
			mMusicButtons[0].startAnimation(growFromMiddleAnimation);
			Intent i = new Intent(this, KansusMusic.class);
			startActivity(i);
			overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_left);
		} else if (v == mMusicButtons[1]) {
			// Playlists
			choose.start();
			mMusicButtons[1].startAnimation(growFromMiddleAnimation);
			Intent i = new Intent(this, KansusMusic.class);
			startActivity(i);
			overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_left);
		} else if (v == mVideoButtons[0]) {
			// Video Library
			choose.start();
			mVideoButtons[0].startAnimation(growFromMiddleAnimation);
			Intent i = new Intent(this, GalleryChooser.class);
			i.putExtra("mediaType", ImageManager.INCLUDE_VIDEOS);
			startActivity(i);
			overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_left);
		} else if (v == mVideoButtons[1]) {
			// Video Download
			choose.start();
			mVideoButtons[1].startAnimation(growFromMiddleAnimation);
			Intent i = new Intent(this, VideoDownloader.class);
			startActivity(i);
			overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_left);
		} else if (v == mVideoButtons[2]) {
			// Video Capture
			choose.start();
			mVideoButtons[2].startAnimation(growFromMiddleAnimation);
			Intent i = new Intent(android.provider.MediaStore.ACTION_VIDEO_CAPTURE);
			startActivity(i);
			overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_left);
		} else if (v == mPictureButtons[0]) {
			// Picture Library
			choose.start();
			mPictureButtons[0].startAnimation(growFromMiddleAnimation);
			Intent i = new Intent(this, GalleryChooser.class);
			i.putExtra("mediaType", ImageManager.INCLUDE_IMAGES);
			startActivity(i);
			overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_left);
		} else if (v == mPictureButtons[1]) {
			// Slideshow
			choose.start();
			mPictureButtons[1].startAnimation(growFromMiddleAnimation);
			Intent i = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
			startActivity(i);
			overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_left);
		} else if (v == mPictureButtons[2]) {
			// Picture Capture
			choose.start();
			mPictureButtons[2].startAnimation(growFromMiddleAnimation);
			Intent i = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
			startActivity(i);
			overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_left);
		} else if (v == mRadioButtons[0]) {
			// Radio Library
			choose.start();
			mRadioButtons[0].startAnimation(growFromMiddleAnimation);
			Intent i = new Intent(getApplicationContext(), RadioLibrary.class);
			startActivity(i);
			overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_left);
		} else if (v == mRadioButtons[1]) {
			// My Radios
			choose.start();
			mRadioButtons[1].startAnimation(growFromMiddleAnimation);
			Intent i = new Intent(getApplicationContext(), MyRadios.class);
			startActivity(i);
			overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_left);
		} else if (v == mRadioButtons[2]) {
			// Radio Recordings
			choose.start();
			mRadioButtons[2].startAnimation(growFromMiddleAnimation);
			Intent i = new Intent(getApplicationContext(), RadioRecordings.class);
			startActivity(i);
			overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_left);
		} else if (v == mTVButtons[0]) {
			// TV Library
			choose.start();
			mTVButtons[0].startAnimation(growFromMiddleAnimation);
			Intent i = new Intent(getApplicationContext(), TVLibrary.class);
			startActivity(i);
			overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_left);
		} else if (v == mTVButtons[1]) {
			// My TVs
			choose.start();
			mTVButtons[1].startAnimation(growFromMiddleAnimation);
			Intent i = new Intent(getApplicationContext(), MyTVs.class);
			startActivity(i);
			overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_left);
		} else if (v == mTVButtons[2]) {
			// TV Recordings
			choose.start();
			mTVButtons[2].startAnimation(growFromMiddleAnimation);
			Intent i = new Intent(getApplicationContext(), TVRecordings.class);
			startActivity(i);
			overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_left);
		}
	}

	public void configButton(Button button, int imageId, int textId, int margin) {
		button.setLayoutParams(buttonParams);
		button.setCompoundDrawablesWithIntrinsicBounds(null, getResources().getDrawable(imageId), null, null);
		button.setBackgroundColor(Color.TRANSPARENT);
		button.setTextSize(20f);
		button.setTextColor(Color.WHITE);
		button.setText(getString(textId));
	}

	public Drawable getScaledDrawable(int resId) {
		Matrix m = new Matrix();
		Bitmap source = BitmapFactory.decodeResource(getResources(), resId);
		Bitmap result = Util.transform(m, source, dp_64, dp_64, true, Util.RECYCLE_INPUT);
		Drawable drawable = new BitmapDrawable(getApplicationContext().getResources(), result);
		result.recycle();
		return drawable;
	}
}
