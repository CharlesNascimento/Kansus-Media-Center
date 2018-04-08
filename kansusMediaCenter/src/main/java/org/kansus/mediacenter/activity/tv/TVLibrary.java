package org.kansus.mediacenter.activity.tv;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.kansus.mediacenter.R;
import org.kansus.mediacenter.adapter.RadioTVLibraryAdapter;
import org.kansus.mediacenter.data.Category;
import org.kansus.mediacenter.data.Item;
import org.kansus.mediacenter.data.ListItem;
import org.kansus.mediacenter.data.TVCategories;
import org.kansus.mediacenter.widget.AmazingListView;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.FrameLayout.LayoutParams;
import android.widget.TextView;
import android.widget.ViewFlipper;

public class TVLibrary extends Activity implements OnClickListener, OnItemClickListener {

	private AmazingListView mMainListView;
	private RadioTVLibraryAdapter mMainAdapter;
	private ArrayList<SecondaryList> mSecondaryLists;

	private TextView mTitle;
	private ViewFlipper mFlipper;

	private int currentPage;
	private ArrayList<String> currentCategories = new ArrayList<String>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.radio_tv_library);

		mMainAdapter = new RadioTVLibraryAdapter(getLayoutInflater(), TVCategories.mainCategories, null);
		mMainListView = (AmazingListView) findViewById(R.id.listView);
		mMainListView.setLoadingView(getLayoutInflater().inflate(R.layout.loading_view, null));
		mMainListView.setOnItemClickListener(this);
		mMainListView.setAdapter(mMainAdapter);

		mTitle = (TextView) findViewById(R.id.window_title_tv);
		mTitle.setText(R.string.tv_library);
		mTitle.setOnClickListener(this);

		createSecondaryLists(1);

		mFlipper = (ViewFlipper) findViewById(R.id.flipper);
		Animation animIn = AnimationUtils.loadAnimation(this, R.anim.slide_in_left);
		Animation animOut = AnimationUtils.loadAnimation(this, R.anim.slide_out_left);
		mFlipper.setInAnimation(animIn);
		mFlipper.setOutAnimation(animOut);
		mFlipper.addView(mSecondaryLists.get(0).listView);

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
		inflater.inflate(R.menu.radio_tv_library, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.tv_library_context, menu);
		super.onCreateContextMenu(menu, v, menuInfo);
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {

		return super.onMenuItemSelected(featureId, item);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (mFlipper.getDisplayedChild() != 0) {
				mFlipper.showPrevious();
				currentPage--;
				currentCategories.remove(currentCategories.size() - 1);
				updateWindowTitle();
				return true;
			}
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.window_title_tv)
			openOptionsMenu();
	}

	@Override
	public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
		ArrayList<ListItem> categories = null;
		NodeList items = null;
		if (currentCategories.isEmpty()) {
			String category = ((Category) mMainAdapter.getItem(position)).name;
			currentCategories.add(category);
			updateWindowTitle();

			items = getCurrentCategoryItems();
			RadioTVLibraryAdapter mSecondaryAdapter = new RadioTVLibraryAdapter(getLayoutInflater(), categories, items);
			mSecondaryLists.get(currentPage).adapter = mSecondaryAdapter;
			mSecondaryLists.get(currentPage++).listView.setAdapter(mSecondaryAdapter);
			mSecondaryAdapter.notifyDataSetChanged();
			mFlipper.showNext();
		} else if (mSecondaryLists.get(currentPage - 1).adapter.getItemViewType(position) == RadioTVLibraryAdapter.TYPE_CATEGORY) {
			String category = ((Category) mSecondaryLists.get(currentPage - 1).adapter.getItem(position)).name;
			currentCategories.add(category);
			updateWindowTitle();

			System.out.println("CHEGOU AQUI");
			items = getCurrentCategoryItems();

			RadioTVLibraryAdapter mSecondaryAdapter = new RadioTVLibraryAdapter(getLayoutInflater(), categories, items);
			mSecondaryLists.get(currentPage).adapter = mSecondaryAdapter;
			mSecondaryLists.get(currentPage++).listView.setAdapter(mSecondaryAdapter);
			mSecondaryAdapter.notifyDataSetChanged();
			mFlipper.showNext();
		} else if (mSecondaryLists.get(currentPage - 1).adapter.getItemViewType(position) == RadioTVLibraryAdapter.TYPE_ITEM) {
			Item selectedItem = (Item) mSecondaryLists.get(currentPage - 1).adapter.getItem(position);
			String name = selectedItem.name;
			String location = selectedItem.location;
			String site = selectedItem.site;
			String stream = selectedItem.stream;
			String thumbnail = selectedItem.thumbnail;

			System.out.println("Name, Location, Site, Stream, Thumbnail: " + name + location + site + stream + thumbnail);

			Intent i = new Intent(getBaseContext(), TVPlayer.class);
			System.out.println("name: " + name + " stream: " + stream);
			i.putExtra("name", name);
			i.putExtra("location", location);
			i.putExtra("site", site);
			i.putExtra("stream", stream);
			i.putExtra("thumbnail", thumbnail);
			startActivity(i);
			overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_left);
		}
	}

	private void createSecondaryLists(int count) {
		this.mSecondaryLists = new ArrayList<SecondaryList>();
		for (int i = 0; i < count; i++) {
			AmazingListView lv = new AmazingListView(this);
			lv.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
			lv.setOnCreateContextMenuListener(this);
			lv.setOnItemClickListener(this);
			SecondaryList sl = new SecondaryList();
			sl.listView = lv;
			this.mSecondaryLists.add(sl);
		}
	}

	private NodeList getCurrentCategoryItems() {
		try {
			String currentCategoryFileName = getCurrentCategoryFileName();
			DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			String xmlPath = "tvs/" + currentCategoryFileName;
			Document document = builder.parse(getResources().getAssets().open(xmlPath));
			NodeList nodes = document.getElementsByTagName("channel");
			return nodes;
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
		return null;
	}

	private String getCurrentCategoryFileName() {
		String fileName = "";
		for (String cat : currentCategories)
			fileName += "_" + cat.toLowerCase(Locale.ENGLISH);
		fileName = fileName.replaceFirst("_", "").replace("/", "_") + ".xml";
		return fileName;
	}

	private void updateWindowTitle() {
		String title = "";
		if (currentCategories.isEmpty())
			title = getString(R.string.tv_library);
		else
			for (String cat : currentCategories)
				title += " - " + cat;
		title = title.replaceFirst(" - ", "");
		this.mTitle.setText(title);
	}

	class SecondaryList {
		AmazingListView listView;
		RadioTVLibraryAdapter adapter;
	}
}
