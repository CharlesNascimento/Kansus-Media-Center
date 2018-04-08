package org.kansus.mediacenter.activity.music;

import java.util.ArrayList;

import org.kansus.mediacenter.R;
import org.kansus.mediacenter.adapter.AlbumAdapter;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

public class AlbumsActivity extends Activity implements OnItemClickListener {

	ArrayList<String> albuns = new ArrayList<String>();
	ListView listView;
	AlbumAdapter adapter;
	ContentResolver contentResolver;
	Cursor cursor;
	Uri uri;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.album);

		listView = (ListView) findViewById(R.id.listViewAlbuns);
		listView.setTextFilterEnabled(true);

		contentResolver = getContentResolver();
		uri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
		cursor = contentResolver.query(uri, null, null, null, null);

		// inserindo dados em 'artistas'
		createListArtistas();

		// inserindo o conteúdo de 'artistas' no ListView
		AlbumAdapter adapter = new AlbumAdapter(getBaseContext(), albuns);
		listView.setAdapter(adapter);
		listView.setOnItemClickListener(this);
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		Bundle extras = new Bundle();
		extras.putString("nomeAlbum", albuns.get(arg2));

		Intent i = new Intent(getBaseContext(), MusicActivity.class);
		i.putExtras(extras);
		startActivity(i);

	}

	private void createListArtistas() {
		// carregar todos os artistas
		if (albuns == null)
			albuns = new ArrayList<String>();

		int columnAlbum = cursor.getColumnIndex(android.provider.MediaStore.Audio.Media.ALBUM);
		// int column_index =
		// cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.DATA);

		String album = "";

		int currentNum = -1;
		cursor.moveToFirst();
		do {
			// columnAlbum =
			// cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM);
			album = cursor.getString(columnAlbum);
			if (!albuns.contains(album)) {
				albuns.add(album);
				currentNum++;
			}
		} while (cursor.moveToNext());
	}
}
