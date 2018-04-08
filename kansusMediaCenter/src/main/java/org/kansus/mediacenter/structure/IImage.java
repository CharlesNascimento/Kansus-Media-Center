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

import android.graphics.Bitmap;
import android.net.Uri;

import java.io.InputStream;

/**
 * Interface de todas as imagens usadas na aplicação.
 */
public interface IImage {
	static final int THUMBNAIL_TARGET_SIZE = 320;
	static final int MINI_THUMB_TARGET_SIZE = 96;
	static final int THUMBNAIL_MAX_NUM_PIXELS = 512 * 384;
	static final int MINI_THUMB_MAX_NUM_PIXELS = 128 * 128;
	static final int UNCONSTRAINED = -1;

	public static final boolean ROTATE_AS_NEEDED = true;
	public static final boolean NO_ROTATE = false;
	public static final boolean USE_NATIVE = true;
	public static final boolean NO_NATIVE = false;

	/**
	 * Pega a lista de imagens que contém esta imagem.
	 * 
	 * @return A lista ({@link IImageList}) que contém a imagem.
	 */
	public abstract IImageList getContainer();

	/**
	 * Pega o {@link Bitmap} para a imagem de tamanho total.
	 * 
	 * @param minSideLength
	 * @param maxNumberOfPixels
	 *            Número máximo de pixels.
	 * @return O {@link Bitmap} da imagem em tamanho real.
	 */
	public abstract Bitmap fullSizeBitmap(int minSideLength,
			int maxNumberOfPixels);

	/**
	 * Pega o {@link Bitmap} para a imagem de tamanho total.
	 * 
	 * @param minSideLength
	 * @param maxNumberOfPixels
	 *            Número máximo de pixels.
	 * @param rotateAsNeeded
	 *            Se é para girar a imagem se for preciso.
	 * @param useNative
	 * @return O {@link Bitmap} da imagem em tamanho real.
	 */
	public abstract Bitmap fullSizeBitmap(int minSideLength,
			int maxNumberOfPixels, boolean rotateAsNeeded, boolean useNative);

	/**
	 * Pega o grau de rotação desta imagem.
	 * 
	 * @return Um <code>int</code> contendo grau de rotação da imagem.
	 */
	public abstract int getDegreesRotated();

	/**
	 * Pega o Input Stream associado à imagem de tamanho real.
	 * 
	 * @return O {@link InputStream} da imagem em tamanho real.
	 */
	public abstract InputStream fullSizeImageData();

	/**
	 * Pega o endereço da imagem de tamanho real.
	 * 
	 * @return O endereço {@link Uri} da imagem em tamanho real.
	 */
	public abstract Uri fullSizeImageUri();

	/**
	 * Pega o caminho dos dados da imagem (Tamanho real)
	 * 
	 * @return Uma <code>String</code> com os dados da imagem.
	 */
	public abstract String getDataPath();

	/**
	 * Pega o título da imagem.
	 * 
	 * @return Uma <code>String</code> contendo o título da imagem.
	 */
	public abstract String getTitle();

	/**
	 * Pega a data em que a imagem foi criada (tirada).
	 * 
	 * @return Um <code>long</code> contendo a data.
	 */
	public abstract long getDateTaken();

	/**
	 * Pega o MimeType da imagem.
	 * 
	 * @return Uma <code>String</code> contendo o MimeType da imagem.
	 */
	public abstract String getMimeType();

	/**
	 * Pega a largura da imagem em pixels.
	 * 
	 * @return Um <code>int</code> contendo a largura da imagem.
	 */
	public abstract int getWidth();

	/**
	 * Pega a altura da imagem em pixels.
	 * 
	 * @return Um <code>int</code> contendo a altura da imagem.
	 */
	public abstract int getHeight();

	/**
	 * Informa se a imagem é somente leitura.
	 * 
	 * @return <code>true</code> se a imagem for somente leitura.
	 */
	public abstract boolean isReadonly();

	/**
	 * Informa se a imagem é DRM.
	 * 
	 * @return <code>true</code> se a imagem for DRM.
	 */
	public abstract boolean isDrm();

	/**
	 * Pega o {@link Bitmap} da miniatura média.
	 * 
	 * @param rotateAsNeeded
	 *            Se é pra girar quando preciso.
	 * @return O {@link Bitmap} da miniatura média.
	 */
	public abstract Bitmap thumbBitmap(boolean rotateAsNeeded);

	/**
	 * Pega o {@link Bitmap} da miniatura pequena.
	 * 
	 * @return O {@link Bitmap} da miniatura pequena.
	 */
	public abstract Bitmap miniThumbBitmap();

	/**
	 * Rotaciona a imagem.
	 * 
	 * @param degrees
	 *            Quantos graus devem ser rotacionados.
	 * @return <code>true</code> se a operação foi efetuada com sucesso.
	 */
	public abstract boolean rotateImageBy(int degrees);

}
