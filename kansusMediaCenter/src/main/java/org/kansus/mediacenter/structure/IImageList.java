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

import android.net.Uri;

import java.util.HashMap;

// ImageList and Image classes have one-to-one correspondence.
// The class hierarchy (* = abstract class):
//
//    IImageList
//    - BaseImageList (*)
//      - VideoList
//      - ImageList
//        - DrmImageList
//    - SingleImageList (contains UriImage)
//    - ImageListUber
//
//    IImage
//    - BaseImage (*)
//      - VideoObject
//      - Image
//        - DrmImage
//    - UriImage

/**
 * A interface de todas as coleções de imagens usadas na aplicação.
 */
public interface IImageList {

	/**
	 * Pega os Bucket IDs das imagens desta lista.
	 * 
	 * @return Um <code>HashMap</code> contendo o par <code>BUCKET_ID</code> e
	 *         <code>BUCKET_DISPLAY_NAME</code> de todas as imagens da lista.
	 */
	public HashMap<String, String> getBucketIds();

	/**
	 * Pega o número de imagens na lista.
	 * 
	 * @return O número de imagens contidas na lista.
	 */
	public int getCount();

	/**
	 * Informa se a lista está vazia.
	 * 
	 * @return <code>true</code> se a lista está vazia.
	 */
	public boolean isEmpty();

	/**
	 * Pega a imagem em uma dada posição.
	 * 
	 * @param i
	 *            A posição
	 * @return A imagem na posição i.
	 */
	public IImage getImageAt(int i);

	/**
	 * Pega a imagem com um {@link Uri} particular.
	 * 
	 * @param uri
	 * @return A imagem com um {@link Uri} particular. null se não encontrada.
	 */
	public IImage getImageForUri(Uri uri);

	/**
	 * Remove uma imagem da lista.
	 * 
	 * @param image
	 *            Imagem a ser removida.
	 * @return <code>true</code> se a imagem foi removida com sucesso.
	 */
	public boolean removeImage(IImage image);

	/**
	 * Remove a imagem situada na posição i.
	 * 
	 * @param i
	 *            Posição.
	 * @return <code>true</code> se a imagem foi removida com sucesso.
	 */
	public boolean removeImageAt(int i);

	/**
	 * Pega a posição de uma dada imagem nesta lista.
	 * 
	 * @param image
	 *            A imagem.
	 * @return O índice da posição.
	 */
	public int getImageIndex(IImage image);

	/**
	 * Fecha esta lista para liberar recursos, nenhuma operação será permitida
	 * mais.
	 */
	public void close();
}
