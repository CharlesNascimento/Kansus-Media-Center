package org.kansus.mediacenter.social;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.kansus.mediacenter.R;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;

public class TwitterSharing {

	private final String CONSUMER_KEY = "R37KNrBDearN9yJfIuhhRA";
	private final String CONSUMER_SECRET = "hHL6fRsMwTDTMHeHGqxgjOheIRzAypesWnu9PcWRzS4";

	private Twitter twitter;
	private Activity callerActivity;
	private SharedPreferences mPrefs;

	public TwitterSharing(Activity callerActivity) {
		this.callerActivity = callerActivity;
		mPrefs = callerActivity.getPreferences(Context.MODE_PRIVATE);
		TwitterFactory tf = new TwitterFactory();
		twitter = tf.getInstance();
		twitter.setOAuthConsumer(CONSUMER_KEY, CONSUMER_SECRET);
	}

	public boolean getAccessToken() {
		String access_token = mPrefs.getString("twitter_access_token", null);
		String access_secret = mPrefs.getString("twitter_access_secret", null);

		if (access_token != null && access_secret != null) {
			AccessToken at = new AccessToken(access_token, access_secret);
			twitter.setOAuthAccessToken(at);
		} else
			return false;
		return true;
	}

	public void deleteAccessToken() {
		SharedPreferences.Editor editor = mPrefs.edit();
		editor.remove("twitter_access_token");
		editor.remove("twitter_access_secret");
		editor.commit();
	}

	public void getAuthorizationIfNeeded() {
		try {
			RequestToken requestToken = twitter.getOAuthRequestToken();
			AccessToken accessToken = null;

			BufferedReader br = new BufferedReader(new InputStreamReader(
					System.in));

			String url = requestToken.getAuthorizationURL();
			Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
			callerActivity.startActivity(intent);

			while (accessToken == null) {
				String pin = br.readLine();
				try {
					if (pin != null && pin.length() > 0) {
						accessToken = twitter.getOAuthAccessToken(requestToken,
								pin);
					} else {
						accessToken = twitter.getOAuthAccessToken(requestToken);
					}
				} catch (TwitterException te) {
					if (te.getStatusCode() == 401) {
						System.out.println("Unable to get the access token.");
					} else {
						te.printStackTrace();
					}
				}
			}
			SharedPreferences.Editor editor = mPrefs.edit();
			editor.putString("twitter_access_token", twitter
					.getOAuthAccessToken().getToken());
			editor.putString("twitter_access_secret", twitter
					.getOAuthAccessToken().getTokenSecret());
			editor.commit();
		} catch (IllegalStateException ie) {
			if (!twitter.getAuthorization().isEnabled()) {
				System.out.println("OAuth consumer key/secret is not set.");
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (TwitterException e) {
			e.printStackTrace();
		}
	}

	public void shareApp() {
		try {
			twitter.updateStatus(callerActivity
					.getString(R.string.app_share_text)
					+ ": "
					+ callerActivity.getString(R.string.app_link));
		} catch (TwitterException e) {
			e.printStackTrace();
		}
	}

	public void shareMusic(String song, String band) {
		try {
			twitter.updateStatus("Ouvindo " + band + " - " + song
					+ " no Kansus Media Center para Android.");
		} catch (TwitterException e) {
			e.printStackTrace();
		}
	}

	public void shareVideo(String video) {
		try {
			twitter.updateStatus("Assistindo ao vídeo " + video
					+ " no Kansus Media Center para Android.");
		} catch (TwitterException e) {
			e.printStackTrace();
		}
	}

	public void shareRadio(String radio) {
		try {
			twitter.updateStatus("Ouvindo a rádio " + radio
					+ " no Kansus Media Center para Android.");
		} catch (TwitterException e) {
			e.printStackTrace();
		}
	}

	public void shareTV(String tv) {
		try {
			twitter.updateStatus("Assistindo ao canal de TV " + tv
					+ " no Kansus Media Center para Android.");
		} catch (TwitterException e) {
			e.printStackTrace();
		}
	}
}