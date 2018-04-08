/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kansus.mediacenter.activity.image;

import java.util.ArrayList;
import java.util.HashSet;

import org.kansus.mediacenter.R;
import org.kansus.mediacenter.activity.Settings;
import org.kansus.mediacenter.helpers.MenuHelper;
import org.kansus.mediacenter.structure.IImage;
import org.kansus.mediacenter.structure.IImageList;
import org.kansus.mediacenter.structure.VideoObject;
import org.kansus.mediacenter.util.ImageManager;
import org.kansus.mediacenter.util.Util;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

@SuppressWarnings("unused")
public class Gallery extends Activity implements OnItemClickListener {
	private static final String STATE_SCROLL_POSITION = "scroll_position";
	private static final String STATE_SELECTED_INDEX = "first_index";

	private static final String TAG = "ImageGallery";
	private static final float INVALID_POSITION = -1f;
	private ImageManager.ImageListParam mParam;
	private IImageList mAllImages;
	private int mInclusion;
	boolean mSortAscending = false;
	private View mNoImagesView;
	public static final int CROP_MSG = 2;

	private Dialog mMediaScanningDialog;
	private MenuItem mSlideShowItem;
	private SharedPreferences mPrefs;

	private long mVideoSizeLimit = Long.MAX_VALUE;
	private View mFooterOrganizeView;

	private BroadcastReceiver mReceiver = null;

	private final Handler mHandler = new Handler();
	private boolean mPausing = true;
	private GridView mGridView;
	private GalleryAdapter mAdapter;

	// The index of the first picture in GridView.
	private int mSelectedIndex = 0;
	private float mScrollPosition = INVALID_POSITION;

	private Drawable mVideoMmsErrorOverlay;
	private Drawable mMultiSelectTrue;
	private Drawable mMultiSelectFalse;

	private Bitmap mMissingImageThumbnailBitmap;
	private Bitmap mMissingVideoThumbnailBitmap;

	private Animation mFooterAppear;
	private Animation mFooterDisappear;

	private HashSet<IImage> mMultiSelected = null;

	TextView mTitle;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

		setContentView(R.layout.gallery);

		mNoImagesView = findViewById(R.id.no_images);

		mGridView = (GridView) findViewById(R.id.grid);
		mGridView.setOnItemClickListener(this);
		mAdapter = new GalleryAdapter(getLayoutInflater());

		mFooterOrganizeView = findViewById(R.id.footer_organize);
		mFooterOrganizeView.setOnClickListener(Util.getNullOnClickListener());

		initializeFooterButtons();

		mVideoSizeLimit = Long.MAX_VALUE;
		mGridView.setOnCreateContextMenuListener(this);

		setupInclusion();

		mTitle = (TextView) findViewById(R.id.gallery_title);
		mTitle.setText(getIntent().getExtras().getString("title"));
	}

	/**
	 * Inicializa os botões do rodapé.
	 */
	private void initializeFooterButtons() {
		Button deleteButton = (Button) findViewById(R.id.button_delete);
		deleteButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				onDeleteMultipleClicked();
			}
		});

		Button shareButton = (Button) findViewById(R.id.button_share);
		shareButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				onShareMultipleClicked();
			}
		});

		Button closeButton = (Button) findViewById(R.id.button_close);
		closeButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				closeMultiSelectMode();
			}
		});
	}

	/**
	 * Adiciona o botão "Slide Show" ao menu.
	 * 
	 * @param menu
	 *            {@link Menu}
	 * @return {@link MenuItem} criado.
	 */
	private MenuItem addSlideShowMenu(Menu menu) {
		return menu.add(Menu.NONE, Menu.NONE, MenuHelper.POSITION_SLIDESHOW, R.string.slide_show)
				.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
					public boolean onMenuItemClick(MenuItem item) {
						return onSlideShowClicked();
					}
				}).setIcon(android.R.drawable.ic_menu_slideshow);
	}

	/**
	 * Método chamado quando o botão "Slide Show" for clicado.
	 * 
	 * @return <code>true</code> se não aconteceu nenhum problema.
	 */
	public boolean onSlideShowClicked() {
		if (!canHandleEvent()) {
			return false;
		}
		IImage img = getCurrentImage();
		if (img == null) {
			img = mAllImages.getImageAt(0);
			if (img == null) {
				return true;
			}
		}
		Uri targetUri = img.fullSizeImageUri();
		Uri thisUri = getIntent().getData();
		if (thisUri != null) {
			String bucket = thisUri.getQueryParameter("bucketId");
			if (bucket != null) {
				targetUri = targetUri.buildUpon().appendQueryParameter("bucketId", bucket).build();
			}
		}
		Intent intent = new Intent(Intent.ACTION_VIEW, targetUri);
		intent.putExtra("slideshow", true);
		startActivity(intent);
		overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_left);
		return true;
	}

	/**
	 * Thread responsável por deletar uma imagem.
	 */
	private final Runnable mDeletePhotoRunnable = new Runnable() {
		public void run() {
			if (!canHandleEvent())
				return;

			IImage currentImage = getCurrentImage();

			if (currentImage != null) {
				mAllImages.removeImage(currentImage);
			}
			mAdapter.notifyDataSetChanged();

			// se não sobrou nenhuma imagem, mostramos a imagem indicando que a
			// galeria está vazia
			mNoImagesView.setVisibility(mAllImages.isEmpty() ? View.VISIBLE : View.GONE);
		}
	};

	/**
	 * Pega o <code>Uri</code> da imagem atualmente selecionada.
	 * 
	 * @return <code>Uri</code> da imagem atualmente selecionada.
	 */
	private Uri getCurrentImageUri() {
		IImage image = getCurrentImage();
		if (image != null) {
			return image.fullSizeImageUri();
		} else {
			return null;
		}
	}

	/**
	 * Pega a imagem atualmente selecionada.
	 * 
	 * @return {@link IImage} referente à imagem atualmente selecionada.
	 */
	private IImage getCurrentImage() {
		System.out.println("currentSelection: " + mSelectedIndex);
		if (mSelectedIndex < 0 || mSelectedIndex >= mAllImages.getCount()) {
			return null;
		} else {
			return mAllImages.getImageAt(mSelectedIndex);
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		// mConfigurationChanged = true;
	}

	/**
	 * Informa se podemos manipular eventos a partir do estado da aplicação, se
	 * a aplicação não estiver pausada, retorna <code>true</code>.
	 * 
	 * @return <code>true</code> se a aplicação não estiver pausada.
	 */
	private boolean canHandleEvent() {
		return !mPausing;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (!canHandleEvent())
			return false;
		switch (keyCode) {
		case KeyEvent.KEYCODE_DEL:
			IImage image = getCurrentImage();
			if (image != null) {
				MenuHelper.deleteImage(this, mDeletePhotoRunnable, getCurrentImage());
			}
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public void onPause() {
		super.onPause();
		mPausing = true;

		// mLoader.stop();

		if (mReceiver != null) {
			unregisterReceiver(mReceiver);
			mReceiver = null;
		}

		// Now that we've paused the threads that are using the cursor it is
		// safe to close it.
		mAllImages.close();
		mAllImages = null;
	}

	@Override
	protected void onSaveInstanceState(Bundle state) {
		super.onSaveInstanceState(state);
		state.putFloat(STATE_SCROLL_POSITION, mScrollPosition);
		state.putInt(STATE_SELECTED_INDEX, mSelectedIndex);
	}

	@Override
	protected void onRestoreInstanceState(Bundle state) {
		super.onRestoreInstanceState(state);
		mScrollPosition = state.getFloat(STATE_SCROLL_POSITION, INVALID_POSITION);
		mSelectedIndex = state.getInt(STATE_SELECTED_INDEX, 0);
	}

	@Override
	public void onResume() {
		super.onResume();
		overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_right);
		
		mGridView.requestFocus();

		String sortOrder = mPrefs.getString("pref_gallery_sort_key", null);
		if (sortOrder != null) {
			mSortAscending = sortOrder.equals("ascending");
		}

		mPausing = false;

		// install an intent filter to receive SD card related events.
		IntentFilter intentFilter = new IntentFilter(Intent.ACTION_MEDIA_MOUNTED);
		intentFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
		intentFilter.addAction(Intent.ACTION_MEDIA_SCANNER_STARTED);
		intentFilter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
		intentFilter.addAction(Intent.ACTION_MEDIA_EJECT);
		intentFilter.addDataScheme("file");

		mReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();
				if (action.equals(Intent.ACTION_MEDIA_MOUNTED)) {
					// SD card available
					// TODO put up a "please wait" message
					// TODO also listen for the media scanner finished message
				} else if (action.equals(Intent.ACTION_MEDIA_UNMOUNTED)) {
					// SD card unavailable
					rebake(true, false);
				} else if (action.equals(Intent.ACTION_MEDIA_SCANNER_STARTED)) {
					rebake(false, true);
				} else if (action.equals(Intent.ACTION_MEDIA_SCANNER_FINISHED)) {
					rebake(false, false);
				} else if (action.equals(Intent.ACTION_MEDIA_EJECT)) {
					rebake(true, false);
				}
			}
		};
		registerReceiver(mReceiver, intentFilter);
		rebake(false, ImageManager.isMediaScannerScanning(getContentResolver()));
		mGridView.setAdapter(mAdapter);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Botão "Slide Show" do menu
		if ((mInclusion & ImageManager.INCLUDE_IMAGES) != 0) {
			mSlideShowItem = addSlideShowMenu(menu);
		}

		// Botão "Settings" do menu
		MenuItem item = menu.add(Menu.NONE, Menu.NONE, MenuHelper.POSITION_GALLERY_SETTING, R.string.camerasettings);
		item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			public boolean onMenuItemClick(MenuItem item) {
				Intent preferences = new Intent();
				preferences.setClass(Gallery.this, Settings.class);
				startActivity(preferences);
				overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_left);
				return true;
			}
		});
		item.setAlphabeticShortcut('p');
		item.setIcon(android.R.drawable.ic_menu_preferences);

		// Botão "Multi Select" do menu
		item = menu.add(Menu.NONE, Menu.NONE, MenuHelper.POSITION_MULTISELECT, R.string.multiselect);
		item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			public boolean onMenuItemClick(MenuItem item) {
				if (isInMultiSelectMode()) {
					closeMultiSelectMode();
				} else {
					openMultiSelectMode();
				}
				return true;
			}
		});
		item.setIcon(R.drawable.ic_menu_multiselect_gallery);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if (!canHandleEvent())
			return false;
		if ((mInclusion & ImageManager.INCLUDE_IMAGES) != 0) {
			boolean videoSelected = isVideoSelected();
			if (mSlideShowItem != null && mAllImages.getCount() > 0) {
				mSlideShowItem.setEnabled(!videoSelected);
			}
		}

		return true;
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		onImageTapped(arg2);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
		mSelectedIndex = info.position;

		if (!canHandleEvent())
			return;

		IImage image = getCurrentImage();

		if (image == null) {
			return;
		}

		boolean isImage = ImageManager.isImage(image);
		if (isImage) {
			// Botão "View" do menu de contexto
			menu.add(R.string.view).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
				public boolean onMenuItemClick(MenuItem item) {
					if (!canHandleEvent())
						return false;
					onImageClicked(mSelectedIndex);
					return true;
				}
			});
		}

		menu.setHeaderTitle(isImage ? R.string.context_menu_header : R.string.video_context_menu_header);
		if ((mInclusion & (ImageManager.INCLUDE_IMAGES | ImageManager.INCLUDE_VIDEOS)) != 0) {
			MenuHelper.MenuItemsResult r = MenuHelper.addImageMenuItems(menu, MenuHelper.INCLUDE_ALL, Gallery.this, mHandler, mDeletePhotoRunnable,
					new MenuHelper.MenuInvoker() {
						public void run(MenuHelper.MenuCallback cb) {
							if (!canHandleEvent()) {
								return;
							}
							cb.run(getCurrentImageUri(), getCurrentImage());
						}
					});

			if (r != null) {
				r.gettingReadyToOpen(menu, image);
			}

			if (isImage) {
				addSlideShowMenu(menu);
			}
		}
		super.onCreateContextMenu(menu, v, menuInfo);
	}

	@Override
	public boolean onSearchRequested() {
		return false;
	}

	private void rebake(boolean unmounted, boolean scanning) {
		if (mAllImages != null) {
			mAllImages.close();
			mAllImages = null;
		}

		if (mMediaScanningDialog != null) {
			mMediaScanningDialog.cancel();
			mMediaScanningDialog = null;
		}

		if (scanning) {
			mMediaScanningDialog = ProgressDialog.show(this, null, getResources().getString(R.string.wait), true, true);
		}

		mParam = allImages(!unmounted && !scanning);
		mAllImages = ImageManager.makeImageList(getContentResolver(), mParam);

		/*
		 * mGvs.setImageList(mAllImages); mGvs.setDrawAdapter(this);
		 * mGvs.setLoader(mLoader); mGvs.start();
		 */
		mAdapter.notifyDataSetChanged();
		mNoImagesView.setVisibility(mAllImages.getCount() > 0 ? View.GONE : View.VISIBLE);
	}

	/**
	 * Checa se o item selecionado é um vídeo.
	 * 
	 * @return <code>true</code> se o item selecionado for um vídeo.
	 */
	private boolean isVideoSelected() {
		IImage image = getCurrentImage();
		return (image != null) && ImageManager.isVideo(image);
	}

	/**
	 * Checa se o tipo de dado do intent é uma imagem.
	 * 
	 * @param type
	 *            <code>String</code> representando o tipo do intent.
	 * @return <code>true</code> se o tipo for imagem.
	 */
	private boolean isImageType(String type) {
		return type.equals("vnd.android.cursor.dir/image") || type.equals("image/*");
	}

	/**
	 * Checa se o tipo de dado do intent é um vídeo.
	 * 
	 * @param type
	 *            <code>String</code> representando o tipo do intent.
	 * @return <code>true</code> se o tipo for vídeo.
	 */
	private boolean isVideoType(String type) {
		return type.equals("vnd.android.cursor.dir/video") || type.equals("video/*");
	}

	/**
	 * De acordo com o intent, define o que incluímos (imagem/vídeo) na galeria.
	 */
	private void setupInclusion() {
		mInclusion = ImageManager.INCLUDE_IMAGES | ImageManager.INCLUDE_VIDEOS;

		Intent intent = getIntent();
		if (intent != null) {
			String type = intent.resolveType(this);
			if (type != null) {
				if (isImageType(type)) {
					mInclusion = ImageManager.INCLUDE_IMAGES;
				}
				if (isVideoType(type)) {
					mInclusion = ImageManager.INCLUDE_VIDEOS;
				}
			}
			Bundle extras = intent.getExtras();

			if (extras != null) {
				mInclusion = (ImageManager.INCLUDE_IMAGES | ImageManager.INCLUDE_VIDEOS) & extras.getInt("mediaTypes", mInclusion);
			}

			if (extras != null && extras.getBoolean("pick-drm")) {
				Log.d(TAG, "pick-drm is true");
				mInclusion = ImageManager.INCLUDE_DRM_IMAGES;
			}
		}
	}

	// Returns the image list parameter which contains the subset of image/video
	// we want.
	private ImageManager.ImageListParam allImages(boolean storageAvailable) {
		if (!storageAvailable) {
			return ImageManager.getEmptyImageListParam();
		} else {
			Uri uri = getIntent().getData();
			return ImageManager.getImageListParam(ImageManager.DataLocation.EXTERNAL, mInclusion, mSortAscending ? ImageManager.SORT_ASCENDING
					: ImageManager.SORT_DESCENDING, (uri != null) ? uri.getQueryParameter("bucketId") : null);
		}
	}

	/**
	 * Seleciona um imagem, no modo de multi seleção.
	 * 
	 * @param image
	 *            imagem a ser selecionada.
	 */
	private void toggleMultiSelected(IImage image) {
		int original = mMultiSelected.size();
		if (!mMultiSelected.add(image)) {
			mMultiSelected.remove(image);
		}
		mGridView.invalidate();
		if (original == 0)
			showFooter();
		if (mMultiSelected.size() == 0)
			hideFooter();
	}

	public void onImageClicked(int index) {
		if (index < 0 || index >= mAllImages.getCount()) {
			return;
		}
		mSelectedIndex = index;
		mGridView.setSelection(index);

		IImage image = mAllImages.getImageAt(index);

		if (isInMultiSelectMode()) {
			toggleMultiSelected(image);
			return;
		}
		Intent intent;
		if (image instanceof VideoObject) {
			intent = new Intent(Intent.ACTION_VIEW, image.fullSizeImageUri());
			intent.putExtra(MediaStore.EXTRA_SCREEN_ORIENTATION, ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		} else {
			intent = new Intent(this, ImageViewer.class);
			intent.putExtra(ImageViewer.KEY_IMAGE_LIST, mParam);
			intent.setData(image.fullSizeImageUri());
		}
		startActivity(intent);
		overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_left);
	}

	public void onImageTapped(int index) {
		// In the multiselect mode, once the finger finishes tapping, we hide
		// the selection box by setting the selected index to none. However, if
		// we use the dpad center key, we will keep the selected index in order
		// to show the selection box. We do this because we have the
		// multiselect marker on the images to indicate which of them are
		// selected, so we don't need the selection box, but in the dpad case
		// we still need the selection box to show as a "cursor".

		if (isInMultiSelectMode()) {
			mGridView.setSelection(-1);
			toggleMultiSelected(mAllImages.getImageAt(index));
		} else {
			onImageClicked(index);
		}
	}

	public void onScroll(float scrollPosition) {
		mScrollPosition = scrollPosition;
	}

	private boolean needsDecoration() {
		return (mMultiSelected != null);
	}

	private void initializeMultiSelectDrawables() {
		if (mMultiSelectTrue == null) {
			mMultiSelectTrue = getResources().getDrawable(R.drawable.btn_check_buttonless_on);
		}
		if (mMultiSelectFalse == null) {
			mMultiSelectFalse = getResources().getDrawable(R.drawable.btn_check_buttonless_off);
		}
	}

	// Create this bitmap lazily, and only once for all the ImageBlocks to
	// use
	public Bitmap getErrorBitmap(IImage image) {
		if (ImageManager.isImage(image)) {
			if (mMissingImageThumbnailBitmap == null) {
				mMissingImageThumbnailBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_missing_thumbnail_picture);
			}
			return mMissingImageThumbnailBitmap;
		} else {
			if (mMissingVideoThumbnailBitmap == null) {
				mMissingVideoThumbnailBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_missing_thumbnail_video);
			}
			return mMissingVideoThumbnailBitmap;
		}
	}

	/**
	 * Mostra o rodapé.
	 */
	private void showFooter() {
		mFooterOrganizeView.setVisibility(View.VISIBLE);
		if (mFooterAppear == null) {
			mFooterAppear = AnimationUtils.loadAnimation(this, R.anim.footer_appear);
		}
		mFooterOrganizeView.startAnimation(mFooterAppear);
	}

	/**
	 * Esconde o rodapé.
	 */
	private void hideFooter() {
		if (mFooterOrganizeView.getVisibility() != View.GONE) {
			mFooterOrganizeView.setVisibility(View.GONE);
			if (mFooterDisappear == null) {
				mFooterDisappear = AnimationUtils.loadAnimation(this, R.anim.footer_disappear);
			}
			mFooterOrganizeView.startAnimation(mFooterDisappear);
		}
	}

	private String getShareMultipleMimeType() {
		final int FLAG_IMAGE = 1, FLAG_VIDEO = 2;
		int flag = 0;
		for (IImage image : mMultiSelected) {
			flag |= ImageManager.isImage(image) ? FLAG_IMAGE : FLAG_VIDEO;
		}
		return flag == FLAG_IMAGE ? "image/*" : flag == FLAG_VIDEO ? "video/*" : "*/*";
	}

	private void onShareMultipleClicked() {
		if (mMultiSelected == null)
			return;
		if (mMultiSelected.size() > 1) {
			Intent intent = new Intent();
			intent.setAction(Intent.ACTION_SEND_MULTIPLE);

			String mimeType = getShareMultipleMimeType();
			intent.setType(mimeType);
			ArrayList<Parcelable> list = new ArrayList<Parcelable>();
			for (IImage image : mMultiSelected) {
				list.add(image.fullSizeImageUri());
			}
			intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, list);
			try {
				startActivity(Intent.createChooser(intent, getText(R.string.send_media_files)));
				overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_left);
			} catch (android.content.ActivityNotFoundException ex) {
				Toast.makeText(this, R.string.no_way_to_share, Toast.LENGTH_SHORT).show();
			}
		} else if (mMultiSelected.size() == 1) {
			IImage image = mMultiSelected.iterator().next();
			Intent intent = new Intent();
			intent.setAction(Intent.ACTION_SEND);
			String mimeType = image.getMimeType();
			intent.setType(mimeType);
			intent.putExtra(Intent.EXTRA_STREAM, image.fullSizeImageUri());
			boolean isImage = ImageManager.isImage(image);
			try {
				startActivity(Intent.createChooser(intent, getText(isImage ? R.string.sendImage : R.string.sendVideo)));
				overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_left);
			} catch (android.content.ActivityNotFoundException ex) {
				Toast.makeText(this, isImage ? R.string.no_way_to_share_image : R.string.no_way_to_share_video, Toast.LENGTH_SHORT).show();
			}
		}
	}

	private void onDeleteMultipleClicked() {
		if (mMultiSelected == null)
			return;
		Runnable action = new Runnable() {
			public void run() {
				ArrayList<Uri> uriList = new ArrayList<Uri>();
				for (IImage image : mMultiSelected) {
					uriList.add(image.fullSizeImageUri());
				}
				closeMultiSelectMode();
				Intent intent = new Intent(Gallery.this, DeleteImage.class);
				intent.putExtra("delete-uris", uriList);
				try {
					startActivity(intent);
					overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_left);
				} catch (ActivityNotFoundException ex) {
					Log.e(TAG, "Delete images fail", ex);
				}
			}
		};
		MenuHelper.deleteMultiple(this, action);
	}

	/**
	 * Informa se a activity está em modo de multi seleção.
	 * 
	 * @return <code>true</code> se a activity está em modo de multi seleção.
	 */
	private boolean isInMultiSelectMode() {
		return mMultiSelected != null;
	}

	/**
	 * Encerra o modo de multi seleção.
	 */
	private void closeMultiSelectMode() {
		if (mMultiSelected == null)
			return;
		mMultiSelected = null;
		mGridView.invalidate();
		hideFooter();
	}

	/**
	 * Ativa o modo de multi seleção.
	 */
	private void openMultiSelectMode() {
		if (mMultiSelected != null)
			return;
		mMultiSelected = new HashSet<IImage>();
		mGridView.invalidate();
	}

	/**
	 * Adapter para a nossa GridView.
	 * 
	 * @author Charles
	 */
	class GalleryAdapter extends BaseAdapter {
		LayoutInflater mInflater;

		GalleryAdapter(LayoutInflater inflater) {
			mInflater = inflater;
		}

		public void updateDisplay() {
			notifyDataSetChanged();
		}

		public int getCount() {
			return mAllImages.getCount();
		}

		public Object getItem(int position) {
			return null;
		}

		public long getItemId(int position) {
			return position;
		}

		public View getView(final int position, View convertView, ViewGroup parent) {
			View v;

			if (convertView == null) {
				v = mInflater.inflate(R.layout.gallery_item, null);
			} else {
				v = convertView;
			}

			TextView titleView = (TextView) v.findViewById(R.id.title);

			ImageView picture = (ImageView) v.findViewById(R.id.thumbnail_iv);
			if (mAllImages.getImageAt(position).thumbBitmap(false) != null) {
				picture.setImageBitmap(mAllImages.getImageAt(position).thumbBitmap(true));
				titleView.setText(mAllImages.getImageAt(position).getTitle());
			} else {
				picture.setImageResource(android.R.color.transparent);
				titleView.setText(mAllImages.getImageAt(position).getTitle());
			}

			// An workaround due to a bug in TextView. If the length of text is
			// different from the previous in convertView, the layout would be
			// wrong.
			titleView.requestLayout();

			return v;
		}
	}
}
