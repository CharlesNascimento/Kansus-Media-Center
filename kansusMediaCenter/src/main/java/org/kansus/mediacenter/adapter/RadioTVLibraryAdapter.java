package org.kansus.mediacenter.adapter;

import java.util.ArrayList;
import java.util.List;

import org.kansus.mediacenter.R;
import org.kansus.mediacenter.data.Category;
import org.kansus.mediacenter.data.Item;
import org.kansus.mediacenter.data.ListItem;
import org.kansus.mediacenter.load.ImageLoader;
import org.kansus.mediacenter.widget.AmazingAdapter;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class RadioTVLibraryAdapter extends AmazingAdapter {

	public static final int TYPE_CATEGORY = 0;
	public static final int TYPE_ITEM = 1;
	private static final int ITEMS_PER_PAGE = 20;

	private List<ListItem> allItems;
	private NodeList allItemsXML;

	private LayoutInflater inflater;
	private ImageLoader imageLoader;

	private AsyncTask<Integer, Void, Boolean> backgroundTask;

	public RadioTVLibraryAdapter(LayoutInflater inflater, List<ListItem> data, NodeList allItemsXML) {
		this.imageLoader = new ImageLoader(inflater.getContext());
		this.allItems = data != null ? data : new ArrayList<ListItem>();
		this.inflater = inflater;
		this.allItemsXML = allItemsXML;

		if (allItemsXML != null)
			reset();
	}

	public void reset() {
		if (backgroundTask != null)
			backgroundTask.cancel(false);

		onNextPageRequested(0);
	}

	@Override
	public int getCount() {
		return allItems.size();
	}

	@Override
	public Object getItem(int position) {
		return allItems.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public int getViewTypeCount() {
		return 2;
	}

	@Override
	public int getItemViewType(int position) {
		if (allItems.get(position) instanceof Category) {
			return TYPE_CATEGORY;
		}
		return TYPE_ITEM;
	}

	@Override
	protected void onNextPageRequested(int page) {
		Log.d(TAG, "Got onNextPageRequested page=" + page);

		if (backgroundTask != null) {
			backgroundTask.cancel(false);
		}

		backgroundTask = new AsyncTask<Integer, Void, Boolean>() {
			@Override
			protected Boolean doInBackground(Integer... params) {
				int page = params[0];
				int lastLoadedItemFromXML = page * ITEMS_PER_PAGE;
				int to = (lastLoadedItemFromXML + ITEMS_PER_PAGE < allItemsXML.getLength()) ? ITEMS_PER_PAGE
						: (allItemsXML.getLength() - lastLoadedItemFromXML);
				System.out.println("TO: " + to);

				for (int i = page * ITEMS_PER_PAGE; i < to; i++) {
					Element element = (Element) allItemsXML.item(i);
					String name = getElementValue(element, "name", null);
					String category = getElementValue(element, "category", null);
					String location = getElementValue(element, "location", null);
					String site = getElementValue(element, "site", null);
					String stream = getElementValue(element, "stream", null);
					String thumbnail = getElementValue(element, "thumbnail", null);

					Item item = new Item(name, category, location, site, stream, thumbnail);
					allItems.add(item);
					if (isCancelled())
						cancel(true);
				}

				return Boolean.valueOf(to < 20 ? false : true);
			}

			@Override
			protected void onPostExecute(Boolean result) {
				if (isCancelled())
					return;

				nextPage();
				notifyDataSetChanged();

				if (result) {
					notifyMayHaveMorePages();
				} else {
					notifyNoMorePages();
				}
			};
		}.execute(page);
	}

	@Override
	protected void bindSectionHeader(View view, int position, boolean displaySectionHeader) {
	}

	@Override
	public View getAmazingView(int position, View convertView, ViewGroup parent) {
		int viewType = this.getItemViewType(position);
		if (viewType == TYPE_CATEGORY) {
			View view = convertView;
			CategoryViewHolder holder;
			if (view == null) {
				view = inflater.inflate(R.layout.library_categogy_item, null);
				holder = new CategoryViewHolder();
				holder.image = (ImageView) view.findViewById(R.id.category_iv);
				holder.name = (TextView) view.findViewById(R.id.category_tv);
				view.setTag(holder);
			} else {
				holder = (CategoryViewHolder) view.getTag();
			}

			String name = ((Category) this.allItems.get(position)).name;
			int imageID = ((Category) this.allItems.get(position)).imageID;
			holder.name.setText(name);
			if (imageID != -1)
				holder.image.setImageResource(imageID);
			else
				holder.image.setVisibility(View.GONE);

			return view;
		} else {
			View view = convertView;
			ItemViewHolder holder;
			if (view == null) {
				view = inflater.inflate(R.layout.library_item, null);
				holder = new ItemViewHolder();
				holder.logo = (ImageView) view.findViewById(R.id.logo_iv);
				holder.name = (TextView) view.findViewById(R.id.name_tv);
				holder.location = (TextView) view.findViewById(R.id.location_tv);
				view.setTag(holder);
			} else {
				holder = (ItemViewHolder) view.getTag();
			}

			String name = ((Item) this.allItems.get(position)).name;
			String location = ((Item) this.allItems.get(position)).location;
			String imageURL = ((Item) this.allItems.get(position)).thumbnail;
			holder.name.setText(name);
			holder.location.setText(location);
			if (!TextUtils.isEmpty(imageURL))
				imageLoader.DisplayImage(imageURL, holder.logo);

			return view;
		}
	}

	@Override
	public void configurePinnedHeader(View header, int position, int alpha) {
	}

	@Override
	public int getPositionForSection(int section) {
		return 0;
	}

	@Override
	public int getSectionForPosition(int position) {
		return 0;
	}

	@Override
	public Object[] getSections() {
		return null;
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

	private static String getElementValue(Element parent, String label, String attr) {
		return getCharacterDataFromElement((Element) parent.getElementsByTagName(label).item(0), attr);
	}

	static class ItemViewHolder {
		ImageView logo;
		TextView name;
		TextView location;
	}

	static class CategoryViewHolder {
		ImageView image;
		TextView name;
	}
}
