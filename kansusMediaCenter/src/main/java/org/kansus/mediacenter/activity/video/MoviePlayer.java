/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kansus.mediacenter.activity.video;

import org.kansus.mediacenter.social.FacebookSharing;
import org.kansus.mediacenter.social.TwitterSharing;
import org.kansus.mediacenter.widget.MovieViewControl;
import org.kansus.mediacenter.R;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;

import io.vov.vitamio.widget.MediaController;

/**
 * This activity plays a video from a specified URI.
 */
public class MoviePlayer extends Activity {

	private MovieViewControl mControl;
	private boolean mFinishOnCompletion;
	private boolean mResumed = false; // Whether this activity has been resumed.
	private boolean mFocused = false; // Whether this window has focus.
	private boolean mControlResumed = false; // Whether the MovieViewControl is
												// resumed.

	private TextView mVideoTitle;
	private ImageView mFacebookButton;
	private ImageView mTwitterButton;
	private TwitterSharing mTwitter;
	private FacebookSharing mFacebook;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.movie_player);
		View rootView = findViewById(R.id.root);
		mVideoTitle = (TextView) findViewById(R.id.video_title);
		mFacebookButton = (ImageView) findViewById(R.id.facebook_iv);
		mFacebook = new FacebookSharing(this);
		mFacebookButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Thread t = new Thread(new Runnable() {

					@Override
					public void run() {
						if (!mFacebook.getAccessToken()) {
							mFacebook.getAuthorizationIfNeeded();
						}
						mFacebook.shareVideo("DESABAFO DE UM JAPA.mp4");
					}
				});
				t.start();
			}
		});
		mTwitterButton = (ImageView) findViewById(R.id.twitter_iv);
		mTwitter = new TwitterSharing(this);
		mTwitterButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Thread t = new Thread(new Runnable() {

					@Override
					public void run() {
						if (!mTwitter.getAccessToken()) {
							mTwitter.getAuthorizationIfNeeded();
						}
						mTwitter.shareVideo("DESABAFO DE UM JAPA.mp4");
					}
				});
				t.start();
			}
		});
		Intent intent = getIntent();
		mControl = new MovieViewControl(rootView, this, intent.getData()) {
			@Override
			public void onCompletion() {
				if (mFinishOnCompletion) {
					finish();
				}
			}
		};
		mControl.mMediaController.setOnShownListener(new MediaController.OnShownListener() {

			@Override
			public void onShown() {
				mVideoTitle.setVisibility(View.VISIBLE);
				mFacebookButton.setVisibility(View.VISIBLE);
				mTwitterButton.setVisibility(View.VISIBLE);
			}
		});
		mControl.mMediaController.setOnHiddenListener(new MediaController.OnHiddenListener() {

			@Override
			public void onHidden() {
				mVideoTitle.setVisibility(View.GONE);
				mFacebookButton.setVisibility(View.GONE);
				mTwitterButton.setVisibility(View.GONE);
			}
		});
		if (intent.hasExtra(MediaStore.EXTRA_SCREEN_ORIENTATION)) {
			int orientation = intent.getIntExtra(MediaStore.EXTRA_SCREEN_ORIENTATION, ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
			if (orientation != getRequestedOrientation()) {
				setRequestedOrientation(orientation);
			}
		}
		mFinishOnCompletion = intent.getBooleanExtra(MediaStore.EXTRA_FINISH_ON_COMPLETION, true);
	}

	@Override
	public void onPause() {
		super.onPause();
		mResumed = false;
		if (mControlResumed) {
			mControl.onPause();
			mControlResumed = false;
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_right);
		mResumed = true;
		if (mFocused && mResumed && !mControlResumed) {
			mControl.onResume();
			mControlResumed = true;
		}
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		mFocused = hasFocus;
		if (mFocused && mResumed && !mControlResumed) {
			mControl.onResume();
			mControlResumed = true;
		}
	}

	@Override
	public boolean onSearchRequested() {
		return false;
	}
}
