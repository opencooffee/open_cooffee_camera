package com.opencooffeecamera.library;

import android.os.Environment;

import java.io.File;

public class ExternalStorage {

    //private static final String LOG_TAG = ExternalStorage.class.getSimpleName();

    private static File externalStoragePublicDir;

    public ExternalStorage(String albumName) {

        if (ExternalStorage.isExternalStorageAvailable() && !ExternalStorage.isExternalStorageReadOnly()) {

            externalStoragePublicDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), albumName);

            if (!externalStoragePublicDir.exists()) {
        
                externalStoragePublicDir.mkdirs();

                if (!externalStoragePublicDir.exists()) {
                    externalStoragePublicDir.mkdirs();
                }

            }

        }

    }

    private static boolean isExternalStorageReadOnly() {

        String extStorageState = Environment.getExternalStorageState();

        return Environment.MEDIA_MOUNTED_READ_ONLY.equals(extStorageState);
    }

    private static boolean isExternalStorageAvailable() {

        String extStorageState = Environment.getExternalStorageState();

        return Environment.MEDIA_MOUNTED.equals(extStorageState);
    }

    public File getPublicAlbumFile(String fileName) {

        try {

            return new File(externalStoragePublicDir, fileName);

        } catch (NullPointerException e) {

            // If the name is null.
            e.printStackTrace();

            return null;
        }

    }

}
