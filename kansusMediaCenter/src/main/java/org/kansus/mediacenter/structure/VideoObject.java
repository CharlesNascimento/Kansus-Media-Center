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

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Video;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;

import org.kansus.mediacenter.util.BitmapManager;


/**
 * Representa um vídeo particular e provém acesso ao seu conteúdo, assim como
 * aos Bitmaps de duas miniaturas e à informações tais como id e caminho dos
 * dados do vídeo em si.
 */
public class VideoObject extends BaseImage implements IImage {
	private static final String TAG = "VideoObject";

	/**
	 * Construtor.
	 * @param container Lista à qual o vídeo pertence.
	 * @param cr Content Resolver.
	 * @param id ID do vídeo.
	 * @param index Índice do vídeo na lista.
	 * @param uri Endereço do vídeo.
	 * @param dataPath Caminho dos dados do vídeo.
	 * @param mimeType Mime Type do vídeo.
	 * @param dateTaken Data em que o vídeo foi criado (gravado).
	 * @param title Título do vídeo.
	 */
	protected VideoObject(BaseImageList container, ContentResolver cr, long id,
			int index, Uri uri, String dataPath, String mimeType,
			long dateTaken, String title) {
		super(container, cr, id, index, uri, dataPath, mimeType, dateTaken,
				title);
	}

	@Override
	public boolean equals(Object other) {
		if (other == null || !(other instanceof VideoObject))
			return false;
		return fullSizeImageUri().equals(
				((VideoObject) other).fullSizeImageUri());
	}

	@Override
	public int hashCode() {
		return fullSizeImageUri().toString().hashCode();
	}

	@Override
	public Bitmap fullSizeBitmap(int minSideLength, int maxNumberOfPixels,
			boolean rotateAsNeeded, boolean useNative) {
		return ThumbnailUtils.createVideoThumbnail(mDataPath,
				Video.Thumbnails.MINI_KIND);
	}

	@Override
	public InputStream fullSizeImageData() {
		try {
			InputStream input = mContentResolver
					.openInputStream(fullSizeImageUri());
			return input;
		} catch (IOException ex) {
			return null;
		}
	}

	@Override
	public int getHeight() {
		return 0;
	}

	@Override
	public int getWidth() {
		return 0;
	}

	public boolean isReadonly() {
		return false;
	}

	public boolean isDrm() {
		return false;
	}

	public boolean rotateImageBy(int degrees) {
		return false;
	}

	public Bitmap thumbBitmap(boolean rotateAsNeeded) {
		return fullSizeBitmap(THUMBNAIL_TARGET_SIZE, THUMBNAIL_MAX_NUM_PIXELS);
	}

	@Override
	public Bitmap miniThumbBitmap() {
		try {
			long id = mId;
			return BitmapManager.instance().getThumbnail(mContentResolver, id,
					Images.Thumbnails.MICRO_KIND, null, true);
		} catch (Throwable ex) {
			Log.e(TAG, "miniThumbBitmap got exception", ex);
			return null;
		}
	}

	@Override
	public String toString() {
		return new StringBuilder("VideoObject").append(mId).toString();
	}
}
