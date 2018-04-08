package org.kansus.mediacenter.widget;

import org.kansus.mediacenter.R;

import android.content.Context;
import android.graphics.Outline;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import io.vov.vitamio.widget.*;

public class StreamController extends io.vov.vitamio.widget.MediaController {

	private ImageButton mStopButton;
	private ImageButton mRecordButton;

	public StreamController(Context context, int dismissTime) {
		super(context);
		makeControllerView();
		initControllerView(this);
	}

	@Override
	protected View makeControllerView() {
		return ((LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.radiocontroller, this);
	}

	private OnClickListener mStopOnClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
		}
	};

	private OnClickListener mRecordOnClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
		}
	};

	private void initControllerView(View v) {
		mStopButton = (ImageButton) findViewById(R.id.mediacontroller_stop);
		if (mStopButton != null) {
			mStopButton.setOnClickListener(mStopOnClickListener);
		}

		mRecordButton = (ImageButton) findViewById(R.id.mediacontroller_record);
		if (mRecordButton != null) {
			mRecordButton.setOnClickListener(mRecordOnClickListener);
		}

		setInfoView((OutlineTextView) findViewById(R.id.mediacontroller_info_view));
	}
}