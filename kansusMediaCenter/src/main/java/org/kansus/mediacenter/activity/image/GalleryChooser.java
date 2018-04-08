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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.kansus.mediacenter.R;
import org.kansus.mediacenter.activity.Settings;
import org.kansus.mediacenter.helpers.MenuHelper;
import org.kansus.mediacenter.structure.IImage;
import org.kansus.mediacenter.structure.IImageList;
import org.kansus.mediacenter.util.BitmapManager;
import org.kansus.mediacenter.util.ImageManager;
import org.kansus.mediacenter.util.Util;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.StatFs;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Activity em que escolhemos qual galeria queremos explorar.
 */
public class GalleryChooser extends Activity implements OnItemClickListener {

	private static final String TAG = "GalleryPicker";
	private static final long LOW_STORAGE_THRESHOLD = 1024 * 1024 * 2;
	private static final int THUMB_SIZE = 142;

	// thread principal
	private Handler mHandler = new Handler();
	// thread para carregamento de conteúdo
	private Thread mWorkerThread;
	// usado para parar a thread carregadora
	private volatile boolean mAbort = false;

	private BroadcastReceiver mReceiver;
	private ContentObserver mDbObserver;

	private GridView mGridView;
	private GalleryChooserAdapter mAdapter;

	private View mNoImagesView;
	private Dialog mMediaScanningDialog;

	private boolean mScanning;
	private boolean mUnmounted;

	// imagem ou vídeo
	private int mMediaType;

	// todas as listas carregadas ficarão nesta coleção, mantendo a referência à
	// elas, assim podemos fechar todas elas quando não forem mais necessárias.
	private ArrayList<IImageList> mAllLists = new ArrayList<IImageList>();

	private Drawable mFrameGalleryMask;
	private Drawable mVideoOverlay;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		setContentView(R.layout.gallery_chooser);

		mGridView = (GridView) findViewById(R.id.albums);
		mGridView.setOnItemClickListener(this);
		mGridView.setOnCreateContextMenuListener(this);

		mReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				onReceiveMediaBroadcast(intent);
			}
		};

		mDbObserver = new ContentObserver(mHandler) {
			@Override
			public void onChange(boolean selfChange) {
				reloadContent(false,
						ImageManager
								.isMediaScannerScanning(getContentResolver()));
			}
		};

		mMediaType = getIntent().getExtras().getInt("mediaType",
				ImageManager.INCLUDE_IMAGES);

		ImageManager.ensureOSXCompatibleFolder();
	}

	@Override
	public void onStart() {
		super.onStart();

		mAdapter = new GalleryChooserAdapter(getLayoutInflater());
		mGridView.setAdapter(mAdapter);

		// install an intent filter to receive SD card related events.
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
		intentFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
		intentFilter.addAction(Intent.ACTION_MEDIA_SCANNER_STARTED);
		intentFilter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
		intentFilter.addAction(Intent.ACTION_MEDIA_EJECT);
		intentFilter.addDataScheme("file");

		registerReceiver(mReceiver, intentFilter);

		getContentResolver()
				.registerContentObserver(
						MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true,
						mDbObserver);

		// Assume the storage is mounted and not scanning.
		mUnmounted = false;
		mScanning = false;
		startWorker();
	}
	
	@Override
	protected void onResume() {
		overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_right);
		super.onResume();
	}

	@Override
	public void onStop() {
		super.onStop();

		abortWorker();

		unregisterReceiver(mReceiver);
		getContentResolver().unregisterContentObserver(mDbObserver);

		// liberamos memória
		mAdapter = null;
		mGridView.setAdapter(null);
		unloadDrawable();
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int position,
			long arg3) {
		launchFolderGallery(position);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			final ContextMenuInfo menuInfo) {
		int position = ((AdapterContextMenuInfo) menuInfo).position;
		menu.setHeaderTitle(mAdapter.baseTitleForPosition(position));
		// "Slide Show"
		if ((mAdapter.getIncludeMediaTypes(position) & ImageManager.INCLUDE_IMAGES) != 0) {
			menu.add(R.string.slide_show).setOnMenuItemClickListener(
					new OnMenuItemClickListener() {
						public boolean onMenuItemClick(MenuItem item) {
							return onSlideShowClicked(menuInfo);
						}
					});
		}
		// "View"
		menu.add(R.string.view).setOnMenuItemClickListener(
				new OnMenuItemClickListener() {
					public boolean onMenuItemClick(MenuItem item) {
						return onViewClicked(menuInfo);
					}
				});
		super.onCreateContextMenu(menu, v, menuInfo);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		// Settings
		menu.add(Menu.NONE, Menu.NONE, MenuHelper.POSITION_GALLERY_SETTING,
				R.string.camerasettings)
				.setOnMenuItemClickListener(new OnMenuItemClickListener() {
					public boolean onMenuItemClick(MenuItem item) {
						Intent preferences = new Intent();
						preferences.setClass(GalleryChooser.this,
								Settings.class);
						startActivity(preferences);
						overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_left);
						return true;
					}
				}).setAlphabeticShortcut('p')
				.setIcon(android.R.drawable.ic_menu_preferences);

		return true;
	}

	@Override
	public boolean onSearchRequested() {
		return false;
	}

	/**
	 * Inicia a thread de carregamento.
	 */
	private void startWorker() {
		mAbort = false;
		mWorkerThread = new Thread("GalleryPicker Worker") {
			@Override
			public void run() {
				workerRun();
			}
		};
		BitmapManager.instance().allowThreadDecoding(mWorkerThread);
		mWorkerThread.start();
	}

	/**
	 * Aborta a thread de carregamento.
	 */
	private void abortWorker() {
		if (mWorkerThread != null) {
			BitmapManager.instance().cancelThreadDecoding(mWorkerThread,
					getContentResolver());
			mAbort = true;
			try {
				mWorkerThread.join();
			} catch (InterruptedException ex) {
				Log.e(TAG, "join interrupted");
			}
			mWorkerThread = null;
			mHandler.removeMessages(0);
			mAdapter.clear();
			mAdapter.updateDisplay();
			clearImageLists();
		}
	}

	/**
	 * Código da thread de carregamento.
	 */
	private void workerRun() {
		ArrayList<Item> allItems = new ArrayList<Item>();

		isScanning();
		if (mAbort)
			return;

		loadImageLists(allItems);
		if (mAbort)
			return;

		loadThumbnails(allItems);
		if (mAbort)
			return;

		checkLowStorage();
	}

	/**
	 * Mostra uma caixa de diálogo se o dispositivo de armazenamento estiver
	 * sendo escaneado.
	 * 
	 * @param scanning
	 */
	public void updateScanningDialog(boolean scanning) {
		boolean prevScanning = (mMediaScanningDialog != null);
		if (prevScanning == scanning && mAdapter.mItems.size() == 0)
			return;
		// Now we are certain the state is changed.
		if (prevScanning) {
			mMediaScanningDialog.cancel();
			mMediaScanningDialog = null;
		} else if (scanning && mAdapter.mItems.size() == 0) {
			mMediaScanningDialog = ProgressDialog.show(this, null,
					getResources().getString(R.string.wait), true, true);
		}
	}

	/**
	 * Mostra o ícone e a mensagem "No media found".
	 */
	private void showNoImagesView() {
		if (mNoImagesView == null) {
			ViewGroup root = (ViewGroup) findViewById(R.id.root);
			getLayoutInflater().inflate(R.layout.gallery_chooser_no_images,
					root);
			mNoImagesView = findViewById(R.id.no_images);
		}
		mNoImagesView.setVisibility(View.VISIBLE);
	}

	/**
	 * Esconde o ícone e a mensagem "No media found".
	 */
	private void hideNoImagesView() {
		if (mNoImagesView != null) {
			mNoImagesView.setVisibility(View.GONE);
		}
	}

	/**
	 * Recarrega o conteúdo, se possível.
	 * 
	 * @param unmounted
	 *            se o dispositivo de armazenamento está montado.
	 * @param scanning
	 *            se o dispositivo de armazenamento está sendo escaneado.
	 */
	private void reloadContent(boolean unmounted, boolean scanning) {
		if (unmounted == mUnmounted && scanning == mScanning)
			return;
		abortWorker();
		mUnmounted = unmounted;
		mScanning = scanning;
		updateScanningDialog(mScanning);
		if (mUnmounted) {
			showNoImagesView();
		} else {
			hideNoImagesView();
			startWorker();
		}
	}

	/**
	 * Chamado quando recebemos broadcasts relacionados com mídia.
	 * 
	 * @param intent
	 *            <code>Intent</code>
	 */
	private void onReceiveMediaBroadcast(Intent intent) {
		String action = intent.getAction();
		if (action.equals(Intent.ACTION_MEDIA_MOUNTED)) {
			// SD card available
			// TODO put up a "please wait" message
		} else if (action.equals(Intent.ACTION_MEDIA_UNMOUNTED)) {
			// SD card unavailable
			reloadContent(true, false);
		} else if (action.equals(Intent.ACTION_MEDIA_SCANNER_STARTED)) {
			reloadContent(false, true);
		} else if (action.equals(Intent.ACTION_MEDIA_SCANNER_FINISHED)) {
			reloadContent(false, false);
		} else if (action.equals(Intent.ACTION_MEDIA_EJECT)) {
			reloadContent(true, false);
		}
	}

	/**
	 * Abre a pasta da galeria.
	 * 
	 * @param position
	 *            posição da lista.
	 */
	private void launchFolderGallery(int position) {
		mAdapter.mItems.get(position).launch(this);
	}

	/**
	 * Chamado quando o botão "Slide Show" do menu de contexto é clicado.
	 * 
	 * @param menuInfo
	 *            informações do menu de contexto.
	 * @return <code>true</code> se não ocorreu nenhum problema.
	 */
	private boolean onSlideShowClicked(ContextMenuInfo menuInfo) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
		int position = info.position;

		if (position < 0 || position >= mAdapter.mItems.size()) {
			return true;
		}
		// o slide show começa da primeira imagem da lista.
		Item item = mAdapter.mItems.get(position);
		Uri targetUri = item.mFirstImageUri;

		if (targetUri != null && item.mBucketId != null) {
			targetUri = targetUri.buildUpon()
					.appendQueryParameter("bucketId", item.mBucketId).build();
		}
		Intent intent = new Intent(Intent.ACTION_VIEW, targetUri);
		intent.putExtra("slideshow", true);
		startActivity(intent);
		overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_left);
		return true;
	}

	/**
	 * Chamado quando o botão "View" do menu de contexto é clicado.
	 * 
	 * @param menuInfo
	 *            informações do menu de contexto.
	 * @return <code>true</code> se não ocorreu nenhum problema.
	 */
	private boolean onViewClicked(ContextMenuInfo menuInfo) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
		launchFolderGallery(info.position);
		return true;
	}

	/**
	 * Checa se o Media Scanner está escaneando.
	 */
	private void isScanning() {
		ContentResolver cr = getContentResolver();
		final boolean scanning = ImageManager.isMediaScannerScanning(cr);
		mHandler.post(new Runnable() {
			public void run() {
				updateScanningDialog(scanning);
			}
		});
	}

	/**
	 * Adiciona um item no nosso GridView, atualizando o mesmo.
	 * 
	 * @param item
	 *            item a ser adicionado.
	 */
	private void addItem(Item item) {
		// escondemos o NoImageView caso estamos adicionando o primeiro item
		if (mAdapter.getCount() == 0) {
			hideNoImagesView();
		}
		mAdapter.addItem(item);
		mAdapter.updateDisplay();
	}

	/**
	 * Carrega todas as listas de imagens do dispositivo
	 * 
	 * @param allItems
	 *            coleção onde as listas de imagens serão guardadas
	 */
	private void loadImageLists(ArrayList<Item> allItems) {
		// primeiro criamos uma lista com todos os itens do dispositivo
		final IImageList allImages;
		if (!mScanning && !mUnmounted) {
			if (this.mMediaType == ImageManager.INCLUDE_IMAGES) {
				allImages = ImageManager.makeImageList(getContentResolver(),
						ImageManager.DataLocation.ALL,
						ImageManager.INCLUDE_IMAGES,
						ImageManager.SORT_DESCENDING, null);
			} else {
				allImages = ImageManager.makeImageList(getContentResolver(),
						ImageManager.DataLocation.ALL,
						ImageManager.INCLUDE_VIDEOS,
						ImageManager.SORT_DESCENDING, null);
			}
		} else {
			allImages = ImageManager.makeEmptyImageList();
		}

		if (mAbort) {
			allImages.close();
			return;
		}

		// a partir da lista com todos os itens, saímos separando estes em
		// listas diferentes, de acordo com sua pasta (BucketId).
		HashMap<String, String> hashMap = allImages.getBucketIds();
		allImages.close();
		if (mAbort)
			return;

		for (Map.Entry<String, String> entry : hashMap.entrySet()) {
			String key = entry.getKey();
			if (key == null) {
				continue;
			}
			IImageList list = createImageList(ImageManager.INCLUDE_IMAGES
					| ImageManager.INCLUDE_VIDEOS, key, getContentResolver());
			if (mAbort)
				return;

			Item item = new Item(Item.TYPE_NORMAL_FOLDERS, key,
					entry.getValue(), list);

			allItems.add(item);

			final Item finalItem = item;
			mHandler.post(new Runnable() {
				public void run() {
					addItem(finalItem);
				}
			});
		}

		mHandler.post(new Runnable() {
			public void run() {
				onCompleteLoadingImageLists();
			}
		});
	}

	/**
	 * Chamado quando o processo de carregamento das listas for completado.
	 */
	private void onCompleteLoadingImageLists() {
		if (!mScanning) {
			// ordenamos os item em ordem alfabética
			Collections.sort(mAdapter.mItems);
			mAdapter.updateDisplay();
			int numItems = mAdapter.mItems.size();
			if (numItems == 0) {
				showNoImagesView();
			} else if (numItems == 1) {
				mAdapter.mItems.get(0).launch(this);
				finish();
				return;
			}
		}
	}

	/**
	 * Carrega as miniaturas das listas de imagens.
	 * 
	 * @param allItems
	 *            listas de imagens.
	 */
	private void loadThumbnails(ArrayList<Item> allItems) {
		for (Item item : allItems) {
			final Bitmap b = makeMiniThumbBitmap(190, THUMB_SIZE,
					item.mImageList);
			if (mAbort) {
				if (b != null)
					b.recycle();
				return;
			}

			final Item finalItem = item;
			mHandler.post(new Runnable() {
				public void run() {
					updateItemThumbnail(finalItem, b);
				}
			});
		}
	}

	/**
	 * Atualiza a miniatura de um item (uma lista).
	 * 
	 * @param item
	 *            item a ser atualizado.
	 * @param b
	 *            <code>Bitmap</code> da miniatura.
	 */
	private void updateItemThumbnail(Item item, Bitmap b) {
		item.setThumbBitmap(b);
		mAdapter.updateDisplay();
	}

	/**
	 * Checa se o dispositivo de armazenamento está com pouco espaço.
	 */
	private void checkLowStorage() {
		if (ImageManager.hasStorage()) {
			String storageDirectory = Environment.getExternalStorageDirectory()
					.toString();
			StatFs stat = new StatFs(storageDirectory);
			long remaining = (long) stat.getAvailableBlocks()
					* (long) stat.getBlockSize();
			if (remaining < LOW_STORAGE_THRESHOLD) {
				mHandler.post(new Runnable() {
					public void run() {
						showLowStorageAlert();
					}
				});
			}
		}
	}

	/**
	 * Mostra uma mensagem alertando que não há espaço suficiente no dispositivo
	 * de armazenamento.
	 */
	private void showLowStorageAlert() {
		Toast.makeText(GalleryChooser.this, R.string.not_enough_space,
				Toast.LENGTH_LONG).show();
	}

	/**
	 * Carrega os Drawables usados na criação das miniaturas dos itens, caso
	 * eles não já estejam carregados.
	 */
	private void loadDrawableIfNeeded() {
		if (mFrameGalleryMask != null)
			return;
		Resources r = getResources();
		mFrameGalleryMask = r
				.getDrawable(R.drawable.frame_gallery_preview_album_mask);
		mVideoOverlay = r.getDrawable(R.drawable.ic_gallery_video_overlay);
	}

	/**
	 * Remove a referência dos Drawables usados na criação das miniaturas dos
	 * itens. Assim, liberamos memória.
	 */
	private void unloadDrawable() {
		mFrameGalleryMask = null;
		mVideoOverlay = null;
	}

	/**
	 * Desenha um Bitmap numa determinada região de um Canvas.
	 */
	private static void placeImage(Bitmap image, Canvas c, Paint paint,
			int imageWidth, int widthPadding, int imageHeight,
			int heightPadding, int offsetX, int offsetY, int pos) {
		int row = pos / 2;
		int col = pos - (row * 2);

		int xPos = (col * (imageWidth + widthPadding)) - offsetX;
		int yPos = (row * (imageHeight + heightPadding)) - offsetY;

		c.drawBitmap(image, xPos, yPos, paint);
	}

	/**
	 * Cria uma miniatura pra uma lista de imagens.
	 * 
	 * @param width
	 *            largura da miniatura.
	 * @param height
	 *            altura da miniatura.
	 * @param images
	 *            lista de imagens.
	 * @return Bitmap da miniatura.
	 */
	private Bitmap makeMiniThumbBitmap(int width, int height, IImageList images) {
		int count = images.getCount();
		final int padding = 1;
		int imageWidth = width;
		int imageHeight = height;
		int offsetWidth = 0;
		int offsetHeight = 0;

		imageWidth = (imageWidth - padding) / 2; // 2 here because we show two
													// images
		imageHeight = (imageHeight - padding) / 2; // per row and column

		final Paint p = new Paint();
		final Bitmap b = Bitmap.createBitmap(width, height,
				Bitmap.Config.ARGB_8888);
		final Canvas c = new Canvas(b);
		final Matrix m = new Matrix();

		// draw the whole canvas as transparent
		p.setColor(0x00000000);
		c.drawPaint(p);

		// load the drawables
		loadDrawableIfNeeded();

		// draw the mask normally
		p.setColor(0xFFFFFFFF);
		mFrameGalleryMask.setBounds(0, 0, width, height);
		mFrameGalleryMask.draw(c);

		Paint pdpaint = new Paint();
		pdpaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));

		pdpaint.setStyle(Paint.Style.FILL);
		// c.drawRect(0, 0, width, height, pdpaint);

		for (int i = 0; i < 4; i++) {
			if (mAbort) {
				return null;
			}

			Bitmap temp = null;
			IImage image = i < count ? images.getImageAt(i) : null;

			if (image != null) {
				temp = image.miniThumbBitmap();
			}

			if (temp != null) {
				if (ImageManager.isVideo(image)) {
					Bitmap newMap = temp.copy(temp.getConfig(), true);
					Canvas overlayCanvas = new Canvas(newMap);
					int overlayWidth = mVideoOverlay.getIntrinsicWidth();
					int overlayHeight = mVideoOverlay.getIntrinsicHeight();
					int left = (newMap.getWidth() - overlayWidth) / 2;
					int top = (newMap.getHeight() - overlayHeight) / 2;
					Rect newBounds = new Rect(left, top, left + overlayWidth,
							top + overlayHeight);
					mVideoOverlay.setBounds(newBounds);
					mVideoOverlay.draw(overlayCanvas);
					temp.recycle();
					temp = newMap;
				}

				temp = Util.transform(m, temp, imageWidth, imageHeight, true,
						Util.RECYCLE_INPUT);
			}

			Bitmap thumb = Bitmap.createBitmap(imageWidth, imageHeight,
					Bitmap.Config.ARGB_8888);
			Canvas tempCanvas = new Canvas(thumb);
			if (temp != null) {
				tempCanvas.drawBitmap(temp, new Matrix(), new Paint());
			}

			placeImage(thumb, c, pdpaint, imageWidth, padding, imageHeight,
					padding, offsetWidth, offsetHeight, i);

			thumb.recycle();

			if (temp != null) {
				temp.recycle();
			}
		}

		return b;
	}

	/**
	 * Cria uma lista de imagens.
	 * 
	 * @param mediaTypes
	 *            tipos de mídia a serem adicionados na lista.
	 * @param bucketId
	 *            id da pasta.
	 * @param cr
	 *            content resolver.
	 * @return lista criada.
	 */
	private IImageList createImageList(int mediaTypes, String bucketId,
			ContentResolver cr) {
		IImageList list = ImageManager.makeImageList(cr,
				ImageManager.DataLocation.ALL, mediaTypes,
				ImageManager.SORT_DESCENDING, bucketId);
		mAllLists.add(list);
		return list;
	}

	/**
	 * Fecha todas as ImageLists, liberando recursos.
	 */
	private void clearImageLists() {
		for (IImageList list : mAllLists) {
			list.close();
		}
		mAllLists.clear();
	}
}

/**
 * Representa uma lista de imagens.
 */
class Item implements Comparable<Item> {

	public static final int TYPE_NONE = -1;
	public static final int TYPE_ALL_IMAGES = 0;
	public static final int TYPE_ALL_VIDEOS = 1;
	public static final int TYPE_CAMERA_IMAGES = 2;
	public static final int TYPE_CAMERA_VIDEOS = 3;
	public static final int TYPE_CAMERA_MEDIAS = 4;
	public static final int TYPE_NORMAL_FOLDERS = 5;

	public final int mType;
	public final String mBucketId;
	public final String mName;
	public final IImageList mImageList;
	public final int mCount;
	public final Uri mFirstImageUri;

	public Bitmap mThumbBitmap;

	public Item(int type, String bucketId, String name, IImageList list) {
		mType = type;
		mBucketId = bucketId;
		mName = name;
		mImageList = list;
		mCount = list.getCount();
		if (mCount > 0) {
			mFirstImageUri = list.getImageAt(0).fullSizeImageUri();
		} else {
			mFirstImageUri = null;
		}
	}

	public void setThumbBitmap(Bitmap thumbBitmap) {
		mThumbBitmap = thumbBitmap;
	}

	public boolean needsBucketId() {
		return mType >= TYPE_CAMERA_IMAGES;
	}

	public void launch(Activity activity) {
		Uri uri = Images.Media.INTERNAL_CONTENT_URI;
		if (needsBucketId()) {
			uri = uri.buildUpon().appendQueryParameter("bucketId", mBucketId)
					.build();
		}
		Intent intent = new Intent(activity.getApplicationContext(),
				Gallery.class);
		intent.setData(uri);
		intent.putExtra("mediaTypes", getIncludeMediaTypes());
		intent.putExtra("title", this.mName);
		activity.startActivity(intent);
		activity.overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_left);
	}

	public int getIncludeMediaTypes() {
		return convertItemTypeToIncludedMediaType(mType);
	}

	public static int convertItemTypeToIncludedMediaType(int itemType) {
		switch (itemType) {
		case TYPE_ALL_IMAGES:
		case TYPE_CAMERA_IMAGES:
			return ImageManager.INCLUDE_IMAGES;
		case TYPE_ALL_VIDEOS:
		case TYPE_CAMERA_VIDEOS:
			return ImageManager.INCLUDE_VIDEOS;
		case TYPE_NORMAL_FOLDERS:
		case TYPE_CAMERA_MEDIAS:
		default:
			return ImageManager.INCLUDE_IMAGES | ImageManager.INCLUDE_VIDEOS;
		}
	}

	@Override
	public int compareTo(Item another) {
		return this.mName.compareTo(another.mName);
	}
}

/**
 * Adapter para a GridView do GalleryChooser.
 */
class GalleryChooserAdapter extends BaseAdapter {
	ArrayList<Item> mItems = new ArrayList<Item>();
	LayoutInflater mInflater;

	GalleryChooserAdapter(LayoutInflater inflater) {
		mInflater = inflater;
	}

	public void addItem(Item item) {
		mItems.add(item);
	}

	public void updateDisplay() {
		notifyDataSetChanged();
	}

	public void clear() {
		mItems.clear();
	}

	public int getCount() {
		return mItems.size();
	}

	public Object getItem(int position) {
		return null;
	}

	public long getItemId(int position) {
		return position;
	}

	public String baseTitleForPosition(int position) {
		return mItems.get(position).mName;
	}

	public int getIncludeMediaTypes(int position) {
		return mItems.get(position).getIncludeMediaTypes();
	}

	public View getView(final int position, View convertView, ViewGroup parent) {
		View v;

		if (convertView == null) {
			v = mInflater.inflate(R.layout.gallery_chooser_item, null);
		} else {
			v = convertView;
		}

		TextView titleView = (TextView) v.findViewById(R.id.title);

		ImageView iv = (ImageView) v
				.findViewById(R.id.imageView1);
		Item item = mItems.get(position);
		if (item.mThumbBitmap != null) {
			iv.setImageBitmap(item.mThumbBitmap);
			String title = item.mName;
			titleView.setText(title);
		} else {
			iv.setImageResource(android.R.color.transparent);
			titleView.setText(item.mName);
		}

		titleView.requestLayout();

		return v;
	}
}
