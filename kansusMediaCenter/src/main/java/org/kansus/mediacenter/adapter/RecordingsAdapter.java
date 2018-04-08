package org.kansus.mediacenter.adapter;

import java.util.List;

import org.kansus.mediacenter.R;
import org.kansus.mediacenter.widget.AmazingAdapter;

import android.os.AsyncTask;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

//TODO fazer o adapter funcionar realmente pra várias páginas. 
public class RecordingsAdapter extends AmazingAdapter {
	
	private List<String> list;
	private LayoutInflater inflater;
	private AsyncTask<Integer, Void, Pair<Boolean, List<String>>> backgroundTask;
	
	public RecordingsAdapter(LayoutInflater inflater, List<String> data) {
		this.list = data;
		this.inflater = inflater;
	}

	public void reset() {
		if (backgroundTask != null)
			backgroundTask.cancel(false);

		// list = Data.getRows(1).second;
		notifyDataSetChanged();
	}

	@Override
	public int getCount() {
		return list.size();
	}

	@Override
	public Object getItem(int position) {
		return list.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	protected void onNextPageRequested(int page) {
		Log.d(TAG, "Got onNextPageRequested page=" + page);

		if (backgroundTask != null) {
			backgroundTask.cancel(false);
		}

		backgroundTask = new AsyncTask<Integer, Void, Pair<Boolean, List<String>>>() {
			@Override
			protected Pair<Boolean, List<String>> doInBackground(
					Integer... params) {
				// int page = params[0];

				return new Pair<Boolean, List<String>>(false, list);
			}

			@Override
			protected void onPostExecute(
					Pair<Boolean, List<String>> result) {
				if (isCancelled())
					return;

				list.addAll(result.second);
				nextPage();
				notifyDataSetChanged();

				if (result.first) {
					// still have more pages
					notifyMayHaveMorePages();
				} else {
					notifyNoMorePages();
				}
			};
		}.execute(page);
	}

	@Override
	protected void bindSectionHeader(View view, int position,
			boolean displaySectionHeader) {
	}

	@Override
	public View getAmazingView(int position, View convertView,
			ViewGroup parent) {
		View res = convertView;
		if (res == null)
			res = inflater.inflate(R.layout.recordings_item, null);

		TextView name = (TextView) res.findViewById(R.id.name_tv);
		TextView date = (TextView) res.findViewById(R.id.date_tv);
		TextView duration = (TextView) res.findViewById(R.id.duration_tv);

		String radio = list.get(position);
		name.setText(radio);
		date.setText("15/11/2012 01:22:51");
		duration.setText("37:26");

		return res;
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
}
