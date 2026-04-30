package lk.kishan.carelink.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class FileUtil {

    public static File getFileFromUri(Context context, Uri uri) throws IOException {
        InputStream inputStream = context.getContentResolver().openInputStream(uri);

        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
        if (inputStream != null) inputStream.close();

        File tempFile = File.createTempFile("gallery_prescription", ".jpg", context.getCacheDir());
        FileOutputStream out = new FileOutputStream(tempFile);

        if (bitmap != null) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, out);
        }

        out.flush();
        out.close();
        return tempFile;
    }

    public static File getFileFromBitmap(Context context, Bitmap bitmap) throws IOException {
        File tempFile = File.createTempFile("camera_prescription", ".jpg", context.getCacheDir());
        FileOutputStream out = new FileOutputStream(tempFile);
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
        out.flush();
        out.close();
        return tempFile;
    }
}