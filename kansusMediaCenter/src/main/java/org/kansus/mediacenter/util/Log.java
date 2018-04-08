package org.kansus.mediacenter.util;

public class Log {
	
	public static final String TAG = "KansusMediaCenterX";

	public static void i(String msg, Object... args) {
		if (msg == null)
			return;
		try {
			android.util.Log.i(TAG, String.format(msg, args));
		} catch (Exception e) {
			android.util.Log.e(TAG, "Log", e);
			android.util.Log.i(TAG, msg);
		}
	}

	public static void d(String msg, Object... args) {
		if (msg == null)
			return;
		try {
			android.util.Log.d(TAG, String.format(msg, args));
		} catch (Exception e) {
			android.util.Log.e(TAG, "Log", e);
			android.util.Log.d(TAG, msg);
		}
	}

	public static void e(String msg, Object... args) {
		if (msg == null)
			return;
		try {
			android.util.Log.e(TAG, String.format(msg, args));
		} catch (Exception e) {
			android.util.Log.e(TAG, "Log", e);
			android.util.Log.e(TAG, msg);
		}
	}

	public static void e(String msg, Throwable t) {
		if (msg == null)
			return;
		android.util.Log.e(TAG, msg, t);
	}
}