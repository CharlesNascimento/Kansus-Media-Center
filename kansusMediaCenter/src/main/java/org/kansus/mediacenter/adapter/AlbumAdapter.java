package org.kansus.mediacenter.adapter;

import java.util.ArrayList;

import org.kansus.mediacenter.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class AlbumAdapter extends BaseAdapter{

	private ArrayList<String> list;
	private LayoutInflater inflater;

	public AlbumAdapter(Context context, ArrayList<String> list) {
		this.list = list;
		inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
	public int getCount() {
		return this.list.size();
	}

	@Override
	public Object getItem(int position) {
		return this.list.get(position);
	}

	@Override
	public long getItemId(int position) {
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		//Pega o item de acordo com a posição.
		String item = this.list.get(position);

		//infla o layout para podermos preencher os dados
		convertView = inflater.inflate(R.layout.item_album, null);

		//inserindo item no list
		TextView nomeAlbum = (TextView)convertView.findViewById(R.id.textViewAlbum);
		nomeAlbum.setText(item);
		
		/*TextView nomeArtista = (TextView)convertView.findViewById(R.id.textViewArtista);
		nomeArtista.setText(item);*/

		return convertView;

	}

}
