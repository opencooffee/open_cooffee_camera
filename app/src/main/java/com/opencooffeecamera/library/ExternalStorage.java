package com.opencooffeecamera.library;

import android.os.Environment;

import java.io.File;

public class ExternalStorage {

    //private static final String LOG_TAG = ExternalStorage.class.getSimpleName();

    private static File externalStoragePublicDir;

    // Constructor.
    public ExternalStorage(String albumName) {

        if (ExternalStorage.isExternalStorageAvailable() && !ExternalStorage.isExternalStorageReadOnly()) {

            externalStoragePublicDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), albumName);

            if (!externalStoragePublicDir.exists()) {
                externalStoragePublicDir.mkdirs();
            }

        }

    }

    /**
     * Check if external storage is in read-only mode.
     *
     * @return Boolean value
     */
    private static boolean isExternalStorageReadOnly() {

        String extStorageState = Environment.getExternalStorageState();

        return Environment.MEDIA_MOUNTED_READ_ONLY.equals(extStorageState);
    }

    /**
     * Check if external storage is available.
     *
     * @return Booleano value
     */
    private static boolean isExternalStorageAvailable() {

        String extStorageState = Environment.getExternalStorageState();

        return Environment.MEDIA_MOUNTED.equals(extStorageState);
    }

    /**
     * Get public album file.
     *
     * @param fileName
     *
     * @return file
     */
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