package org.kansus.mediacenter.activity.music;

import java.util.ArrayList;

import org.kansus.mediacenter.R;
import org.kansus.mediacenter.adapter.MusicAdapter;

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

public class MusicActivity extends Activity implements OnItemClickListener {

    ArrayList<String> musicas = new ArrayList<>();
    ListView listView;
    ContentResolver contentResolver;
    Cursor cursor;
    Uri uri;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.album);

        Bundle bundle = getIntent().getExtras();

        String nomeArtista = bundle.getString("nomeArtista");
        String nomeAlbum = bundle.getString("nomeAlbum");

        // Toast.makeText(getBaseContext(), nomeArtista+" - "+nomeAlbum,
        // Toast.LENGTH_SHORT).show();

        listView = (ListView) findViewById(R.id.listViewAlbuns);
        listView.setTextFilterEnabled(true);

        contentResolver = getContentResolver();
        uri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        cursor = contentResolver.query(uri, null, null, null, null);

        // inserindo dados em 'artistas'
        createListArtistas(nomeArtista, nomeAlbum);

        // inserindo o conteúdo de 'artistas' no ListView
        MusicAdapter adapter = new MusicAdapter(getBaseContext(), musicas);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(this);
    }

    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
        Bundle bundle = new Bundle();

        bundle.putString("nomeMusica", musicas.get(arg2));

        Intent i = new Intent(getBaseContext(), MusicPlayer.class);
        i.putExtras(bundle);
        startActivity(i);

    }

    private void createListArtistas(String nomeArtista, String nomeAlbum) {
        // carregar todos os artistas
        if (musicas == null)
            musicas = new ArrayList<String>();

        int columnTitle = cursor.getColumnIndex(android.provider.MediaStore.Audio.Media.TITLE);
        int columnAlbum = cursor.getColumnIndex(android.provider.MediaStore.Audio.Media.ALBUM);
        int columnArtist = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.ARTIST);

        String title = "";
        String album = "";
        String artista = "";

        cursor.moveToFirst();

        if (nomeArtista == null && nomeAlbum == null) {
            do {
                title = cursor.getString(columnTitle);
                musicas.add(title);
            } while (cursor.moveToNext());
        } else {
            if (nomeArtista != null) {
                do {
                    artista = cursor.getString(columnArtist);
                    if (artista.equals(nomeArtista)) {
                        title = cursor.getString(columnTitle);
                        musicas.add(title);
                    }
                } while (cursor.moveToNext());
            } else if (nomeAlbum != null) {
                do {
                    album = cursor.getString(columnAlbum);
                    if (album.equals(nomeAlbum)) {
                        title = cursor.getString(columnTitle);
                        musicas.add(title);
                    }
                } while (cursor.moveToNext());
            }
        }
    }
}
