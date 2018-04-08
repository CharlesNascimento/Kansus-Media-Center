/*
 * Copyright (C) 2008 The Android Open Source Project
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

package org.kansus.mediacenter.helpers;

import java.io.Closeable;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.kansus.mediacenter.R;
import org.kansus.mediacenter.structure.IImage;
import org.kansus.mediacenter.util.ImageManager;
import org.kansus.mediacenter.util.Util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.StatFs;
import android.preference.PreferenceManager;
import android.text.format.Formatter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * A utility class to handle various kinds of menu operations.
 */
public class MenuHelper {
	private static final String TAG = "MenuHelper";

	public static final int INCLUDE_ALL = 0xFFFFFFFF;
	public static final int INCLUDE_VIEWPLAY_MENU = (1 << 0);
	public static final int INCLUDE_SHARE_MENU = (1 << 1);
	public static final int INCLUDE_SET_MENU = (1 << 2);
	public static final int INCLUDE_CROP_MENU = (1 << 3);
	public static final int INCLUDE_DELETE_MENU = (1 << 4);
	public static final int INCLUDE_ROTATE_MENU = (1 << 5);
	public static final int INCLUDE_DETAILS_MENU = (1 << 6);
	public static final int INCLUDE_SHOWMAP_MENU = (1 << 7);

	public static final int MENU_IMAGE_SHARE = 1;
	public static final int MENU_IMAGE_SHOWMAP = 2;

	public static final int POSITION_SWITCH_CAMERA_MODE = 1;
	public static final int POSITION_GOTO_GALLERY = 2;
	public static final int POSITION_VIEWPLAY = 3;
	public static final int POSITION_CAPTURE_PICTURE = 4;
	public static final int POSITION_CAPTURE_VIDEO = 5;
	public static final int POSITION_IMAGE_SHARE = 6;
	public static final int POSITION_IMAGE_ROTATE = 7;
	public static final int POSITION_IMAGE_TOSS = 8;
	public static final int POSITION_IMAGE_CROP = 9;
	public static final int POSITION_IMAGE_SET = 10;
	public static final int POSITION_DETAILS = 11;
	public static final int POSITION_SHOWMAP = 12;
	public static final int POSITION_SLIDESHOW = 13;
	public static final int POSITION_MULTISELECT = 14;
	public static final int POSITION_CAMERA_SETTING = 15;
	public static final int POSITION_GALLERY_SETTING = 16;

	public static final int NO_STORAGE_ERROR = -1;
	public static final int CANNOT_STAT_ERROR = -2;
	public static final String EMPTY_STRING = "";
	public static final String JPEG_MIME_TYPE = "image/jpeg";
	// valid range is -180f to +180f
	public static final float INVALID_LATLNG = 255f;

	/**
	 * Activity result code used to report crop results.
	 */
	public static final int RESULT_COMMON_MENU_CROP = 490;

	public interface MenuItemsResult {
		public void gettingReadyToOpen(Menu menu, IImage image);

		public void aboutToCall(MenuItem item, IImage image);
	}

	public interface MenuInvoker {
		public void run(MenuCallback r);
	}

	public interface MenuCallback {
		public void run(Uri uri, IImage image);
	}

	public static void closeSilently(Closeable c) {
		if (c != null) {
			try {
				c.close();
			} catch (Throwable e) {
				// ignore
			}
		}
	}

	public static long getImageFileSize(IImage image) {
		java.io.InputStream data = image.fullSizeImageData();
		if (data == null)
			return -1;
		try {
			return data.available();
		} catch (java.io.IOException ex) {
			return -1;
		} finally {
			closeSilently(data);
		}
	}

	// This is a hack before we find a solution to pass a permission to other
	// applications. See bug #1735149, #1836138.
	// Checks if the URI is on our whitelist:
	// content://media/... (MediaProvider)
	// file:///sdcard/... (Browser download)
	public static boolean isWhiteListUri(Uri uri) {
		if (uri == null)
			return false;

		String scheme = uri.getScheme();
		String authority = uri.getAuthority();

		if (scheme.equals("content") && authority.equals("media")) {
			return true;
		}

		if (scheme.equals("file")) {
			List<String> p = uri.getPathSegments();

			if (p.size() >= 1 && p.get(0).equals("sdcard")) {
				return true;
			}
		}

		return false;
	}

	public static void enableShareMenuItem(Menu menu, boolean enabled) {
		MenuItem item = menu.findItem(MENU_IMAGE_SHARE);
		if (item != null) {
			item.setVisible(enabled);
			item.setEnabled(enabled);
		}
	}

	private static void setDetailsValue(View d, String text, int valueId) {
		((TextView) d.findViewById(valueId)).setText(text);
	}

	private static void hideDetailsRow(View d, int rowId) {
		d.findViewById(rowId).setVisibility(View.GONE);
	}

	private static ExifInterface getExif(IImage image) {
		if (!JPEG_MIME_TYPE.equals(image.getMimeType())) {
			return null;
		}

		try {
			return new ExifInterface(image.getDataPath());
		} catch (IOException ex) {
			Log.e(TAG, "cannot read exif", ex);
			return null;
		}
	}

	private static void hideExifInformation(View d) {
		hideDetailsRow(d, R.id.details_resolution_row);
		hideDetailsRow(d, R.id.details_make_row);
		hideDetailsRow(d, R.id.details_model_row);
		hideDetailsRow(d, R.id.details_whitebalance_row);
	}

	private static void showExifInformation(IImage image, View d,
			Activity activity) {
		ExifInterface exif = getExif(image);
		if (exif == null) {
			hideExifInformation(d);
			return;
		}

		String value = exif.getAttribute(ExifInterface.TAG_MAKE);
		if (value != null) {
			setDetailsValue(d, value, R.id.details_make_value);
		} else {
			hideDetailsRow(d, R.id.details_make_row);
		}

		value = exif.getAttribute(ExifInterface.TAG_MODEL);
		if (value != null) {
			setDetailsValue(d, value, R.id.details_model_value);
		} else {
			hideDetailsRow(d, R.id.details_model_row);
		}

		value = getWhiteBalanceString(exif);
		if (value != null && !value.equals(EMPTY_STRING)) {
			setDetailsValue(d, value, R.id.details_whitebalance_value);
		} else {
			hideDetailsRow(d, R.id.details_whitebalance_row);
		}
	}

	/**
	 * Returns a human-readable string describing the white balance value.
	 * Returns empty string if there is no white balance value or it is not
	 * recognized.
	 */
	private static String getWhiteBalanceString(ExifInterface exif) {
		int whitebalance = exif.getAttributeInt(
				ExifInterface.TAG_WHITE_BALANCE, -1);
		if (whitebalance == -1)
			return "";

		switch (whitebalance) {
		case ExifInterface.WHITEBALANCE_AUTO:
			return "Auto";
		case ExifInterface.WHITEBALANCE_MANUAL:
			return "Manual";
		default:
			return "";
		}
	}

	// Called when "Details" is clicked.
	// Displays detailed information about the image/video.
	private static boolean onDetailsClicked(MenuInvoker onInvoke,
			final Handler handler, final Activity activity) {
		onInvoke.run(new MenuCallback() {
			public void run(Uri u, IImage image) {
				if (image == null) {
					return;
				}

				final AlertDialog.Builder builder = new AlertDialog.Builder(
						activity);

				final View d = View.inflate(activity, R.layout.details_viewer,
						null);

				ImageView imageView = (ImageView) d
						.findViewById(R.id.details_thumbnail_image);
				imageView.setImageBitmap(image.miniThumbBitmap());

				TextView textView = (TextView) d
						.findViewById(R.id.details_image_title);
				textView.setText(image.getTitle());

				long length = getImageFileSize(image);
				String lengthString = length < 0 ? EMPTY_STRING : Formatter
						.formatFileSize(activity, length);
				((TextView) d.findViewById(R.id.details_file_size_value))
						.setText(lengthString);

				d.findViewById(R.id.details_frame_rate_row).setVisibility(
						View.GONE);
				d.findViewById(R.id.details_bit_rate_row).setVisibility(
						View.GONE);
				d.findViewById(R.id.details_format_row)
						.setVisibility(View.GONE);
				d.findViewById(R.id.details_codec_row).setVisibility(View.GONE);

				int dimensionWidth = 0;
				int dimensionHeight = 0;
				if (ImageManager.isImage(image)) {
					// getWidth is much slower than reading from EXIF
					dimensionWidth = image.getWidth();
					dimensionHeight = image.getHeight();
					d.findViewById(R.id.details_duration_row).setVisibility(
							View.GONE);
				}

				String value = null;
				if (dimensionWidth > 0 && dimensionHeight > 0) {
					value = String.format(
							activity.getString(R.string.details_dimension_x),
							dimensionWidth, dimensionHeight);
				}

				if (value != null) {
					setDetailsValue(d, value, R.id.details_resolution_value);
				} else {
					hideDetailsRow(d, R.id.details_resolution_row);
				}

				value = EMPTY_STRING;
				long dateTaken = image.getDateTaken();
				if (dateTaken != 0) {
					Date date = new Date(image.getDateTaken());
					SimpleDateFormat dateFormat = new SimpleDateFormat();
					value = dateFormat.format(date);
				}
				if (value != EMPTY_STRING) {
					setDetailsValue(d, value, R.id.details_date_taken_value);
				} else {
					hideDetailsRow(d, R.id.details_date_taken_row);
				}

				// Show more EXIF header details for JPEG images.
				if (JPEG_MIME_TYPE.equals(image.getMimeType())) {
					showExifInformation(image, d, activity);
				} else {
					hideExifInformation(d);
				}

				builder.setNeutralButton(R.string.details_ok,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								dialog.dismiss();
							}
						});

				handler.post(new Runnable() {
					public void run() {
						builder.setIcon(android.R.drawable.ic_dialog_info)
								.setTitle(R.string.details_panel_title)
								.setView(d).show();
					}
				});
			}
		});
		return true;
	}

	// Called when "Rotate left" or "Rotate right" is clicked.
	private static boolean onRotateClicked(MenuInvoker onInvoke,
			final int degree) {
		onInvoke.run(new MenuCallback() {
			public void run(Uri u, IImage image) {
				if (image == null || image.isReadonly()) {
					return;
				}
				image.rotateImageBy(degree);
			}
		});
		return true;
	}

	// Called when "Crop" is clicked.
	private static boolean onCropClicked(MenuInvoker onInvoke,
			final Activity activity) {
		onInvoke.run(new MenuCallback() {
			public void run(Uri u, IImage image) {
				if (u == null) {
					return;
				}

				Intent cropIntent = new Intent("org.kansus.mediacenter.action.CROP");
				cropIntent.setData(u);
				activity.startActivityForResult(cropIntent,
						RESULT_COMMON_MENU_CROP);
				activity.overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_left);
			}
		});
		return true;
	}

	// Called when "Set as" is clicked.
	private static boolean onSetAsClicked(MenuInvoker onInvoke,
			final Activity activity) {
		onInvoke.run(new MenuCallback() {
			public void run(Uri u, IImage image) {
				if (u == null || image == null) {
					return;
				}

				Intent intent = Util.createSetAsIntent(image);
				activity.startActivity(Intent.createChooser(intent,
						activity.getText(R.string.setImage)));
				activity.overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_left);
			}
		});
		return true;
	}

	// Called when "Share" is clicked.
	private static boolean onImageShareClicked(MenuInvoker onInvoke,
			final Activity activity) {
		onInvoke.run(new MenuCallback() {
			public void run(Uri u, IImage image) {
				if (image == null)
					return;

				Intent intent = new Intent();
				intent.setAction(Intent.ACTION_SEND);
				String mimeType = image.getMimeType();
				intent.setType(mimeType);
				intent.putExtra(Intent.EXTRA_STREAM, u);
				boolean isImage = ImageManager.isImage(image);
				try {
					activity.startActivity(Intent.createChooser(intent,
							activity.getText(isImage ? R.string.sendImage
									: R.string.sendVideo)));
					activity.overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_left);
				} catch (android.content.ActivityNotFoundException ex) {
					Toast.makeText(
							activity,
							isImage ? R.string.no_way_to_share_image
									: R.string.no_way_to_share_video,
							Toast.LENGTH_SHORT).show();
				}
			}
		});
		return true;
	}

	// Called when "Play" is clicked.
	private static boolean onViewPlayClicked(MenuInvoker onInvoke,
			final Activity activity) {
		onInvoke.run(new MenuCallback() {
			public void run(Uri uri, IImage image) {
				if (image != null) {
					Intent intent = new Intent(Intent.ACTION_VIEW, image
							.fullSizeImageUri());
					activity.startActivity(intent);
					activity.overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_left);
				}
			}
		});
		return true;
	}

	// Called when "Delete" is clicked.
	private static boolean onDeleteClicked(MenuInvoker onInvoke,
			final Activity activity, final Runnable onDelete) {
		onInvoke.run(new MenuCallback() {
			public void run(Uri uri, IImage image) {
				if (image != null) {
					deleteImage(activity, onDelete, image);
				}
			}
		});
		return true;
	}

	public static MenuItemsResult addImageMenuItems(Menu menu, int inclusions,
			final Activity activity, final Handler handler,
			final Runnable onDelete, final MenuInvoker onInvoke) {
		final ArrayList<MenuItem> requiresWriteAccessItems = new ArrayList<MenuItem>();
		final ArrayList<MenuItem> requiresNoDrmAccessItems = new ArrayList<MenuItem>();
		final ArrayList<MenuItem> requiresImageItems = new ArrayList<MenuItem>();
		final ArrayList<MenuItem> requiresVideoItems = new ArrayList<MenuItem>();

		if ((inclusions & INCLUDE_ROTATE_MENU) != 0) {
			SubMenu rotateSubmenu = menu.addSubMenu(Menu.NONE, Menu.NONE,
					POSITION_IMAGE_ROTATE, R.string.rotate).setIcon(
					android.R.drawable.ic_menu_rotate);
			// Don't show the rotate submenu if the item at hand is read only
			// since the items within the submenu won't be shown anyway. This
			// is really a framework bug in that it shouldn't show the submenu
			// if the submenu has no visible items.
			MenuItem rotateLeft = rotateSubmenu
					.add(R.string.rotate_left)
					.setOnMenuItemClickListener(
							new MenuItem.OnMenuItemClickListener() {
								public boolean onMenuItemClick(MenuItem item) {
									return onRotateClicked(onInvoke, -90);
								}
							}).setAlphabeticShortcut('l');

			MenuItem rotateRight = rotateSubmenu
					.add(R.string.rotate_right)
					.setOnMenuItemClickListener(
							new MenuItem.OnMenuItemClickListener() {
								public boolean onMenuItemClick(MenuItem item) {
									return onRotateClicked(onInvoke, 90);
								}
							}).setAlphabeticShortcut('r');

			requiresWriteAccessItems.add(rotateSubmenu.getItem());
			requiresWriteAccessItems.add(rotateLeft);
			requiresWriteAccessItems.add(rotateRight);

			requiresImageItems.add(rotateSubmenu.getItem());
			requiresImageItems.add(rotateLeft);
			requiresImageItems.add(rotateRight);
		}

		if ((inclusions & INCLUDE_CROP_MENU) != 0) {
			MenuItem autoCrop = menu.add(Menu.NONE, Menu.NONE,
					POSITION_IMAGE_CROP, R.string.camera_crop);
			autoCrop.setIcon(android.R.drawable.ic_menu_crop);
			autoCrop.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
				public boolean onMenuItemClick(MenuItem item) {
					return onCropClicked(onInvoke, activity);
				}
			});
			requiresWriteAccessItems.add(autoCrop);
			requiresImageItems.add(autoCrop);
		}

		if ((inclusions & INCLUDE_SET_MENU) != 0) {
			MenuItem setMenu = menu.add(Menu.NONE, Menu.NONE,
					POSITION_IMAGE_SET, R.string.camera_set);
			setMenu.setIcon(android.R.drawable.ic_menu_set_as);
			setMenu.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
				public boolean onMenuItemClick(MenuItem item) {
					return onSetAsClicked(onInvoke, activity);
				}
			});
			requiresImageItems.add(setMenu);
		}

		if ((inclusions & INCLUDE_SHARE_MENU) != 0) {
			MenuItem item1 = menu.add(Menu.NONE, MENU_IMAGE_SHARE,
					POSITION_IMAGE_SHARE, R.string.camera_share)
					.setOnMenuItemClickListener(
							new MenuItem.OnMenuItemClickListener() {
								public boolean onMenuItemClick(MenuItem item) {
									return onImageShareClicked(onInvoke,
											activity);
								}
							});
			item1.setIcon(android.R.drawable.ic_menu_share);
			MenuItem item = item1;
			requiresNoDrmAccessItems.add(item);
		}

		if ((inclusions & INCLUDE_DELETE_MENU) != 0) {
			MenuItem deleteItem = menu.add(Menu.NONE, Menu.NONE,
					POSITION_IMAGE_TOSS, R.string.camera_toss);
			requiresWriteAccessItems.add(deleteItem);
			deleteItem
					.setOnMenuItemClickListener(
							new MenuItem.OnMenuItemClickListener() {
								public boolean onMenuItemClick(MenuItem item) {
									return onDeleteClicked(onInvoke, activity,
											onDelete);
								}
							}).setAlphabeticShortcut('d')
					.setIcon(android.R.drawable.ic_menu_delete);
		}

		if ((inclusions & INCLUDE_DETAILS_MENU) != 0) {
			MenuItem detailsMenu = menu.add(Menu.NONE, Menu.NONE,
					POSITION_DETAILS, R.string.details)
					.setOnMenuItemClickListener(
							new MenuItem.OnMenuItemClickListener() {
								public boolean onMenuItemClick(MenuItem item) {
									return onDetailsClicked(onInvoke, handler,
											activity);
								}
							});
			detailsMenu.setIcon(R.drawable.ic_menu_view_details);
		}

		if ((inclusions & INCLUDE_VIEWPLAY_MENU) != 0) {
			MenuItem videoPlayItem = menu.add(Menu.NONE, Menu.NONE,
					POSITION_VIEWPLAY, R.string.video_play)
					.setOnMenuItemClickListener(
							new MenuItem.OnMenuItemClickListener() {
								public boolean onMenuItemClick(MenuItem item) {
									return onViewPlayClicked(onInvoke, activity);
								}
							});
			requiresVideoItems.add(videoPlayItem);
		}

		return new MenuItemsResult() {
			public void gettingReadyToOpen(Menu menu, IImage image) {
				// protect against null here. this isn't strictly speaking
				// required but if a client app isn't handling sdcard removal
				// properly it could happen
				if (image == null) {
					return;
				}

				ArrayList<MenuItem> enableList = new ArrayList<MenuItem>();
				ArrayList<MenuItem> disableList = new ArrayList<MenuItem>();
				ArrayList<MenuItem> list;

				list = image.isReadonly() ? disableList : enableList;
				list.addAll(requiresWriteAccessItems);

				list = image.isDrm() ? disableList : enableList;
				list.addAll(requiresNoDrmAccessItems);

				list = ImageManager.isImage(image) ? enableList : disableList;
				list.addAll(requiresImageItems);

				list = ImageManager.isVideo(image) ? enableList : disableList;
				list.addAll(requiresVideoItems);

				for (MenuItem item : enableList) {
					item.setVisible(true);
					item.setEnabled(true);
				}

				for (MenuItem item : disableList) {
					item.setVisible(false);
					item.setEnabled(false);
				}
			}

			// must override abstract method
			public void aboutToCall(MenuItem menu, IImage image) {
			}
		};
	}

	public static void deletePhoto(Activity activity, Runnable onDelete) {
		deleteImpl(activity, onDelete, true);
	}

	public static void deleteImage(Activity activity, Runnable onDelete, IImage image) {
		deleteImpl(activity, onDelete, ImageManager.isImage(image));
	}

	static void deleteImpl(Activity activity, Runnable onDelete, boolean isImage) {
		boolean needConfirm = PreferenceManager.getDefaultSharedPreferences(
				activity).getBoolean("pref_gallery_confirm_delete_key", true);
		if (!needConfirm) {
			if (onDelete != null)
				onDelete.run();
		} else {
			String title = activity.getString(R.string.confirm_delete_title);
			String message = activity
					.getString(isImage ? R.string.confirm_delete_message
							: R.string.confirm_delete_video_message);
			confirmAction(activity, title, message, onDelete);
		}
	}

	public static void deleteMultiple(Context context, Runnable action) {
		boolean needConfirm = PreferenceManager.getDefaultSharedPreferences(
				context).getBoolean("pref_gallery_confirm_delete_key", true);
		if (!needConfirm) {
			if (action != null)
				action.run();
		} else {
			String title = context.getString(R.string.confirm_delete_title);
			String message = context
					.getString(R.string.confirm_delete_multiple_message);
			confirmAction(context, title, message, action);
		}
	}

	public static void confirmAction(Context context, String title,
			String message, final Runnable action) {
		OnClickListener listener = new OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				switch (which) {
				case DialogInterface.BUTTON_POSITIVE:
					if (action != null)
						action.run();
				}
			}
		};
		new AlertDialog.Builder(context)
				.setIcon(android.R.drawable.ic_dialog_alert).setTitle(title)
				.setMessage(message)
				.setPositiveButton(android.R.string.ok, listener)
				.setNegativeButton(android.R.string.cancel, listener).create()
				.show();
	}

	public static String formatDuration(final Context context, int durationMs) {
		int duration = durationMs / 1000;
		int h = duration / 3600;
		int m = (duration - h * 3600) / 60;
		int s = duration - (h * 3600 + m * 60);
		String durationValue;
		if (h == 0) {
			durationValue = String.format(
					context.getString(R.string.details_ms), m, s);
		} else {
			durationValue = String.format(
					context.getString(R.string.details_hms), h, m, s);
		}
		return durationValue;
	}

	public static void showStorageToast(Activity activity) {
		showStorageToast(activity, calculatePicturesRemaining());
	}

	public static void showStorageToast(Activity activity, int remaining) {
		String noStorageText = null;

		if (remaining == MenuHelper.NO_STORAGE_ERROR) {
			String state = Environment.getExternalStorageState();
			if (state == Environment.MEDIA_CHECKING) {
				noStorageText = activity.getString(R.string.preparing_sd);
			} else {
				noStorageText = activity.getString(R.string.no_storage);
			}
		} else if (remaining < 1) {
			noStorageText = activity.getString(R.string.not_enough_space);
		}

		if (noStorageText != null) {
			Toast.makeText(activity, noStorageText, Toast.LENGTH_LONG).show();
		}
	}

	public static int calculatePicturesRemaining() {
		try {
			if (!ImageManager.hasStorage()) {
				return NO_STORAGE_ERROR;
			} else {
				String storageDirectory = Environment
						.getExternalStorageDirectory().toString();
				StatFs stat = new StatFs(storageDirectory);
				float remaining = ((float) stat.getAvailableBlocks() * (float) stat
						.getBlockSize()) / 400000F;
				return (int) remaining;
			}
		} catch (Exception ex) {
			// if we can't stat the filesystem then we don't know how many
			// pictures are remaining. it might be zero but just leave it
			// blank since we really don't know.
			return CANNOT_STAT_ERROR;
		}
	}
}
