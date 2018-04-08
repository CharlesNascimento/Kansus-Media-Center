package org.kansus.mediacenter.social;

import org.kansus.mediacenter.R;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import com.facebook.android.DialogError;
import com.facebook.android.Facebook;
import com.facebook.android.FacebookError;
import com.facebook.android.Facebook.DialogListener;

public class FacebookSharing implements DialogListener {

	private final String APP_ID = "337538509667028";
	private Facebook facebook;
	private SharedPreferences mPrefs;
	private Activity callerActivity;

	public FacebookSharing(Activity callerActivity) {
		this.callerActivity = callerActivity;
		mPrefs = callerActivity.getPreferences(Context.MODE_PRIVATE);
		facebook = new Facebook(APP_ID);
	}

	public boolean getAccessToken() {
		String access_token = mPrefs.getString("access_token", null);
		long expires = mPrefs.getLong("access_expires", 0);
		if (access_token != null) {
			facebook.setAccessToken(access_token);
		} else
			return false;
		if (expires != 0) {
			facebook.setAccessExpires(expires);
		} else
			return false;
		return true;
	}

	public void deleteAccessToken() {
		SharedPreferences.Editor editor = mPrefs.edit();
		editor.remove("access_token");
		editor.remove("access_expires");
		editor.commit();
	}

	public void getAuthorizationIfNeeded() {
		if (!facebook.isSessionValid()) {

			callerActivity.runOnUiThread(new Runnable() {

				@Override
				public void run() {
					facebook.authorize(callerActivity,
							new String[] { "publish_stream" },
							new DialogListener() {

								@Override
								public void onComplete(Bundle values) {
									SharedPreferences.Editor editor = mPrefs
											.edit();
									editor.putString("access_token",
											facebook.getAccessToken());
									editor.putLong("access_expires",
											facebook.getAccessExpires());
									editor.commit();
								}

								@Override
								public void onFacebookError(FacebookError error) {
								}

								@Override
								public void onError(DialogError e) {
								}

								@Override
								public void onCancel() {
								}
							});
				}
			});
		}
	}

	public void shareApp() {
		final Bundle params = new Bundle();
		params.putString("caption", callerActivity.getString(R.string.app_name));
		params.putString("description",
				callerActivity.getString(R.string.app_description));
		params.putString("picture",
				callerActivity.getString(R.string.app_icon_url));
		params.putString("name",
				callerActivity.getString(R.string.app_share_text));
		params.putString("link", callerActivity.getString(R.string.app_link));

		final FacebookSharing dialogListener = this;
		callerActivity.runOnUiThread(new Runnable() {
			public void run() {
				facebook.dialog(callerActivity, "feed", params, dialogListener);
			}
		});
	}

	public void shareMusic(String song, String band) {
		final Bundle params = new Bundle();
		params.putString("caption", "Ouvindo " + band + " - " + song
				+ " no Kansus Media Center para Android.");
		params.putString("picture",
				callerActivity.getString(R.string.app_icon_url));

		final FacebookSharing dialogListener = this;
		callerActivity.runOnUiThread(new Runnable() {
			public void run() {
				facebook.dialog(callerActivity, "feed", params, dialogListener);
			}
		});
	}

	public void shareVideo(String video) {
		final Bundle params = new Bundle();
		params.putString("caption", "Assistindo ao vídeo " + video
				+ " no Kansus Media Center para Android.");
		params.putString("picture",
				callerActivity.getString(R.string.app_icon_url));

		final FacebookSharing dialogListener = this;
		callerActivity.runOnUiThread(new Runnable() {
			public void run() {
				facebook.dialog(callerActivity, "feed", params, dialogListener);
			}
		});
	}

	public void shareRadio(String radio) {
		final Bundle params = new Bundle();
		params.putString("caption", "Ouvindo a rádio " + radio
				+ " no Kansus Media Center para Android.");
		params.putString("picture",
				callerActivity.getString(R.string.app_icon_url));

		final FacebookSharing dialogListener = this;
		callerActivity.runOnUiThread(new Runnable() {
			public void run() {
				facebook.dialog(callerActivity, "feed", params, dialogListener);
			}
		});
	}

	public void shareTV(String tv) {
		final Bundle params = new Bundle();
		params.putString("caption", "Assistindo ao canal de TV " + tv
				+ " no Kansus Media Center para Android.");
		params.putString("picture",
				callerActivity.getString(R.string.app_icon_url));

		final FacebookSharing dialogListener = this;
		callerActivity.runOnUiThread(new Runnable() {
			public void run() {
				facebook.dialog(callerActivity, "feed", params, dialogListener);
			}
		});
	}

	public Facebook getFacebookInstance() {
		return facebook;
	}

	@Override
	public void onComplete(Bundle values) {
		Toast.makeText(callerActivity.getBaseContext(),
				"Postagem efetuada com sucesso!", Toast.LENGTH_LONG).show();
	}

	@Override
	public void onFacebookError(FacebookError e) {
		Toast.makeText(
				callerActivity.getBaseContext(),
				"Ocorreu um erro no servidor do Facebook, tente novamente mais tarde.",
				Toast.LENGTH_LONG).show();
	}

	@Override
	public void onError(DialogError e) {
		Toast.makeText(callerActivity.getBaseContext(),
				"Ocorreu um erro inesperado, tente novamente.",
				Toast.LENGTH_LONG).show();
	}

	@Override
	public void onCancel() {
		Toast.makeText(callerActivity.getBaseContext(), "Postagem cancelada.",
				Toast.LENGTH_LONG).show();
	}
}
