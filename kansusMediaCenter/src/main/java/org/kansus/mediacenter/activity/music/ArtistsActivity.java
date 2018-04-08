package org.kansus.mediacenter.activity.music;

import java.util.ArrayList;

import org.kansus.mediacenter.R;
import org.kansus.mediacenter.adapter.ArtistaAdapter;

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

public class ArtistsActivity extends Activity implements OnItemClickListener {

	ArrayList<String> artistas = new ArrayList<String>();
	ListView listView;
	ContentResolver contentResolver;
	Cursor cursor;
	Uri uri;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.artista);

		listView = (ListView) findViewById(R.id.listViewArtistas);
		listView.setTextFilterEnabled(true);

		contentResolver = getContentResolver();
		uri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
		cursor = contentResolver.query(uri, null, null, null, null);

		// inserindo dados em 'artistas'
		createListArtistas();

		// inserindo o conteúdo de 'artistas' no ListView
		ArtistaAdapter adapter = new ArtistaAdapter(getBaseContext(), artistas);
		listView.setAdapter(adapter);
		listView.setOnItemClickListener(this);
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {

		Bundle extras = new Bundle();
		extras.putString("nomeArtista", artistas.get(arg2));

		Intent i = new Intent(getBaseContext(), MusicActivity.class);
		i.putExtras(extras);
		startActivity(i);
		// Toast.makeText(getBaseContext(), artistas.get(arg2),
		// Toast.LENGTH_SHORT).show();
	}

	private void createListArtistas() {
		// carregar todos os artistas
		if (artistas == null)
			artistas = new ArrayList<String>();

		int columnArtist = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.ARTIST);
		// int column_index =
		// cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.DATA);

		String artist = "";

		int currentNum = -1;
		cursor.moveToFirst();
		do {
			artist = cursor.getString(columnArtist);
			if (!artistas.contains(artist)) {
				artistas.add(artist);
				currentNum++;
			}
		} while (cursor.moveToNext());
	}
}
