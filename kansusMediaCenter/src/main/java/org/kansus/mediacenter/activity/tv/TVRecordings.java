package org.kansus.mediacenter.activity.tv;

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
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView;

public class TVRecordings extends Activity implements OnItemClickListener {

	private AmazingListView mListView;
	private TextView mTitle;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.tv_recordings);
		mListView = (AmazingListView) findViewById(R.id.tr_lv);
		mTitle = (TextView) findViewById(R.id.window_title_tv);
		mListView.setOnCreateContextMenuListener(this);
		mListView.setOnItemClickListener(this);
		mTitle.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				openOptionsMenu();
			}
		});

		String[] itens = { "Rede Chuck Norris de Televisão (1)",
				"Rede Chuck Norris de Televisão (2)",
				"Rede Chuck Norris de Televisão (3)",
				"Potrancas Asiáticas TV (1)", "Potrancas Asiáticas TV (2)",
				"TV Universal do Reino de Goku (1)" };
		RecordingsAdapter adapter = new RecordingsAdapter(
				getLayoutInflater(), Arrays.asList(itens));
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
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		System.out.println("OOOOOPPPPPAAAA");
		return super.onMenuItemSelected(featureId, item);
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {

	}
}
