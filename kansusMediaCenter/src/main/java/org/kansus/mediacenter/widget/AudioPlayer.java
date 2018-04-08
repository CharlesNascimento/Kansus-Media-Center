package org.kansus.mediacenter.widget;

import io.vov.vitamio.MediaPlayer;
import io.vov.vitamio.MediaPlayer.OnBufferingUpdateListener;
import io.vov.vitamio.MediaPlayer.OnCompletionListener;
import io.vov.vitamio.MediaPlayer.OnErrorListener;
import io.vov.vitamio.MediaPlayer.OnInfoListener;
import io.vov.vitamio.MediaPlayer.OnPreparedListener;
import io.vov.vitamio.MediaPlayer.OnSeekCompleteListener;
import io.vov.vitamio.MediaPlayer.OnTimedTextListener;

import java.io.IOException;
import java.util.HashMap;

import org.kansus.mediacenter.R;
import org.kansus.mediacenter.util.Log;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;

public class AudioPlayer implements StreamController.MediaPlayerControl {

	private Uri mUri;
	private long mDuration;

	private static final int STATE_ERROR = -1;
	private static final int STATE_IDLE = 0;
	private static final int STATE_PREPARING = 1;
	private static final int STATE_PREPARED = 2;
	private static final int STATE_PLAYING = 3;
	private static final int STATE_PAUSED = 4;
	private static final int STATE_PLAYBACK_COMPLETED = 5;
	private static final int STATE_SUSPEND = 6;
	private static final int STATE_RESUME = 7;
	private static final int STATE_SUSPEND_UNSUPPORTED = 8;

	private int mCurrentState = STATE_IDLE;
	private int mTargetState = STATE_IDLE;

	private MediaPlayer mMediaPlayer = null;
	private OnCompletionListener mOnCompletionListener;
	private OnPreparedListener mOnPreparedListener;
	private OnErrorListener mOnErrorListener;
	private OnSeekCompleteListener mOnSeekCompleteListener;
	private OnTimedTextListener mOnSubtitleUpdateListener;
	private OnInfoListener mOnInfoListener;
	private OnBufferingUpdateListener mOnBufferingUpdateListener;
	private int mCurrentBufferPercentage;
	private long mSeekWhenPrepared;
	private boolean mCanPause = true;
	private boolean mCanSeekBack = true;
	private boolean mCanSeekForward = true;
	private Context mContext;

	public AudioPlayer(Context context) {
		initVideoView(context);
	}

	private void initVideoView(Context ctx) {
		mContext = ctx;
		mCurrentState = STATE_IDLE;
		mTargetState = STATE_IDLE;
		if (ctx instanceof Activity)
			((Activity) ctx).setVolumeControlStream(AudioManager.STREAM_MUSIC);
	}

	public void setAudioURI(Uri uri) {
		mUri = uri;
		mSeekWhenPrepared = 0;
		openAudio();
	}

	public void stopPlayback() {
		if (mMediaPlayer != null) {
			mMediaPlayer.stop();
			mMediaPlayer.release();
			mMediaPlayer = null;
			mCurrentState = STATE_IDLE;
			mTargetState = STATE_IDLE;
		}
	}

	private void openAudio() {
		if (mUri == null)
			return;

		Intent i = new Intent("com.android.music.musicservicecommand");
		i.putExtra("command", "pause");
		mContext.sendBroadcast(i);

		release(false);
		try {
			mDuration = -1;
			mCurrentBufferPercentage = 0;
			mMediaPlayer = new MediaPlayer(mContext);
			mMediaPlayer.setOnPreparedListener(mPreparedListener);
			mMediaPlayer.setOnCompletionListener(mCompletionListener);
			mMediaPlayer.setOnErrorListener(mErrorListener);
			mMediaPlayer.setOnBufferingUpdateListener(mBufferingUpdateListener);
			mMediaPlayer.setOnInfoListener(mInfoListener);
			mMediaPlayer.setOnSeekCompleteListener(mSeekCompleteListener);
			mMediaPlayer.setOnTimedTextListener(mSubtitleUpdateListener);
			mMediaPlayer.setDataSource(mContext, mUri);
			mMediaPlayer.setScreenOnWhilePlaying(true);
			mMediaPlayer.prepareAsync();
			mCurrentState = STATE_PREPARING;
		} catch (IOException ex) {
			Log.e("Unable to open content: " + mUri, ex);
			mCurrentState = STATE_ERROR;
			mTargetState = STATE_ERROR;
			mErrorListener.onError(mMediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
			return;
		} catch (IllegalArgumentException ex) {
			Log.e("Unable to open content: " + mUri, ex);
			mCurrentState = STATE_ERROR;
			mTargetState = STATE_ERROR;
			mErrorListener.onError(mMediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
			return;
		}
	}

	OnPreparedListener mPreparedListener = new OnPreparedListener() {
		public void onPrepared(MediaPlayer mp) {
			Log.d("onPrepared");
			mCurrentState = STATE_PREPARED;
			mTargetState = STATE_PLAYING;

			if (mOnPreparedListener != null)
				mOnPreparedListener.onPrepared(mMediaPlayer);

			long seekToPosition = mSeekWhenPrepared;

			if (seekToPosition != 0)
				seekTo(seekToPosition);
			if (mTargetState == STATE_PLAYING) {
				start();
			}
		}
	};

	private OnCompletionListener mCompletionListener = new OnCompletionListener() {
		public void onCompletion(MediaPlayer mp) {
			Log.d("onCompletion");
			mCurrentState = STATE_PLAYBACK_COMPLETED;
			mTargetState = STATE_PLAYBACK_COMPLETED;
			if (mOnCompletionListener != null)
				mOnCompletionListener.onCompletion(mMediaPlayer);
		}
	};

	private OnErrorListener mErrorListener = new OnErrorListener() {
		public boolean onError(MediaPlayer mp, int framework_err, int impl_err) {
			Log.d("Error: %d, %d", framework_err, impl_err);
			mCurrentState = STATE_ERROR;
			mTargetState = STATE_ERROR;

			if (mOnErrorListener != null) {
				if (mOnErrorListener.onError(mMediaPlayer, framework_err, impl_err))
					return true;
			}

			int message = framework_err == MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK ? R.string.vitamio_videoview_error_text_invalid_progressive_playback
					: R.string.vitamio_videoview_error_text_unknown;

			new AlertDialog.Builder(mContext).setTitle(R.string.vitamio_videoview_error_title).setMessage(message)
					.setPositiveButton(R.string.vitamio_videoview_error_button, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							if (mOnCompletionListener != null)
								mOnCompletionListener.onCompletion(mMediaPlayer);
						}
					}).setCancelable(false).show();
			return true;
		}
	};

	private OnBufferingUpdateListener mBufferingUpdateListener = new OnBufferingUpdateListener() {
		public void onBufferingUpdate(MediaPlayer mp, int percent) {
			mCurrentBufferPercentage = percent;
			if (mOnBufferingUpdateListener != null)
				mOnBufferingUpdateListener.onBufferingUpdate(mp, percent);
		}
	};

	private OnInfoListener mInfoListener = new OnInfoListener() {
		@Override
		public boolean onInfo(MediaPlayer mp, int what, int extra) {
			Log.d("onInfo: (%d, %d)", what, extra);
			if (mOnInfoListener != null) {
				mOnInfoListener.onInfo(mp, what, extra);
			} else if (mMediaPlayer != null) {
				if (what == MediaPlayer.MEDIA_INFO_BUFFERING_START)
					mMediaPlayer.pause();
				else if (what == MediaPlayer.MEDIA_INFO_BUFFERING_END)
					mMediaPlayer.start();
			}

			return true;
		}
	};

	private OnSeekCompleteListener mSeekCompleteListener = new OnSeekCompleteListener() {
		@Override
		public void onSeekComplete(MediaPlayer mp) {
			Log.d("onSeekComplete");
			if (mOnSeekCompleteListener != null)
				mOnSeekCompleteListener.onSeekComplete(mp);
		}
	};

	private OnTimedTextListener mSubtitleUpdateListener = new OnTimedTextListener() {
		@Override
		public void onTimedText(String text) {
			Log.i("onSubtitleUpdate: %s", text);
			if (mOnSubtitleUpdateListener != null)
				mOnSubtitleUpdateListener.onTimedText(text);
		}

		@Override
		public void onTimedTextUpdate(byte[] pixels, int width, int height) {
			Log.i("onSubtitleUpdate: bitmap subtitle, %dx%d", width, height);
			if (mOnSubtitleUpdateListener != null)
				mOnSubtitleUpdateListener.onTimedTextUpdate(pixels, width, height);
		}
	};

	public void setOnPreparedListener(OnPreparedListener l) {
		mOnPreparedListener = l;
	}

	public void setOnCompletionListener(OnCompletionListener l) {
		mOnCompletionListener = l;
	}

	public void setOnErrorListener(OnErrorListener l) {
		mOnErrorListener = l;
	}

	public void setOnBufferingUpdateListener(OnBufferingUpdateListener l) {
		mOnBufferingUpdateListener = l;
	}

	public void setOnSeekCompleteListener(OnSeekCompleteListener l) {
		mOnSeekCompleteListener = l;
	}

	public void setOnSubtitleUpdateListener(OnTimedTextListener l) {
		mOnSubtitleUpdateListener = l;
	}

	public void setOnInfoListener(OnInfoListener l) {
		mOnInfoListener = l;
	}

	private void release(boolean cleartargetstate) {
		if (mMediaPlayer != null) {
			mMediaPlayer.reset();
			mMediaPlayer.release();
			mMediaPlayer = null;
			mCurrentState = STATE_IDLE;
			if (cleartargetstate)
				mTargetState = STATE_IDLE;
		}
	}

	@Override
	public void start() {
		if (isInPlaybackState()) {
			mMediaPlayer.start();
			mCurrentState = STATE_PLAYING;
		}
		mTargetState = STATE_PLAYING;
	}

	@Override
	public void pause() {
		if (isInPlaybackState()) {
			if (mMediaPlayer.isPlaying()) {
				mMediaPlayer.pause();
				mCurrentState = STATE_PAUSED;
			}
		}
		mTargetState = STATE_PAUSED;
	}

	public void suspend() {
		if (isInPlaybackState()) {
			release(false);
			mCurrentState = STATE_SUSPEND_UNSUPPORTED;
			Log.d("Unable to suspend video. Release MediaPlayer.");
		}
	}

	public void resume() {
		if (mCurrentState == STATE_SUSPEND) {
			mTargetState = STATE_RESUME;
		} else if (mCurrentState == STATE_SUSPEND_UNSUPPORTED) {
			openAudio();
		}
	}

	public void stop() {
		if (mMediaPlayer != null)
			mMediaPlayer.stop();
	}

	@Override
	public long getDuration() {
		if (isInPlaybackState()) {
			if (mDuration > 0)
				return mDuration;
			mDuration = mMediaPlayer.getDuration();
			return mDuration;
		}
		mDuration = -1;
		return mDuration;
	}

	@Override
	public long getCurrentPosition() {
		if (isInPlaybackState())
			return mMediaPlayer.getCurrentPosition();
		return 0;
	}

	@Override
	public void seekTo(long msec) {
		if (isInPlaybackState()) {
			mMediaPlayer.seekTo(msec);
			mSeekWhenPrepared = 0;
		} else {
			mSeekWhenPrepared = msec;
		}
	}

	@Override
	public boolean isPlaying() {
		return isInPlaybackState() && mMediaPlayer.isPlaying();
	}

	@Override
	public int getBufferPercentage() {
		if (mMediaPlayer != null)
			return mCurrentBufferPercentage;
		return 0;
	}

	public void setVolume(float leftVolume, float rightVolume) {
		if (mMediaPlayer != null)
			mMediaPlayer.setVolume(leftVolume, rightVolume);
	}

	public void setBufferSize(int bufSize) {
		if (mMediaPlayer != null)
			mMediaPlayer.setBufferSize(bufSize);
	}

	public boolean isBuffering() {
		if (mMediaPlayer != null)
			return mMediaPlayer.isBuffering();
		return false;
	}

	public void setMetaEncoding(String encoding) {
		if (mMediaPlayer != null)
			mMediaPlayer.setMetaEncoding(encoding);
	}

	public String getMetaEncoding() {
		if (mMediaPlayer != null)
			return mMediaPlayer.getMetaEncoding();
		return null;
	}

	public int getAudioTrack() {
		if (mMediaPlayer != null)
			return mMediaPlayer.getAudioTrack();
		return -1;
	}

	protected boolean isInPlaybackState() {
		return (mMediaPlayer != null && mCurrentState != STATE_ERROR && mCurrentState != STATE_IDLE && mCurrentState != STATE_PREPARING);
	}
	/*
	@Override
	public boolean canPause() {
		return mCanPause;
	}

	@Override
	public boolean canSeekBackward() {
		return mCanSeekBack;
	}

	@Override
	public boolean canSeekForward() {
		return mCanSeekForward;
	}*/
}