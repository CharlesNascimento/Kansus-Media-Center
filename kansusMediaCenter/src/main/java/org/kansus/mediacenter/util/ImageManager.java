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

package org.kansus.mediacenter.util;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.location.Location;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.kansus.mediacenter.structure.BaseImageList;
import org.kansus.mediacenter.structure.DrmImageList;
import org.kansus.mediacenter.structure.IImage;
import org.kansus.mediacenter.structure.IImageList;
import org.kansus.mediacenter.structure.ImageList;
import org.kansus.mediacenter.structure.ImageListUber;
import org.kansus.mediacenter.structure.SingleImageList;
import org.kansus.mediacenter.structure.VideoList;
import org.kansus.mediacenter.structure.VideoObject;

/**
 * ImageManager é usado para recuperar e armazenar imagens no Content Provider
 * de mídia.
 */
public class ImageManager {
	private static final String TAG = "ImageManager";

	// Caminho do dispositivo de armazenamento externo. (SD Card)
	private static final Uri STORAGE_URI = Images.Media.EXTERNAL_CONTENT_URI;
	// private static final Uri THUMB_URI =
	// Images.Thumbnails.EXTERNAL_CONTENT_URI;

	// Local onde videos são armazenados.
	private static final Uri VIDEO_STORAGE_URI = Uri
			.parse("content://media/external/video/media");

	/**
	 * Especifica todos os parametros que nós precisamos para criar uma lista de
	 * imagens (Também precisamos de um ContentResolver).
	 */
	public static class ImageListParam implements Parcelable {
		public DataLocation mLocation;
		public int mInclusion;
		public int mSort;
		public String mBucketId;

		// This is only used if we are creating a single image list.
		public Uri mSingleImageUri;

		// This is only used if we are creating an empty image list.
		public boolean mIsEmptyImageList;

		public ImageListParam() {
		}

		public void writeToParcel(Parcel out, int flags) {
			out.writeInt(mLocation.ordinal());
			out.writeInt(mInclusion);
			out.writeInt(mSort);
			out.writeString(mBucketId);
			out.writeParcelable(mSingleImageUri, flags);
			out.writeInt(mIsEmptyImageList ? 1 : 0);
		}

		private ImageListParam(Parcel in) {
			mLocation = DataLocation.values()[in.readInt()];
			mInclusion = in.readInt();
			mSort = in.readInt();
			mBucketId = in.readString();
			mSingleImageUri = in.readParcelable(null);
			mIsEmptyImageList = (in.readInt() != 0);
		}

		public String toString() {
			return String.format("ImageListParam{loc=%s,inc=%d,sort=%d,"
					+ "bucket=%s,empty=%b,single=%s}", mLocation, mInclusion,
					mSort, mBucketId, mIsEmptyImageList, mSingleImageUri);
		}

		@SuppressWarnings("rawtypes")
		public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
			public ImageListParam createFromParcel(Parcel in) {
				return new ImageListParam(in);
			}

			public ImageListParam[] newArray(int size) {
				return new ImageListParam[size];
			}
		};

		public int describeContents() {
			return 0;
		}
	}

	// Location
	public static enum DataLocation {
		NONE, INTERNAL, EXTERNAL, ALL
	}

	// Inclusion
	public static final int INCLUDE_IMAGES = (1 << 0);
	public static final int INCLUDE_DRM_IMAGES = (1 << 1);
	public static final int INCLUDE_VIDEOS = (1 << 2);

	// Sort
	public static final int SORT_ASCENDING = 1;
	public static final int SORT_DESCENDING = 2;

	// Caminho onde as imagens criadas pela câmera do dispositivo são
	// armazenadas.
	public static final String CAMERA_IMAGE_BUCKET_NAME = Environment
			.getExternalStorageDirectory().toString() + "/DCIM/Camera";
	public static final String CAMERA_IMAGE_BUCKET_ID = getBucketId(CAMERA_IMAGE_BUCKET_NAME);

	/**
	 * Calcula o ID de um caminho através de uma função hash.
	 * 
	 * @param path
	 *            Caminho.
	 * @return O ID do caminho.
	 */
	public static String getBucketId(String path) {
		return String.valueOf(path.toLowerCase().hashCode());
	}

	/**
	 * OSX requires plugged-in USB storage to have path /DCIM/NNNAAAAA to be
	 * imported. This is a temporary fix for bug#1655552.
	 */
	public static void ensureOSXCompatibleFolder() {
		File nnnAAAAA = new File(Environment.getExternalStorageDirectory()
				.toString() + "/DCIM/100ANDRO");
		if ((!nnnAAAAA.exists()) && (!nnnAAAAA.mkdir())) {
			Log.e(TAG, "create NNNAAAAA file: " + nnnAAAAA.getPath()
					+ " failed");
		}
	}

	/**
	 * Checa se um determinado Mime Type é de uma imagem.
	 * 
	 * @param mimeType
	 *            Mime Type.
	 * @return true se o Mime Type for de uma imagem.
	 */
	public static boolean isImageMimeType(String mimeType) {
		return mimeType.startsWith("image/");
	}

	/*
	 * This is commented out because isVideo is not calling this now. public
	 * static boolean isVideoMimeType(String mimeType) { return
	 * mimeType.startsWith("video/"); }
	 */

	/**
	 * Checa se uma imagem é uma imagem
	 * 
	 * @return true se a imagem é uma imagem.
	 */
	public static boolean isImage(IImage image) {
		return isImageMimeType(image.getMimeType());
	}

	/**
	 * Checa se uma imagem é um video
	 * 
	 * @return true se a imagem é um video.
	 */
	public static boolean isVideo(IImage image) {
		// This is the right implementation, but we use instanceof for speed.
		// return isVideoMimeType(image.getMimeType());
		return (image instanceof VideoObject);
	}

	/**
	 * Armazena um arranjo de bytes jpeg ou bitmap em um arquivo (usando o
	 * caminho e nome do arquivo especificados). Também armazena uma entrada no
	 * armazenamento de midia para esta imagem. O título, data tirada,
	 * localização são atributos da imagem. O degree é um array de um elemento
	 * só que retorna a orientação da imagem.
	 * 
	 * @param cr
	 *            Content Resolver.
	 * @param title
	 *            Título da imagem.
	 * @param dateTaken
	 *            Data em que a imagem foi tirada.
	 * @param location
	 *            Onda a imagem foi tirada.
	 * @param directory
	 *            Diretório da imagem.
	 * @param filename
	 *            Nome do arquivo de imagem.
	 * @param source
	 *            Arranjo de bytes bitmap
	 * @param jpegData
	 *            Arranjo de bytes jpeg
	 * @param degree
	 *            Orientação da imagem.
	 * @return Endereço da imagem criada.
	 */
	public static Uri addImage(ContentResolver cr, String title,
			long dateTaken, Location location, String directory,
			String filename, Bitmap source, byte[] jpegData, int[] degree) {
		// We should store image data earlier than insert it to ContentProvider,
		// otherwise
		// we may not be able to generate thumbnail in time.
		OutputStream outputStream = null;
		String filePath = directory + "/" + filename;
		try {
			File dir = new File(directory);
			if (!dir.exists())
				dir.mkdirs();
			File file = new File(directory, filename);
			outputStream = new FileOutputStream(file);
			if (source != null) {
				source.compress(CompressFormat.JPEG, 75, outputStream);
				degree[0] = 0;
			} else {
				outputStream.write(jpegData);
				degree[0] = getExifOrientation(filePath);
			}
		} catch (FileNotFoundException ex) {
			Log.w(TAG, ex);
			return null;
		} catch (IOException ex) {
			Log.w(TAG, ex);
			return null;
		} finally {
			Util.closeSilently(outputStream);
		}

		ContentValues values = new ContentValues(7);
		values.put(Images.Media.TITLE, title);

		// That filename is what will be handed to Gmail when a user shares a
		// photo. Gmail gets the name of the picture attachment from the
		// "DISPLAY_NAME" field.
		values.put(Images.Media.DISPLAY_NAME, filename);
		values.put(Images.Media.DATE_TAKEN, dateTaken);
		values.put(Images.Media.MIME_TYPE, "image/jpeg");
		values.put(Images.Media.ORIENTATION, degree[0]);
		values.put(Images.Media.DATA, filePath);

		/*
		 * if (location != null) { values.put(Images.Media.LATITUDE,
		 * location.getLatitude()); values.put(Images.Media.LONGITUDE,
		 * location.getLongitude()); }
		 */

		return cr.insert(STORAGE_URI, values);
	}

	/**
	 * Descobre qual a orientação de uma imagem.
	 * 
	 * @param filepath
	 *            Caminho da imagem.
	 * @return A orientação da imagem.
	 */
	public static int getExifOrientation(String filepath) {
		int degree = 0;
		ExifInterface exif = null;
		try {
			exif = new ExifInterface(filepath);
		} catch (IOException ex) {
			Log.e(TAG, "cannot read exif", ex);
		}
		if (exif != null) {
			int orientation = exif.getAttributeInt(
					ExifInterface.TAG_ORIENTATION, -1);
			if (orientation != -1) {
				// We only recognize a subset of orientation tag values.
				switch (orientation) {
				case ExifInterface.ORIENTATION_ROTATE_90:
					degree = 90;
					break;
				case ExifInterface.ORIENTATION_ROTATE_180:
					degree = 180;
					break;
				case ExifInterface.ORIENTATION_ROTATE_270:
					degree = 270;
					break;
				}

			}
		}
		return degree;
	}

	/**
	 * Cria uma Lista de imagens.
	 * 
	 * @param cr
	 *            Content Resolver.
	 * @param param
	 *            Parâmetros da lista de imagens.
	 * @return A lista de imagens {@link IImageList} criada.
	 */
	public static IImageList makeImageList(ContentResolver cr,
			ImageListParam param) {
		DataLocation location = param.mLocation;
		int inclusion = param.mInclusion;
		int sort = param.mSort;
		String bucketId = param.mBucketId;
		Uri singleImageUri = param.mSingleImageUri;
		boolean isEmptyImageList = param.mIsEmptyImageList;

		if (isEmptyImageList || cr == null) {
			return new EmptyImageList();
		}

		if (singleImageUri != null) {
			return new SingleImageList(cr, singleImageUri);
		}

		// false ==> don't require write access
		boolean haveSdCard = hasStorage(false);

		// use this code to merge videos and stills into the same list
		ArrayList<BaseImageList> l = new ArrayList<BaseImageList>();

		if (haveSdCard && location != DataLocation.INTERNAL) {
			if ((inclusion & INCLUDE_IMAGES) != 0) {
				l.add(new ImageList(cr, STORAGE_URI, sort, bucketId));
			}
			if ((inclusion & INCLUDE_VIDEOS) != 0) {
				l.add(new VideoList(cr, VIDEO_STORAGE_URI, sort, bucketId));
			}
		}
		if (location == DataLocation.INTERNAL || location == DataLocation.ALL) {
			if ((inclusion & INCLUDE_IMAGES) != 0) {
				l.add(new ImageList(cr, Images.Media.INTERNAL_CONTENT_URI,
						sort, bucketId));
			}
			if ((inclusion & INCLUDE_DRM_IMAGES) != 0) {
				l.add(new DrmImageList(cr,
						MediaStore.Images.Media.EXTERNAL_CONTENT_URI, sort,
						bucketId));
			}
		}

		// Optimization: If some of the lists are empty, remove them.
		// If there is only one remaining list, return it directly.
		Iterator<BaseImageList> iter = l.iterator();
		while (iter.hasNext()) {
			BaseImageList sublist = iter.next();
			if (sublist.isEmpty()) {
				sublist.close();
				iter.remove();
			}
		}

		if (l.size() == 1) {
			BaseImageList list = l.get(0);
			return list;
		}

		ImageListUber uber = new ImageListUber(l.toArray(new IImageList[l
				.size()]), sort);
		return uber;
	}

	/**
	 * Cria uma lista de imagem a partir de um endereço.
	 * 
	 * @param cr
	 *            Content Resolver.
	 * @param uri
	 *            Endereço.
	 * @param sort
	 *            Ordenação.
	 * @return A lista de imagens {@link IImageList} criada.
	 */
	public static IImageList makeImageList(ContentResolver cr, Uri uri, int sort) {
		String uriString = (uri != null) ? uri.toString() : "";

		// TODO: we need to figure out whether we're viewing
		// DRM images in a better way. Is there a constant
		// for content://drm somewhere??

		if (uriString.startsWith("content://drm")) {
			return makeImageList(cr, DataLocation.ALL, INCLUDE_DRM_IMAGES,
					sort, null);
		} else if (uriString.startsWith("content://media/external/video")) {
			return makeImageList(cr, DataLocation.EXTERNAL, INCLUDE_VIDEOS,
					sort, null);
		} else if (isSingleImageMode(uriString)) {
			return makeSingleImageList(cr, uri);
		} else {
			String bucketId = uri.getQueryParameter("bucketId");
			return makeImageList(cr, DataLocation.ALL, INCLUDE_IMAGES, sort,
					bucketId);
		}
	}

	/**
	 * Checa se uma imagem é avulsa, ou seja, não está nos locais padrões para
	 * armazenamento de imagem.
	 * 
	 * @param uriString
	 *            Endereço.
	 * @return true se a imagem é avulsa.
	 */
	public static boolean isSingleImageMode(String uriString) {
		return !uriString
				.startsWith(MediaStore.Images.Media.EXTERNAL_CONTENT_URI
						.toString())
				&& !uriString
						.startsWith(MediaStore.Images.Media.INTERNAL_CONTENT_URI
								.toString());
	}

	/**
	 * Cria um {@link ImageListParam} a partir dos parâmetros passados.
	 * 
	 * @param location
	 *            Localização das imagens.
	 * @param inclusion
	 *            Quais tipos de imagens serão incluidos na lista.
	 * @param sort
	 *            Ordenação da lista.
	 * @param bucketId
	 *            Id do endereço.
	 * @return Os parâmetros em forma de {@link ImageListParam}.
	 */
	public static ImageListParam getImageListParam(DataLocation location,
			int inclusion, int sort, String bucketId) {
		ImageListParam param = new ImageListParam();
		param.mLocation = location;
		param.mInclusion = inclusion;
		param.mSort = sort;
		param.mBucketId = bucketId;
		return param;
	}

	/**
	 * Cria um {@link ImageListParam} para uma lista de uma imagem apenas (
	 * {@link SingleImageList}).
	 * 
	 * @param Endereço
	 *            da imagem.
	 * @return Os parâmetros em forma de {@link ImageListParam}.
	 */
	public static ImageListParam getSingleImageListParam(Uri uri) {
		ImageListParam param = new ImageListParam();
		param.mSingleImageUri = uri;
		return param;
	}

	/**
	 * Cria um {@link ImageListParam} para uma lista vazia (
	 * {@link EmptyImageList}).
	 * 
	 * @return Os parâmetros em forma de {@link ImageListParam}.
	 */
	public static ImageListParam getEmptyImageListParam() {
		ImageListParam param = new ImageListParam();
		param.mIsEmptyImageList = true;
		return param;
	}

	/**
	 * @param cr
	 *            Content Resolver.
	 * @param location
	 *            Localização das imagens.
	 * @param inclusion
	 *            Quais tipos de imagem devem ser adicionados à lista.
	 * @param sort
	 *            Ordenação da lista.
	 * @param bucketId
	 *            ID do caminho.
	 * @return A lista de imagens ({@link IImageList}) criada.
	 */
	public static IImageList makeImageList(ContentResolver cr,
			DataLocation location, int inclusion, int sort, String bucketId) {
		ImageListParam param = getImageListParam(location, inclusion, sort,
				bucketId);
		return makeImageList(cr, param);
	}

	/**
	 * Cria uma lista de imagens ({@link IImageList}) vazia.
	 * 
	 * @return A lista de imagens ({@link IImageList}) criada.
	 */
	public static IImageList makeEmptyImageList() {
		return makeImageList(null, getEmptyImageListParam());
	}

	/**
	 * Cria uma lista de imagens ({@link IImageList}) com apenas uma imagem.
	 * 
	 * @param cr
	 *            Content Resolver.
	 * @param uri
	 *            Endereço da imagem.
	 * @return A lista de imagens ({@link IImageList}) criada.
	 */
	public static IImageList makeSingleImageList(ContentResolver cr, Uri uri) {
		return makeImageList(cr, getSingleImageListParam(uri));
	}

	/**
	 * Método que checa se um dispositivo de armazenamento é writable.
	 * 
	 * @return true se for writable.
	 */
	private static boolean checkFsWritable() {
		// Create a temporary file to see whether a volume is really writeable.
		// It's important not to put it in the root directory which may have a
		// limit on the number of files.
		String directoryName = Environment.getExternalStorageDirectory()
				.toString() + "/DCIM";
		File directory = new File(directoryName);
		if (!directory.isDirectory()) {
			if (!directory.mkdirs()) {
				return false;
			}
		}
		File f = new File(directoryName, ".probe");
		try {
			// Remove stale file if any
			if (f.exists()) {
				f.delete();
			}
			if (!f.createNewFile()) {
				return false;
			}
			f.delete();
			return true;
		} catch (IOException ex) {
			return false;
		}
	}

	/**
	 * Checa se existe um dispositivo de armazenamento externo writable no
	 * aparelho.
	 * 
	 * @return true se existe um dispositivo de armazenamento externo.
	 */
	public static boolean hasStorage() {
		return hasStorage(true);
	}

	/**
	 * Checa se existe um dispositivo de armazenamento no aparelho.
	 * 
	 * @param requireWriteAccess
	 *            Se é necessário escrever algo nele.
	 * @return true se existe um dispositivo de armazenamento externo.
	 */
	public static boolean hasStorage(boolean requireWriteAccess) {
		String state = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED.equals(state)) {
			if (requireWriteAccess) {
				boolean writable = checkFsWritable();
				return writable;
			} else {
				return true;
			}
		} else if (!requireWriteAccess
				&& Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
			return true;
		}
		return false;
	}

	/**
	 * Efetua uma consulta em um endereço
	 * 
	 * @param resolver
	 *            Content Resolver.
	 * @param uri
	 *            Endereço a ser consultado.
	 * @param projection
	 *            Projeção.
	 * @param selection
	 *            Seleção.
	 * @param selectionArgs
	 *            Argumentos da seleção.
	 * @param sortOrder
	 *            Tipo de ordenação.
	 * @return um cursor apontando para a localidade.
	 */
	private static Cursor query(ContentResolver resolver, Uri uri,
			String[] projection, String selection, String[] selectionArgs,
			String sortOrder) {
		try {
			if (resolver == null) {
				return null;
			}
			return resolver.query(uri, projection, selection, selectionArgs,
					sortOrder);
		} catch (UnsupportedOperationException ex) {
			return null;
		}

	}

	/**
	 * Checa se o Media Scanner está escaneando.
	 * 
	 * @param cr
	 *            Content Resolver.
	 * @return true se o Media Scanner estiver escaneando.
	 */
	public static boolean isMediaScannerScanning(ContentResolver cr) {
		boolean result = false;
		Cursor cursor = query(cr, MediaStore.getMediaScannerUri(),
				new String[] { MediaStore.MEDIA_SCANNER_VOLUME }, null, null,
				null);
		if (cursor != null) {
			if (cursor.getCount() == 1) {
				cursor.moveToFirst();
				result = "external".equals(cursor.getString(0));
			}
			cursor.close();
		}

		return result;
	}

	/**
	 * Representa uma lista de imagens ({@link ImageList}) vazia.
	 */
	private static class EmptyImageList implements IImageList {
		public void close() {
		}

		public HashMap<String, String> getBucketIds() {
			return new HashMap<String, String>();
		}

		public int getCount() {
			return 0;
		}

		public boolean isEmpty() {
			return true;
		}

		public IImage getImageAt(int i) {
			return null;
		}

		public IImage getImageForUri(Uri uri) {
			return null;
		}

		public boolean removeImage(IImage image) {
			return false;
		}

		public boolean removeImageAt(int i) {
			return false;
		}

		public int getImageIndex(IImage image) {
			throw new UnsupportedOperationException();
		}
	}
}
