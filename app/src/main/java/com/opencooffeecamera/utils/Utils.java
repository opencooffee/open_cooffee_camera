package com.opencooffeecamera.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Point;
import android.support.media.ExifInterface;
import android.os.Build;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;

import com.opencooffeecamera.Common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Calendar;
import java.util.UUID;

public class Utils {

    /**
     * Get the end user visible name the end product.
     *
     * @return String deviceModel
     */
    public static String getDeviceModel() {

        String deviceModel;

        // The end-user-visible name for the end product.
        deviceModel = Build.MODEL;

        if (deviceModel != null) {
            return deviceModel;
        } else {
            return "";
        }

    }

    /**
     * Get the device's current time in milliseconds from January 1, 1970 00: 00: 00.0 UTC.
     *
     * @return Long timestamp
     */
    public static long getDeviceCurrentTimestamp() {
        return System.currentTimeMillis();
    }

    /**
     * Get the width of the device screen.
     *
     * @param context
     *
     * @return device screen width as int
     */
    public static int getDeviceScreeWidth(Context context) {

        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);

        if (windowManager != null) {

            Display display = windowManager.getDefaultDisplay();

            if (display != null) {

                Point size = new Point();

                display.getSize(size);

                return size.x;
            } else {
                return 0;
            }

        }

        return 0;
    }

    /**
     * Get the height of the device screen.
     *
     * @param context
     *
     * @return device screen height as int
     */
    public static int getDeviceScreenHeight(Context context) {

        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);

        if (windowManager != null) {

            Display display = windowManager.getDefaultDisplay();

            if (display != null) {

                Point size = new Point();

                display.getSize(size);

                return size.y;

            } else {
                return 0;
            }

        }

        return 0;
    }

    /**
     * Show system UI.
     *
     * @param decorView
     */
    @SuppressLint("InlinedApi")
    public static void showSystemUI(View decorView) {
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }

    /**
     * This snippet hides the system bars.
     *
     * @param decorView
     */
    @SuppressLint("InlinedApi")
    public static void hideSystemUI(View decorView) {

        // Set the IMMERSIVE flag.
        // Set the content to appear under the system bars so that the content doesn't resize when the system bars hide and show.
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // Hide navigation bar.
                        | View.SYSTEM_UI_FLAG_FULLSCREEN // Hide status bar.
                        | View.SYSTEM_UI_FLAG_IMMERSIVE
        );

    }

    /**
     * Convert DP units to their equivalent in PX, depending on the density of the device.
     *
     * @param context
     * It is used to obtain resources and metric visualization specific to the device.
     *
     * @param dp
     * Need to convert it into pixels.
     *
     * @return a float value to represent the value in PX equivalent to the DP depending on the density of the device.
     */
    public static float convertDpToPixel(Context context, float dp) {

        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();

        return dp * (metrics.densityDpi / 160f);
    }

    /**
     * Convert PX units to their equivalent in DP, depending on the density of the device.
     *
     * @param context
     * It is used to obtain resources and metric visualization specific to the device.
     *
     * @param px
     * Need to convert it into DP.
     *
     * @return a float value to represent the value in DP equivalent to the PX depending on the density of the device.
     */
    public static float convertPixelsToDp(Context context, float px) {

        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();

        return px / (metrics.densityDpi / 160f);
    }

    /**
     * Get the distance in DP.
     *
     * @param context
     * @param x1
     * @param y1
     * @param x2
     * @param y2
     *
     * @return float distance
     */
    public static float onTouchDistance(Context context, float x1, float y1, float x2, float y2) {

        float dx = x1 - x2;
        float dy = y1 - y2;

        float distanceInPx = (float) Math.sqrt(dx * dx + dy * dy);

        return convertPixelsToDp(context, distanceInPx);
    }

    /**
     * Generate a compressed UUID String.
     *
     * @return String compressUUIDString
     */
    public static String generateCompressUUIDString() {

        UUID uuid = UUID.randomUUID();

        long mostSignificant = uuid.getMostSignificantBits();
        long leastSignificant = uuid.getLeastSignificantBits();

        ByteBuffer byteBuffer = ByteBuffer.allocate(16);

        byteBuffer.putLong(mostSignificant);
        byteBuffer.putLong(leastSignificant);

        byte[] uuidByteArray = byteBuffer.array();

        // Base64.NO_WRAP: Parameter to omit all line terminations.
        // Base64.URL_SAFE: Parameter to indicate the use of the variant "URL and the safe name" of base 64 (see RFC 3548 section 4), where - and _ is used instead of + and /.
        return Base64.encodeToString(uuidByteArray, Base64.NO_WRAP | Base64.URL_SAFE);
    }

    /**
     * Copy a file.
     *
     * @param srcFile
     *
     * @param dstFile
     *
     * @return Boolean value
     */
    public static boolean copyFile(File srcFile, File dstFile) {

        boolean isFileCopied = false;

        InputStream is = null;
        OutputStream os = null;

        byte[] bytes = new byte[Common.BUFFER_SIZE];

        try {

            is = new FileInputStream(srcFile);
            os = new FileOutputStream(dstFile);

            for(;;) {

                int count = is.read(bytes, 0, Common.BUFFER_SIZE);

                if (count == -1) {
                    break;
                }

                os.write(bytes, 0, count);

                os.flush();
            }

            isFileCopied = true;

        } catch (Exception e) {
            // If the stream is closed or another IOException occurs.
            e.printStackTrace();
        } finally {

            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            return isFileCopied;
        }

    }

    /**
     * Get a file name from the current time.
     * With this, we make sure that each file has a different name every time.
     *
     * @param mediaType
     *
     * @return mediaFileName
     */
    public static String getMediaFileNameByCurrentTime(String mediaType) {

        // The Calendar class is an abstract class that provides methods for converting between a specific instant in time and a set of calendar fields such as YEAR, MONTH, DAY_OF_MONTH, HOUR,
        // and so on, and for manipulating the calendar fields, such as getting the date of the next week.
        // An instant in time can be represented by a millisecond value that is an offset from the Epoch, January 1, 1970 00:00:00.000 GMT (Gregorian).
        Calendar currentCalendar = Calendar.getInstance();

        // Field number for get and set indicating the second within the minute.
        int currentSecond = currentCalendar.get(Calendar.SECOND);

        // Field number for get and set indicating the minute within the hour.
        int currentMinute = currentCalendar.get(Calendar.MINUTE);

        // Field number for get and set indicating the hour of the day.
        int currentHour = currentCalendar.get(Calendar.HOUR_OF_DAY);

        // Field number for get and set indicating the day of the month.
        int currentDay = currentCalendar.get(Calendar.DAY_OF_MONTH);

        // Field number for get and set indicating the month.
        int currentMonth = currentCalendar.get(Calendar.MONTH);

        // Field number for get and set indicating the year.
        int currentYear = currentCalendar.get(Calendar.YEAR);

        String mediaFileName = String.valueOf(currentHour) + "_" + String.valueOf(currentMinute) + "_" + String.valueOf(currentSecond) + "_" + String.valueOf(currentDay) + "_" + String.valueOf(currentMonth) + "_" + String.valueOf(currentYear);

        String extension = "";

        if (mediaType.equals(Common.TYPE_IMAGE)) {
            extension = ".jpg";
        }

        mediaFileName = mediaFileName + extension;

        return mediaFileName;
    }

    /**
     * Set all metadata to default values.
     *
     * @param file
     */
    public static void setMetadataToMediaFile(File file) {

        try {

            ExifInterface exifInterface = new ExifInterface(file.getPath());

            // This constant was deprecated in API level 24. use TAG_F_NUMBER instead.
            // Type is String.
            /*
            exifInterface.setAttribute(ExifInterface.TAG_APERTURE, "");
            */

            // This constant was deprecated in API level 24. use TAG_ISO_SPEED_RATINGS instead.
            // Type is String.
            /*
            exifInterface.setAttribute(ExifInterface.TAG_ISO, "");
            */

            // This constant was deprecated in API level 24. use TAG_SUBSEC_TIME_DIGITIZED instead.
            // Type is String.
            /*
            exifInterface.setAttribute(ExifInterface.TAG_SUBSEC_TIME_DIG, "");
            */

            // This constant was deprecated in API level 24. use TAG_SUBSEC_TIME_ORIGINAL instead.
            // Type is String.
            /*
            exifInterface.setAttribute(ExifInterface.TAG_SUBSEC_TIME_ORIG, "");
            */

            // Type is String.
            exifInterface.setAttribute(ExifInterface.TAG_DATETIME, "");

            // Type is Double.
            exifInterface.setAttribute(ExifInterface.TAG_EXPOSURE_TIME, "0.0");

            // Type is int.
            exifInterface.setAttribute(ExifInterface.TAG_FLASH, "0");

            // Type is rational.
            exifInterface.setAttribute(ExifInterface.TAG_FOCAL_LENGTH, "NaN");

            // The altitude (in meters) based on the reference in TAG_GPS_ALTITUDE_REF.
            // Type is int.
            exifInterface.setAttribute(ExifInterface.TAG_GPS_ALTITUDE, "0");

            // 0 if the altitude is above sea level.
            // Type is int.
            exifInterface.setAttribute(ExifInterface.TAG_GPS_ALTITUDE_REF, "0");

            // Type is String.
            exifInterface.setAttribute(ExifInterface.TAG_GPS_DATESTAMP, "");

            // Type is rational.
            exifInterface.setAttribute(ExifInterface.TAG_GPS_LATITUDE, "NaN");

            // Type is String.
            exifInterface.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "");

            // Type is rational.
            exifInterface.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, "NaN");

            // Type is String.
            exifInterface.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, "");

            // Type is String.
            exifInterface.setAttribute(ExifInterface.TAG_GPS_PROCESSING_METHOD, "");

            // Type is String.
            exifInterface.setAttribute(ExifInterface.TAG_GPS_TIMESTAMP, "0");

            // Type is int.
            exifInterface.setAttribute(ExifInterface.TAG_IMAGE_LENGTH, "0");

            // Type is int.
            exifInterface.setAttribute(ExifInterface.TAG_IMAGE_WIDTH, "0");

            // Type is String.
            exifInterface.setAttribute(ExifInterface.TAG_MAKE, "");

            // Type is String.
            exifInterface.setAttribute(ExifInterface.TAG_MODEL, "");

            // Type is int.
            exifInterface.setAttribute(ExifInterface.TAG_ORIENTATION, "0");

            // Type is int.
            exifInterface.setAttribute(ExifInterface.TAG_WHITE_BALANCE, "0");

            if (Build.VERSION.SDK_INT >= 23) {

                // Type is String.
                exifInterface.setAttribute(ExifInterface.TAG_DATETIME_DIGITIZED, "");

                // Type is String.
                exifInterface.setAttribute(ExifInterface.TAG_SUBSEC_TIME, "");

            }

            if (Build.VERSION.SDK_INT >= 24) {

                // Type is String.
                exifInterface.setAttribute(ExifInterface.TAG_APERTURE_VALUE, "");

                // Type is String.
                exifInterface.setAttribute(ExifInterface.TAG_ARTIST, "");

                // Type is String.
                exifInterface.setAttribute(ExifInterface.TAG_BITS_PER_SAMPLE, "");

                // Type is String.
                exifInterface.setAttribute(ExifInterface.TAG_BRIGHTNESS_VALUE, "");

                // Type is String.
                exifInterface.setAttribute(ExifInterface.TAG_CFA_PATTERN, "");

                // Type is int.
                exifInterface.setAttribute(ExifInterface.TAG_COLOR_SPACE, "0");

                // Type is String.
                exifInterface.setAttribute(ExifInterface.TAG_COMPONENTS_CONFIGURATION, "");

                // Type is rational.
                exifInterface.setAttribute(ExifInterface.TAG_COMPRESSED_BITS_PER_PIXEL, "NaN");

                // Type is int.
                exifInterface.setAttribute(ExifInterface.TAG_COMPRESSION, "0");

                // Type is int.
                exifInterface.setAttribute(ExifInterface.TAG_CONTRAST, "0");

                // Type is String.
                exifInterface.setAttribute(ExifInterface.TAG_COPYRIGHT, "");

                // Type is int.
                exifInterface.setAttribute(ExifInterface.TAG_CUSTOM_RENDERED, "0");

                // Type is String.
                exifInterface.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, "");

                // Type is String.
                exifInterface.setAttribute(ExifInterface.TAG_DEVICE_SETTING_DESCRIPTION, "");

                // Type is Double.
                exifInterface.setAttribute(ExifInterface.TAG_DIGITAL_ZOOM_RATIO, "0.0");

                // Type is String.
                exifInterface.setAttribute(ExifInterface.TAG_EXIF_VERSION, "");

                // Type is Double.
                exifInterface.setAttribute(ExifInterface.TAG_EXPOSURE_BIAS_VALUE, "0.0");

                // Type is rational.
                exifInterface.setAttribute(ExifInterface.TAG_EXPOSURE_INDEX, "NaN");

                // Type is int.
                exifInterface.setAttribute(ExifInterface.TAG_EXPOSURE_MODE, "0");

                // Type is int.
                exifInterface.setAttribute(ExifInterface.TAG_EXPOSURE_PROGRAM, "0");

                // Type is String.
                exifInterface.setAttribute(ExifInterface.TAG_FILE_SOURCE, "");

                // Type is String.
                exifInterface.setAttribute(ExifInterface.TAG_FLASHPIX_VERSION, "");

                // Type is rational.
                exifInterface.setAttribute(ExifInterface.TAG_FLASH_ENERGY, "NaN");

                // Type is int.
                exifInterface.setAttribute(ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM, "0");

                // Type is int.
                exifInterface.setAttribute(ExifInterface.TAG_FOCAL_PLANE_RESOLUTION_UNIT, "0");

                // Type is rational.
                exifInterface.setAttribute(ExifInterface.TAG_FOCAL_PLANE_X_RESOLUTION, "NaN");

                // Type is rational.
                exifInterface.setAttribute(ExifInterface.TAG_FOCAL_PLANE_Y_RESOLUTION, "NaN");

                // Type is Double.
                exifInterface.setAttribute(ExifInterface.TAG_F_NUMBER, "0.0");

                // Type is int.
                exifInterface.setAttribute(ExifInterface.TAG_GAIN_CONTROL, "0");

                // Type is String.
                exifInterface.setAttribute(ExifInterface.TAG_GPS_AREA_INFORMATION, "");

                // Type is rational.
                exifInterface.setAttribute(ExifInterface.TAG_GPS_DEST_BEARING, "NaN");

                // Type is String.
                exifInterface.setAttribute(ExifInterface.TAG_GPS_DEST_BEARING_REF, "");

                // Type is rational.
                exifInterface.setAttribute(ExifInterface.TAG_GPS_DEST_DISTANCE, "NaN");

                // Type is String.
                exifInterface.setAttribute(ExifInterface.TAG_GPS_DEST_DISTANCE_REF, "");

                // Type is rational.
                exifInterface.setAttribute(ExifInterface.TAG_GPS_DEST_LATITUDE, "NaN");

                // Type is String.
                exifInterface.setAttribute(ExifInterface.TAG_GPS_DEST_LATITUDE_REF, "");

                // Type is rational.
                exifInterface.setAttribute(ExifInterface.TAG_GPS_DEST_LONGITUDE, "NaN");

                // Type is String.
                exifInterface.setAttribute(ExifInterface.TAG_GPS_DEST_LONGITUDE_REF, "");

                // Type is int.
                exifInterface.setAttribute(ExifInterface.TAG_GPS_DIFFERENTIAL, "0");

                // Type is rational.
                exifInterface.setAttribute(ExifInterface.TAG_GPS_DOP, "NaN");

                // Type is rational.
                exifInterface.setAttribute(ExifInterface.TAG_GPS_IMG_DIRECTION, "NaN");

                // Type is rational.
                exifInterface.setAttribute(ExifInterface.TAG_GPS_IMG_DIRECTION_REF, "NaN");

                // Type is String.
                exifInterface.setAttribute(ExifInterface.TAG_GPS_MAP_DATUM, "");

                // Type is String.
                exifInterface.setAttribute(ExifInterface.TAG_GPS_MEASURE_MODE, "");

                // Type is String.
                exifInterface.setAttribute(ExifInterface.TAG_GPS_SATELLITES, "");

                // Type is rational.
                exifInterface.setAttribute(ExifInterface.TAG_GPS_SPEED, "NaN");

                // Type is rational.
                exifInterface.setAttribute(ExifInterface.TAG_GPS_SPEED_REF, "NaN");

                // Type is String.
                exifInterface.setAttribute(ExifInterface.TAG_GPS_STATUS, "");

                // Type is rational.
                exifInterface.setAttribute(ExifInterface.TAG_GPS_TRACK, "NaN");

                // Type is String.
                exifInterface.setAttribute(ExifInterface.TAG_GPS_TRACK_REF, "");

                // Type is String.
                exifInterface.setAttribute(ExifInterface.TAG_GPS_VERSION_ID, "");

                // Type is String.
                exifInterface.setAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION, "");

                // Type is String.
                exifInterface.setAttribute(ExifInterface.TAG_IMAGE_UNIQUE_ID, "");

                // Type is String.
                exifInterface.setAttribute(ExifInterface.TAG_INTEROPERABILITY_INDEX, "");

                // Type is int.
                exifInterface.setAttribute(ExifInterface.TAG_ISO_SPEED_RATINGS, "0");

                // Type is int.
                exifInterface.setAttribute(ExifInterface.TAG_JPEG_INTERCHANGE_FORMAT, "0");

                // Type is int.
                exifInterface.setAttribute(ExifInterface.TAG_JPEG_INTERCHANGE_FORMAT_LENGTH, "0");

                // Type is int.
                exifInterface.setAttribute(ExifInterface.TAG_LIGHT_SOURCE, "0");

                // Type is String.
                exifInterface.setAttribute(ExifInterface.TAG_MAKER_NOTE, "");

                // Type is rational.
                exifInterface.setAttribute(ExifInterface.TAG_MAX_APERTURE_VALUE, "NaN");

                // Type is int.
                exifInterface.setAttribute(ExifInterface.TAG_METERING_MODE, "0");

                // Type is String.
                exifInterface.setAttribute(ExifInterface.TAG_OECF, "");

                // Type is int.
                exifInterface.setAttribute(ExifInterface.TAG_PHOTOMETRIC_INTERPRETATION, "0");

                // Type is int.
                exifInterface.setAttribute(ExifInterface.TAG_PIXEL_X_DIMENSION, "0");

                // Type is int.
                exifInterface.setAttribute(ExifInterface.TAG_PIXEL_Y_DIMENSION, "0");

                // Type is int.
                exifInterface.setAttribute(ExifInterface.TAG_PLANAR_CONFIGURATION, "0");

                // Type is String.
                exifInterface.setAttribute(ExifInterface.TAG_PRIMARY_CHROMATICITIES, "");

                // Type is rational.
                exifInterface.setAttribute(ExifInterface.TAG_REFERENCE_BLACK_WHITE, "NaN");

                // Type is String.
                exifInterface.setAttribute(ExifInterface.TAG_RELATED_SOUND_FILE, "");

                // Type is int.
                exifInterface.setAttribute(ExifInterface.TAG_RESOLUTION_UNIT, "0");

                // Type is int.
                exifInterface.setAttribute(ExifInterface.TAG_ROWS_PER_STRIP, "0");

                // Type is int.
                exifInterface.setAttribute(ExifInterface.TAG_SAMPLES_PER_PIXEL, "0");

                // Type is int.
                exifInterface.setAttribute(ExifInterface.TAG_SATURATION, "0");

                // Type is int.
                exifInterface.setAttribute(ExifInterface.TAG_SCENE_CAPTURE_TYPE, "0");

                // Type is String.
                exifInterface.setAttribute(ExifInterface.TAG_SCENE_TYPE, "");

                // Type is int.
                exifInterface.setAttribute(ExifInterface.TAG_SENSING_METHOD, "0");

                // Type is int.
                exifInterface.setAttribute(ExifInterface.TAG_SHARPNESS, "0");

                // Type is rational.
                exifInterface.setAttribute(ExifInterface.TAG_SHUTTER_SPEED_VALUE, "NaN");

                // Type is String.
                exifInterface.setAttribute(ExifInterface.TAG_SOFTWARE, "");

                // Type is String.
                exifInterface.setAttribute(ExifInterface.TAG_SPATIAL_FREQUENCY_RESPONSE, "");

                // Type is String.
                exifInterface.setAttribute(ExifInterface.TAG_SPECTRAL_SENSITIVITY, "");

                // Type is int.
                exifInterface.setAttribute(ExifInterface.TAG_STRIP_BYTE_COUNTS, "0");

                // Type is int.
                exifInterface.setAttribute(ExifInterface.TAG_STRIP_OFFSETS, "0");

                // Type is int.
                exifInterface.setAttribute(ExifInterface.TAG_SUBJECT_AREA, "0");

                // Type is Double.
                exifInterface.setAttribute(ExifInterface.TAG_SUBJECT_DISTANCE, "0.0");

                // Type is int.
                exifInterface.setAttribute(ExifInterface.TAG_SUBJECT_DISTANCE_RANGE, "0");

                // Type is int.
                exifInterface.setAttribute(ExifInterface.TAG_SUBJECT_LOCATION, "0");

                // Type is String.
                exifInterface.setAttribute(ExifInterface.TAG_SUBSEC_TIME_DIGITIZED, "");

                // Type is String.
                exifInterface.setAttribute(ExifInterface.TAG_SUBSEC_TIME_ORIGINAL, "");

                // Type is int.
                exifInterface.setAttribute(ExifInterface.TAG_THUMBNAIL_IMAGE_LENGTH, "0");

                // Type is int.
                exifInterface.setAttribute(ExifInterface.TAG_THUMBNAIL_IMAGE_WIDTH, "0");

                // Type is int.
                exifInterface.setAttribute(ExifInterface.TAG_TRANSFER_FUNCTION, "0");

                // Type is String.
                exifInterface.setAttribute(ExifInterface.TAG_USER_COMMENT, "");

                // Type is rational.
                exifInterface.setAttribute(ExifInterface.TAG_WHITE_POINT, "NaN");

                // Type is rational.
                exifInterface.setAttribute(ExifInterface.TAG_X_RESOLUTION, "NaN");

                // Type is rational.
                exifInterface.setAttribute(ExifInterface.TAG_Y_CB_CR_COEFFICIENTS, "NaN");

                // Type is int.
                exifInterface.setAttribute(ExifInterface.TAG_Y_CB_CR_POSITIONING, "0");

                // Type is int.
                exifInterface.setAttribute(ExifInterface.TAG_Y_CB_CR_SUB_SAMPLING, "0");

                // Type is rational.
                exifInterface.setAttribute(ExifInterface.TAG_Y_RESOLUTION, "NaN");
            }

            if (Build.VERSION.SDK_INT >= 26) {

                // Type is int.
                exifInterface.setAttribute(ExifInterface.TAG_DEFAULT_CROP_SIZE, "0");

                // Type is int.
                exifInterface.setAttribute(ExifInterface.TAG_DNG_VERSION, "0");

                // Type is int.
                exifInterface.setAttribute(ExifInterface.TAG_NEW_SUBFILE_TYPE, "0");

                // Type is int.
                exifInterface.setAttribute(ExifInterface.TAG_ORF_ASPECT_FRAME, "0");

                // Type is int.
                exifInterface.setAttribute(ExifInterface.TAG_ORF_PREVIEW_IMAGE_LENGTH, "0");

                // Type is int.
                exifInterface.setAttribute(ExifInterface.TAG_ORF_PREVIEW_IMAGE_START, "0");

                // Type is undefined.
                exifInterface.setAttribute(ExifInterface.TAG_ORF_THUMBNAIL_IMAGE, "undefined");

                // Type is int.
                exifInterface.setAttribute(ExifInterface.TAG_RW2_ISO, "0");

                // Type is undefined.
                exifInterface.setAttribute(ExifInterface.TAG_RW2_JPG_FROM_RAW, "undefined");

                // Type is int.
                exifInterface.setAttribute(ExifInterface.TAG_RW2_SENSOR_BOTTOM_BORDER, "0");

                // Type is int.
                exifInterface.setAttribute(ExifInterface.TAG_RW2_SENSOR_LEFT_BORDER, "0");

                // Type is int.
                exifInterface.setAttribute(ExifInterface.TAG_RW2_SENSOR_RIGHT_BORDER, "0");

                // Type is int.
                exifInterface.setAttribute(ExifInterface.TAG_RW2_SENSOR_TOP_BORDER, "0");

                // Type is int.
                exifInterface.setAttribute(ExifInterface.TAG_SUBFILE_TYPE, "0");
            }

            exifInterface.saveAttributes();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}