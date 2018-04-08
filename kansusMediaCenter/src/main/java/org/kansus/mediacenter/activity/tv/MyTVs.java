package org.kansus.mediacenter.activity.tv;

import org.kansus.mediacenter.R;

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
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class MyTVs extends Activity implements OnItemClickListener {

	private ListView mListView;
	private TextView mTitle;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.my_tvs);
		mListView = (ListView) findViewById(R.id.mt_lv);
		mTitle = (TextView) findViewById(R.id.window_title_tv);
		mListView.setOnCreateContextMenuListener(this);
		mListView.setOnItemClickListener(this);
		mTitle.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				openOptionsMenu();
			}
		});
		
		String[] itens = { "Rede Chuck Norris de Televisão",
				"TV Confins do Inferno",
				"TV Marte News",
				"Potrancas Asiáticas TV",
				"MacGyver TV",
				"TV Universal do Reino de Goku",
				"TV Não Tenho Mais Ideia de Nomes"};
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(
				getApplicationContext(),
				android.R.layout.simple_selectable_list_item, itens);
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
		inflater.inflate(R.menu.my_tvs, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.my_tvs_context, menu);
		super.onCreateContextMenu(menu, v, menuInfo);
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {

	}
}
