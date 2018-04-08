package org.kansus.mediacenter.activity.music;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.kansus.mediacenter.R;
import org.kansus.mediacenter.social.FacebookSharing;
import org.kansus.mediacenter.social.TwitterSharing;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import android.app.Activity;
import android.content.ContentResolver;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.TextView;

public class MusicPlayer extends Activity implements OnPreparedListener, MediaController.MediaPlayerControl{
	private static final String TAG = "AudioPlayer";

	ContentResolver contentResolver;
	Uri uri;
	Cursor cursor;
	BackgroundSound backSound;
	static String url;
	//public static final String AUDIO_FILE_NAME = "/sdcard/09elessabem.mp3";
	private MediaPlayer mediaPlayer;
	private MediaController mMediaController;
	private String audioFile;
	private Handler handler = new Handler();
	Bundle bundle;
	TextView titleMusic;
	TextView letraMusic;
	//Button urlLetraCompleta;
	static String auxTitle;
	static String nomeArtista;
	static String letraMusica;
	static Lyrics objLetra = new Lyrics("", "");
	
	private ImageView mFacebookButton;
	private ImageView mTwitterButton;
	private TwitterSharing mTwitter;
	private FacebookSharing mFacebook;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.music_player);
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
						mFacebook.shareMusic(auxTitle, nomeArtista);
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
						mTwitter.shareMusic(auxTitle, nomeArtista);
					}
				});
				t.start();
			}
		});

		titleMusic= (TextView) findViewById(R.id.textNomeMusica);

		mediaPlayer = new MediaPlayer();
		mediaPlayer.setOnPreparedListener(this);
		backSound = new BackgroundSound(mediaPlayer);
		mMediaController = new MediaController(this);

		contentResolver = getContentResolver();
		uri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI; 
		cursor = contentResolver.query(uri, null, null, null, null);

		int idfile = cursor.getColumnIndex(android.provider.MediaStore.Audio.Media.DATA);
		int idtitle = cursor.getColumnIndex(android.provider.MediaStore.Audio.Media.TITLE);
		int idartist = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.ARTIST);

		cursor.moveToFirst();
		bundle = getIntent().getExtras();
		String titleIntent = bundle.getString("nomeMusica");

		auxTitle = cursor.getString(idtitle);

		while(!titleIntent.equals(auxTitle)){
			cursor.moveToNext();
			auxTitle = cursor.getString(idtitle);
		}

		audioFile = cursor.getString(idfile);
		nomeArtista = cursor.getString(idartist);
		titleMusic.setText("Playing: " + nomeArtista + " - " + auxTitle);

		url = "http://lyrics.wikia.com/api.php?func=getSong&artist="+nomeArtista.replace(" ", "_")+"&song="+auxTitle.replace(" ", "_")+"&fmt=xml";
		letraMusic = (TextView) findViewById(R.id.textLetraMusica);

		Thread t = new Thread(new Runnable() {

			@Override
			public void run() {
				LetraLoader ll = new LetraLoader();
				ll.execute();
				//letraMusic.setText(objLetra.letra);
				//Toast.makeText(getBaseContext(), objLetra.letra, Toast.LENGTH_SHORT).show();
			}
		});

		t.start();

		while(objLetra.letra.equals("")){
			letraMusic.setText(objLetra.letra);
		}
		letraMusic.setText(objLetra.letra);
		/*urlLetraCompleta = (Button) findViewById(R.id.buttonVerLetra);
    urlLetraCompleta.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {        	
            Intent i = new Intent("android.intent.category.VIEW", Uri.parse(objLetra.urlLetraCompleta));
            startActivity(i);
        }
    });*/

		try {
			mediaPlayer.setDataSource(audioFile);
		} catch (IOException e) {
			Log.e(TAG, "Could not open file " + audioFile + " for playback.", e);
		}

		cursor.moveToNext();
		backSound.execute();
	}

	@Override
	protected void onStop() {
		super.onStop();
		//mediaController.hide();
		mediaPlayer.stop();
		mediaPlayer.release();
	}

	//--MediaPlayerControl methods----------------------------------------------------
	public void start() {
		mediaPlayer.start();
	}

	public void pause() {
		mediaPlayer.pause();
	}

	public int getDuration() {
		return mediaPlayer.getDuration();
	}

	public int getCurrentPosition() {
		return mediaPlayer.getCurrentPosition();
	}

	public void seekTo(int i) {
		mediaPlayer.seekTo(i);
	}

	public boolean isPlaying() {
		return mediaPlayer.isPlaying();
	}

	public int getBufferPercentage() {
		return 0;
	}

	public boolean canPause() {
		return true;
	}

	public boolean canSeekBackward() {
		return true;
	}

	public boolean canSeekForward() {
		return true;
	}

	@Override
	public int getAudioSessionId() {
		return 0;
	}


	//--------------------------------------------------------------------------------

	public void onPrepared(MediaPlayer mediaPlayer) {
		Log.d(TAG, "onPrepared");
		mMediaController.setMediaPlayer(this);
		mMediaController.setAnchorView(findViewById(R.id.main_audio_view));

		handler.post(new Runnable() {
			public void run() {
				mMediaController.setEnabled(true);
				mMediaController.show(0);
			}
		});
	}


	public class BackgroundSound extends AsyncTask<Void, Void, Void> {

		MediaPlayer player;

		public BackgroundSound(MediaPlayer player){
			this.player = player;
		}



		@Override
		protected Void doInBackground(Void... params) {
			//MediaPlayer player = MediaPlayer.create(YourActivity.this, R.raw.test_cbr); 
			player.setLooping(true); // Set looping 
			player.setVolume(100,100); 
			try {
				player.prepare();
			} catch (IllegalStateException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			player.start(); 

			return null;
		}
	}




	public static class LetraLoader extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			String pedacoLetra = "";
			String urlLetraCompleta = "";
			//System.out.println("oiiiii");
			try {


				DocumentBuilder builder = DocumentBuilderFactory.newInstance()
				.newDocumentBuilder();
				Document doc = builder
				.parse(connectToURL(url));

				NodeList nodes = doc.getElementsByTagName("LyricsResult");
				for (int i = 0; i < nodes.getLength(); i++) {
					Element element = (Element) nodes.item(i);

					pedacoLetra = getElementValue(element, "lyrics", null);

					urlLetraCompleta = getElementValue(element, "url",
							null);

					objLetra = new Lyrics(pedacoLetra, urlLetraCompleta);

					if (isCancelled())
						break;
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			return null;
		}
	}

	private static String getCharacterDataFromElement(Element e, String attr) {
		if (attr != null) {
			return e.getAttribute(attr);
		}
		try {
			Node child = e.getFirstChild();
			if (child instanceof CharacterData) {
				CharacterData cd = (CharacterData) child;
				return cd.getData();
			}
		} catch (Exception ex) {

		}
		return "";
	}

	private static String getElementValue(Element parent, String label,
			String attr) {
		return getCharacterDataFromElement((Element) parent
				.getElementsByTagName(label).item(0), attr);
	}



	private static InputStream connectToURL(String path) {
		try {
			URL url = new URL(path);
			HttpURLConnection urlConnection = (HttpURLConnection) url
			.openConnection();
			urlConnection.setDefaultUseCaches(false);
			urlConnection.setUseCaches(false);
			urlConnection.setRequestMethod("GET");
			urlConnection.setRequestProperty("Pragma", "no-cache");
			urlConnection.setRequestProperty("Cache-Control", "no-cache");
			urlConnection.setRequestProperty("Expires", "-1");
			urlConnection.setRequestProperty("Content-type", "text/xml");
			urlConnection.setRequestProperty("Connection", "Keep-Alive");
			return urlConnection.getInputStream();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
}

