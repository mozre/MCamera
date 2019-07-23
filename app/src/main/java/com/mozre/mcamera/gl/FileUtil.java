package com.mozre.mcamera.gl;

import android.content.Context;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class FileUtil {

    public static byte[] getFileContent( InputStream is) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[ 4096];
        try {
            int count = -1;
            while ( ( count = is.read( buffer)) != -1)
                byteArrayOutputStream.write( buffer, 0, count);
            byteArrayOutputStream.close();
        } catch (IOException e) {
            return null;
        }
        return byteArrayOutputStream.toByteArray();
    }

    public static byte[] getFileContent( Context context, int id) {
        InputStream is = context.getResources().openRawResource( id);
        return getFileContent( is);
    }

    public static byte[] getFileContent( String path) {
        try {
            return getFileContent( new FileInputStream( path));
        } catch ( IOException e) {
            return null;
        }
    }

    public static byte[] getFileContent( File file) {
        try {
            return getFileContent( new FileInputStream( file));
        } catch ( IOException e) {
            return null;
        }
    }
}
