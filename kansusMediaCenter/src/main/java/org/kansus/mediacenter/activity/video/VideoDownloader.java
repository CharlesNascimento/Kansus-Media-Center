package org.kansus.mediacenter.activity.video;

import java.util.ArrayList;

import org.kansus.mediacenter.R;
import org.kansus.mediacenter.video.download.VideoAttributes;
import org.kansus.mediacenter.video.download.YouTubeDownload;
import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class VideoDownloader extends Activity implements TextWatcher,
		OnClickListener, YouTubeDownload.IDownloadListener, OnItemSelectedListener {

	Button mDownloadBtn;
	ProgressBar mDownloadProgressBar;
	ProgressBar mIndeteminateProgressBar;
	EditText mDownloadLinkEt;
	Spinner mAvaliableFormatsSpn;
	TextView mTitleTv;
	TextView mDurationTv;
	TextView mSizeTv;
	TextView mResolutionTv;
	TextView mProgressTv;

	ArrayAdapter<String> emptyAdapter;
	ArrayAdapter<String> adapter;

	YouTubeDownload dl;
	Thread downloadThread;

	String selectedFormat = "";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.video_downloader);
		getFields();
		configFields();

		emptyAdapter = new ArrayAdapter<String>(getBaseContext(),
				android.R.layout.simple_spinner_dropdown_item,
				new ArrayList<String>());
	}
	
	@Override
	protected void onResume() {
		overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_right);
		super.onResume();
	}

	public void getFields() {
		mDownloadBtn = (Button) findViewById(R.id.download_btn);
		mDownloadProgressBar = (ProgressBar) findViewById(R.id.download_pb);
		mIndeteminateProgressBar = (ProgressBar) findViewById(R.id.progressBar);
		mDownloadLinkEt = (EditText) findViewById(R.id.downloadLink_et);
		mAvaliableFormatsSpn = (Spinner) findViewById(R.id.avaliable_formats_sp);
		mTitleTv = (TextView) findViewById(R.id.title_tv);
		mDurationTv = (TextView) findViewById(R.id.duration_tv);
		mSizeTv = (TextView) findViewById(R.id.size_tv);
		mResolutionTv = (TextView) findViewById(R.id.resolution_tv);
		mProgressTv = (TextView) findViewById(R.id.progress_tv);
	}

	public void configFields() {
		mDownloadBtn.setOnClickListener(this);
		mDownloadLinkEt.addTextChangedListener(this);
		mAvaliableFormatsSpn.setOnItemSelectedListener(this);
		mProgressTv.setVisibility(View.INVISIBLE);
		mIndeteminateProgressBar.setVisibility(View.INVISIBLE);
		mDownloadLinkEt.setText("");
	}

	public void updateFormatsSpinner(final ArrayAdapter<String> adapter) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mAvaliableFormatsSpn.setAdapter(adapter);
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		//getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	public void afterTextChanged(Editable s) {
		if (dl != null)
			dl.interruptUrlsGathering();
		updateFormatsSpinner(emptyAdapter);
		final String link;

		if ((link = YouTubeDownload.processYouTubeUrl(s.toString())) != null) {
			mIndeteminateProgressBar.setVisibility(View.VISIBLE);
			mProgressTv.setText(R.string.gathering_video_information);
			mProgressTv.setVisibility(View.VISIBLE);

			dl = new YouTubeDownload(this);
			downloadThread = new Thread(new Runnable() {

				@Override
				public void run() {
					dl.getAvaliableFormats(link);
				}
			});
			downloadThread.start();
		}
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count,
			int after) {
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
	}

	@Override
	public void onClick(View v) {
		if (v == mDownloadBtn) {
			if (mDownloadBtn.getText().equals(getString(R.string.download))) {
				if (!selectedFormat.equals("") && !selectedFormat.equals("-1")) {
					downloadThread = new Thread(new Runnable() {

						@Override
						public void run() {
							dl.download(dl.getVideoUrls().get(selectedFormat),
									Environment.getExternalStorageDirectory()
											.getAbsolutePath());
						}
					});
					downloadThread.start();
					mProgressTv.setVisibility(View.VISIBLE);
					mDownloadBtn.setText(R.string.cancel);
				} else {
					Toast.makeText(getApplicationContext(),
							getString(R.string.no_format_selected),
							Toast.LENGTH_LONG).show();
				}
			} else {
				dl.cancelDownload();
				mProgressTv.setVisibility(View.INVISIBLE);
				mDownloadBtn.setText(R.string.download);
			}
		}
	}

	@Override
	public void onPercentageChange(final int percentage) {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				mDownloadProgressBar.setProgress(percentage);
				mProgressTv.setText(String.valueOf(percentage) + "% "
						+ getString(R.string.completed));
			}
		});
	}

	@Override
	public void onDownloadComplete() {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				Toast.makeText(getApplicationContext(),
						getString(R.string.download_completed),
						Toast.LENGTH_LONG).show();
				mProgressTv.setVisibility(View.INVISIBLE);
				mProgressTv.setText(R.string.starting_download);
				mDownloadBtn.setText(R.string.download);
			}
		});
	}

	@Override
	public void onDownloadCancel() {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				Toast.makeText(getApplicationContext(),
						getString(R.string.download_canceled),
						Toast.LENGTH_LONG).show();
				mDownloadProgressBar.setProgress(0);
				mProgressTv.setVisibility(View.INVISIBLE);
				mProgressTv.setText(R.string.starting_download);
				mDownloadBtn.setText(R.string.download);
			}
		});
	}

	@Override
	public void onCompletedGettingVideoAttributes(
			final VideoAttributes videoAttributes) {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				mTitleTv.setText(getString(R.string.title)
						+ videoAttributes.getTitle());
				mDurationTv.setText(getString(R.string.duration)
						+ videoAttributes.getDuration());
				mSizeTv.setText(getString(R.string.size)
						+ videoAttributes.getSize());
				mResolutionTv.setText(getString(R.string.resolution)
						+ videoAttributes.getResolution());

				adapter = new ArrayAdapter<String>(getBaseContext(),
						android.R.layout.simple_spinner_dropdown_item,
						dl.avaliableFormatsToString());
				updateFormatsSpinner(adapter);

				mIndeteminateProgressBar.setVisibility(View.INVISIBLE);
				mProgressTv.setVisibility(View.INVISIBLE);
				mProgressTv.setText("");
			}
		});
	}

	@Override
	public void onConnectionError() {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				Toast.makeText(getApplicationContext(),
						getString(R.string.error_connecting), Toast.LENGTH_LONG)
						.show();

				mIndeteminateProgressBar.setVisibility(View.INVISIBLE);
				mProgressTv.setVisibility(View.INVISIBLE);
				mProgressTv.setText("");
				mDownloadBtn.setText(R.string.download);
			}
		});
	}

	@Override
	public void onItemSelected(final AdapterView<?> arg0, View arg1, int arg2,
			long arg3) {
		downloadThread = new Thread(new Runnable() {

			@Override
			public void run() {
				selectedFormat = String.valueOf(dl.getItag(arg0
						.getSelectedItem().toString()));
				dl.setVideoSize(dl.getVideoUrls().get(selectedFormat));
				runOnUiThread(new Runnable() {

					@Override
					public void run() {
						mSizeTv.setText(getString(R.string.size)
								+ dl.getVideoAttributes().getSize() + " MB");
					}
				});
			}
		});
		downloadThread.start();
	}

	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
	}

	@Override
	protected void onDestroy() {
		if (dl != null) {
			dl.cancelDownload();
			dl.interruptUrlsGathering();
		}
		super.onDestroy();
	}
}
