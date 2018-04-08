package org.kansus.mediacenter.activity.radio;

import java.util.Arrays;

import org.kansus.mediacenter.R;
import org.kansus.mediacenter.adapter.RecordingsAdapter;
import org.kansus.mediacenter.widget.AmazingListView;

import android.app.Activity;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView;

public class RadioRecordings extends Activity implements OnItemClickListener {

	private AmazingListView mListView;
	private TextView mTitle;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.radio_recordings);
		mListView = (AmazingListView) findViewById(R.id.rr_lv);
		mTitle = (TextView) findViewById(R.id.window_title_tv);
		mListView.setOnCreateContextMenuListener(this);
		mListView.setOnItemClickListener(this);
		mTitle.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				openOptionsMenu();
			}
		});

		String[] itens = { "Rádio Chuck Norris 9999 FM (1)",
				"Rádio Chuck Norris 9999 FM (2)",
				"Rádio Chuck Norris 9999 FM (3)",
				"Rádio MacGyver Grandes Hits FM (1)",
				"Rádio Universal do Reino de Goku AM (1)",
				"Rádio Chuck Norris 9999 FM (4)" };
		RecordingsAdapter adapter = new RecordingsAdapter(getLayoutInflater(), Arrays.asList(itens));
		mListView.setAdapter(adapter);

		super.onCreate(savedInstanceState);
	}
	
	@Override
	protected void onResume() {
		overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_right);
		super.onResume();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.recordings, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.recordings_context, menu);
		super.onCreateContextMenu(menu, v, menuInfo);
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {

	}
}
