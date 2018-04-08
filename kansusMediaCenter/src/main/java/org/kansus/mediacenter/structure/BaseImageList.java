/*
 * Copyright (C) 2009 The Android Open Source Project
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

package org.kansus.mediacenter.structure;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.kansus.mediacenter.util.ImageManager;
import org.kansus.mediacenter.util.Util;


import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

/**
 * Representa uma coleção de <code>BaseImage</code>s.
 */
public abstract class BaseImageList implements IImageList {
	private static final String TAG = "BaseImageList";
	private static final int CACHE_CAPACITY = 512;
	private final LruCache<Integer, BaseImage> mCache = new LruCache<Integer, BaseImage>(
			CACHE_CAPACITY);

	protected ContentResolver mContentResolver;
	protected int mSort;

	protected Uri mBaseUri;
	protected Cursor mCursor;
	protected String mBucketId;
	protected boolean mCursorDeactivated = false;

	/**
	 * Construtor.
	 * @param resolver Content Resolver.
	 * @param imageUri Endereço das imagens.
	 * @param sort Sentido da ondenação.
	 * @param bucketId Bucket ID da imagem.
	 */
	public BaseImageList(ContentResolver resolver, Uri uri, int sort,
			String bucketId) {
		mSort = sort;
		mBaseUri = uri;
		mBucketId = bucketId;
		mContentResolver = resolver;
		mCursor = createCursor();

		if (mCursor == null) {
			Log.w(TAG, "createCursor returns null.");
		}

		// TODO: We need to clear the cache because we may "reopen" the image
		// list. After we implement the image list state, we can remove this
		// kind of usage.
		mCache.clear();
	}

	public void close() {
		try {
			invalidateCursor();
		} catch (IllegalStateException e) {
			// IllegalStateException may be thrown if the cursor is stale.
			Log.e(TAG, "Caught exception while deactivating cursor.", e);
		}
		mContentResolver = null;
		if (mCursor != null) {
			mCursor.close();
			mCursor = null;
		}
	}

	/**
	 * Pega o endereço dos conteúdos da lista.
	 * 
	 * @param id
	 *            Id do caminho.
	 * @return Um {@link Uri} com o endereço.
	 */
	protected Uri contentUri(long id) {
		// TODO: avoid using exception for most cases
		try {
			// does our uri already have an id (single image query)?
			// if so just return it
			long existingId = ContentUris.parseId(mBaseUri);
			if (existingId != id)
				Log.e(TAG, "id mismatch");
			return mBaseUri;
		} catch (NumberFormatException ex) {
			// otherwise tack on the id
			return ContentUris.withAppendedId(mBaseUri, id);
		}
	}
	
	public String getBucketId() {
		return this.mBucketId;
	}

	public int getCount() {
		Cursor cursor = getCursor();
		if (cursor == null)
			return 0;
		synchronized (this) {
			return cursor.getCount();
		}
	}

	public boolean isEmpty() {
		return getCount() == 0;
	}

	/**
	 * Pega o cursor desta lista.
	 * 
	 * @return O {@link Cursor} desta lista.
	 */
	private Cursor getCursor() {
		synchronized (this) {
			if (mCursor == null)
				return null;
			if (mCursorDeactivated) {
				mCursor = createCursor();
				mCursorDeactivated = false;
			}
			return mCursor;
		}
	}

	public IImage getImageAt(int i) {
		BaseImage result = mCache.get(i);
		if (result == null) {
			Cursor cursor = getCursor();
			if (cursor == null)
				return null;
			synchronized (this) {
				result = cursor.moveToPosition(i) ? loadImageFromCursor(cursor)
						: null;
				mCache.put(i, result);
			}
		}
		return result;
	}

	public boolean removeImage(IImage image) {
		// TODO: need to delete the thumbnails as well
		if (mContentResolver.delete(image.fullSizeImageUri(), null, null) > 0) {
			((BaseImage) image).onRemove();
			invalidateCursor();
			invalidateCache();
			return true;
		} else {
			return false;
		}
	}

	public boolean removeImageAt(int i) {
		// TODO: need to delete the thumbnails as well
		return removeImage(getImageAt(i));
	}

	/**
	 * Cria um cursor apontando para esta lista.
	 * 
	 * @return Um {@link Cursor} referente à esta lista.
	 */
	protected abstract Cursor createCursor();

	/**
	 * Carrega uma imagem a partir de um {@link Cursor}.
	 * 
	 * @param cursor
	 *            O {@link Cursor} apontando pra imagem.
	 * @return A imagem carregada.
	 */
	protected abstract BaseImage loadImageFromCursor(Cursor cursor);

	/**
	 * Pega o ID da imagem sendo apontada por um {@link Cursor}.
	 * 
	 * @param cursor
	 *            O {@link Cursor} apontando pra imagem.
	 * @return Um <code>long</code> contendo o ID da imagem.
	 */
	protected abstract long getImageId(Cursor cursor);

	/**
	 * Desativa um {@link Cursor}.
	 */
	protected void invalidateCursor() {
		if (mCursor == null)
			return;
		mCursor.deactivate();
		mCursorDeactivated = true;
	}

	/**
	 * Limpa a memória cache desta lista.
	 */
	protected void invalidateCache() {
		mCache.clear();
	}

	private static final Pattern sPathWithId = Pattern.compile("(.*)/\\d+");

	/**
	 * Pega o caminho de um {@link Uri} sem o ID.
	 * @param uri {@link Uri}.
	 * @return Uma <code>String</code> com o caminho sem o id.
	 */
	private static String getPathWithoutId(Uri uri) {
		String path = uri.getPath();
		Matcher matcher = sPathWithId.matcher(path);
		return matcher.matches() ? matcher.group(1) : path;
	}

	/**
	 * Informa se uma imagem é filha desta lista.
	 * @param uri {@link Uri} da imagem.
	 * @return
	 */
	private boolean isChildImageUri(Uri uri) {
		// Sometimes, the URI of an image contains a query string with key
		// "bucketId" in order to restore the image list. However, the query
		// string is not part of the mBaseUri. So, we check only other parts
		// of the two Uri to see if they are the same.
		Uri base = mBaseUri;
		return Util.equals(base.getScheme(), uri.getScheme())
				&& Util.equals(base.getHost(), uri.getHost())
				&& Util.equals(base.getAuthority(), uri.getAuthority())
				&& Util.equals(base.getPath(), getPathWithoutId(uri));
	}

	public IImage getImageForUri(Uri uri) {
		if (!isChildImageUri(uri))
			return null;
		// Find the id of the input URI.
		long matchId;
		try {
			matchId = ContentUris.parseId(uri);
		} catch (NumberFormatException ex) {
			Log.i(TAG, "fail to get id in: " + uri, ex);
			return null;
		}
		// TODO: design a better method to get URI of specified ID
		Cursor cursor = getCursor();
		if (cursor == null)
			return null;
		synchronized (this) {
			cursor.moveToPosition(-1); // before first
			for (int i = 0; cursor.moveToNext(); ++i) {
				if (getImageId(cursor) == matchId) {
					BaseImage image = mCache.get(i);
					if (image == null) {
						image = loadImageFromCursor(cursor);
						mCache.put(i, image);
					}
					return image;
				}
			}
			return null;
		}
	}

	public int getImageIndex(IImage image) {
		return ((BaseImage) image).mIndex;
	}

	/**
	 * Provém um ordenamento padrão para as subclasses. A lista é primeiramente
	 * ordenada pela data, e depois pelo ID. O ordenamento pode ser em ordem
	 * crescente ou decrescente, dependendo da variável mSort. A data é obtida
	 * através da coluna "datetaken". Porém se seu valor for null, a coluna
	 * "date_modified" é usada em seu lugar.
	 * 
	 * @return Uma <code>String</code> representando o ordenamento.
	 */
	protected String sortOrder() {
		String ascending = (mSort == ImageManager.SORT_ASCENDING) ? " ASC"
				: " DESC";

		// Use DATE_TAKEN if it's non-null, otherwise use DATE_MODIFIED.
		// DATE_TAKEN is in milliseconds, but DATE_MODIFIED is in seconds.
		String dateExpr = "case ifnull(datetaken,0)"
				+ " when 0 then date_modified*1000" + " else datetaken"
				+ " end";

		// Add id to the end so that we don't ever get random sorting
		// which could happen, I suppose, if the date values are the same.
		return dateExpr + ascending + ", _id" + ascending;
	}
}
