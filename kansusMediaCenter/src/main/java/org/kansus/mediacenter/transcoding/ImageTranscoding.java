package org.kansus.mediacenter.transcoding;

import java.io.File;
import java.io.FileOutputStream;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.os.Environment;

public class ImageTranscoding {

	private static void codec(Bitmap src, String fileName,
			Bitmap.CompressFormat format, int quality) {
		if (format == CompressFormat.JPEG)
			fileName += ".jpg";
		else if (format == CompressFormat.PNG)
			fileName += ".png";
		File outputDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Pictures/Converted");
		File dest = new File(outputDir , fileName);

		Bitmap bitmap = src;
		try {
			FileOutputStream out = new FileOutputStream(dest);
			bitmap.compress(format, quality, out);
			out.flush();
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void toJPG(Bitmap src, String fileName, Bitmap.CompressFormat format,
			int quality) {
		codec(src, fileName, CompressFormat.JPEG, quality);
	}
	
	public static void toPNG(Bitmap src, String fileName, Bitmap.CompressFormat format) {
		codec(src, fileName, CompressFormat.PNG, 90);
	}
}
