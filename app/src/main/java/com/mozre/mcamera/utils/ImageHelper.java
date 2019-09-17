package com.mozre.mcamera.utils;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ImageHelper {
    private static final String TAG = Constants.getTagName(ImageHelper.class.getSimpleName());
    private static final String IMAGE_PRE_NAME = "IMG_";
    private static final String IMAGE_END_NAME = ".jpg";
    private static final String DATA_STR_FORMAL = "yyyyMMddHHmmss";
    private static final String IMAGE_DIRECTORY = "Camera2";

    public static String imageFileName() {
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATA_STR_FORMAL);
        StringBuffer buffer = new StringBuffer(IMAGE_PRE_NAME);
        buffer.append(dateFormat.format(new Date())).append(IMAGE_END_NAME);

        return buffer.toString();
    }

    public static String saveImage(byte[] data) {
        File rootMediaPath = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),IMAGE_DIRECTORY);
        if (!rootMediaPath.exists() && !rootMediaPath.mkdirs()) {
            Log.e(TAG, "onImageAvailable create file fail!");
        }
        File imageFile = new File(rootMediaPath.getPath(), imageFileName());
        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(imageFile);
            outputStream.write(data);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "capture onImageAvailable: FileNotFoundException " , e);
        } catch (IOException e) {
            Log.e(TAG, "capture onImageAvailable: IOException", e);
        } finally {
            if (outputStream !=null ){
                try {
                    outputStream.close();
                } catch (IOException e) {
                    Log.e(TAG, "onImageAvailable: IOException", e);
                }
            }
        }
        return imageFile.getPath();
    }

    public static void insertImageToDatabase(String filePath, int dataSize, ContentResolver resolver) {
        File file = new File(filePath);
        ContentValues values = new ContentValues(9);
        values.put(MediaStore.Images.ImageColumns.TITLE, filePath.substring(filePath.lastIndexOf(File.separator), filePath.indexOf(".")));
        values.put(MediaStore.Images.ImageColumns.ORIENTATION, 0);
        values.put(MediaStore.Images.ImageColumns.DISPLAY_NAME, file.getName());
        values.put(MediaStore.Images.ImageColumns.DATE_TAKEN, System.currentTimeMillis());
        values.put(MediaStore.Images.ImageColumns.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.Images.ImageColumns.DATA, filePath);
        values.put(MediaStore.Images.ImageColumns.SIZE, dataSize);
        resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
    }

}
