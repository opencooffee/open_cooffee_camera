package com.opencooffeecamera;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.opencooffeecamera.library.CustomTextView;
import com.opencooffeecamera.library.ExternalStorage;
import com.opencooffeecamera.utils.RuntimePermissionUtils;
import com.opencooffeecamera.utils.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class CooffeeCameraResult extends AppCompatActivity {

    //private static final String LOG_TAG = CooffeeCameraResult.class.getSimpleName();

    private Context context;

    private Toast messageToast;

    private ExternalStorage externalStorage;

    private Rect rect;

    private GestureDetector gestureDetector;

    private View decorView;
    private ImageButton backImageButton, cancelImageButton, saveImageButton, shareImageButton;
    private ImageView previewImageView;
    private CustomTextView toastTextView;

    // Is hide system UI?
    private boolean isHideSystemUI = true;

    // If the user had to change the write external storage permissions from the configuration.
    private boolean isGoToSettingsForWriteExternalStoragePermission = false;

    // If the multimedia file is saved in the device's memory, it will be set to true.
    private boolean isMediaFileAlreadySaved = false;

    // Time that elapses from when is asked to go to the configuration to activate storage permission until it is returned from it.
    private long requestStoragePermissionTimestamp = 0;

    private String mediaFileType, path;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.cooffee_camera_result);

        Bundle extras = getIntent().getExtras();

        if (extras != null && extras.containsKey("type") && extras.getString("type") != null) {

            mediaFileType = extras.getString("type");

            context = getApplicationContext();

            externalStorage = new ExternalStorage(Common.ALBUM_NAME);

            if (mediaFileType != null && mediaFileType.equals(Common.TYPE_IMAGE)) {

                if (extras.containsKey("path") && extras.getString("path") != null) {

                    path = extras.getString("path");

                    initFields();
                    initCallbacks();

                    setBitmapFromFilePath(path);

                } else {
                    finish();
                }

            } else {
                finish();
            }

        }

    }

    @Override
    protected void onResume() {
        super.onResume();

        if (Build.VERSION.SDK_INT >= 23 && isGoToSettingsForWriteExternalStoragePermission) {

            isGoToSettingsForWriteExternalStoragePermission = false;

            if (checkSelfPermission(RuntimePermissionUtils.PERMISSION_WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                savePhotoInDeviceMemory();
            }

        }

        Utils.hideSystemUI(decorView);
    }

    @Override
    public void onDestroy() {

        File file = new File(path);

        if (file.exists()) {
           file.delete();
        }

        super.onDestroy();
    }

    /**
     * Listening for Fling Gestures.
     */
    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        return gestureDetector.onTouchEvent(motionEvent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        switch (requestCode) {

            case RuntimePermissionUtils.REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION:

                if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(RuntimePermissionUtils.PERMISSION_WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    savePhotoInDeviceMemory();
                } else {

                    if (!ActivityCompat.shouldShowRequestPermissionRationale(CooffeeCameraResult.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                        if ((Utils.getDeviceCurrentTimestamp() - requestStoragePermissionTimestamp) <= Common.RESPONSE_TIMEOUT) {

                            requestStoragePermissionTimestamp = 0;

                            RuntimePermissionUtils.showMessage(
                                    CooffeeCameraResult.this,
                                    getResources().getString(R.string.storage_runtime_permission_text),
                                    new DialogInterface.OnClickListener() {

                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {

                                            if (which == -1) {
                                                isGoToSettingsForWriteExternalStoragePermission = true;
                                                RuntimePermissionUtils.goToSettings(CooffeeCameraResult.this);
                                            }

                                        }

                                    }
                            );

                        }

                    }

                }

                break;

            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }

    }

    private void initFields() {

        decorView = getWindow().getDecorView();

        previewImageView = findViewById(R.id.preview); // ImageView

        backImageButton = findViewById(R.id.back); // ImageButton

        cancelImageButton = findViewById(R.id.cancel); // ImageButton

        saveImageButton = findViewById(R.id.save); // ImageButton

        shareImageButton = findViewById(R.id.share); // ImageButton

        // Customize the toast to show.
        LayoutInflater layoutInflater = getLayoutInflater();

        View toastLayout = layoutInflater.inflate(R.layout.toast, (ViewGroup) findViewById(R.id.toast_linear_layout));

        toastTextView = toastLayout.findViewById(R.id.toast); // CustomTextView

        messageToast = new Toast(this);
        messageToast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
        messageToast.setDuration(Toast.LENGTH_SHORT);
        messageToast.setView(toastLayout);
    }

    /**
     * Start listening to events.
     */
    private void initCallbacks() {

        gestureDetector = new GestureDetector(context, new GestureDetector.OnGestureListener() {

            @Override
            public boolean onDown(MotionEvent motionEvent) {

                if (isHideSystemUI) {
                    Utils.showSystemUI(decorView);
                    isHideSystemUI = false;
                } else {
                    Utils.hideSystemUI(decorView);
                    isHideSystemUI = true;
                }

                return false;
            }

            @Override
            public void onShowPress(MotionEvent motionEvent) {
                // Empty.
            }

            @Override
            public boolean onSingleTapUp(MotionEvent motionEvent) {
                return false;
            }

            @Override
            public boolean onScroll(MotionEvent motionEvent1, MotionEvent motionEvent2, float distanceX, float distanceY) {
                return false;
            }

            @Override
            public void onLongPress(MotionEvent motionEvent) {
                // Empty.
            }

            @Override
            public boolean onFling(MotionEvent motionEvent1, MotionEvent motionEvent2, float velocityX, float velocityY) {
                return false;
            }

        });

        // Back event listener.

        backImageButton.setOnTouchListener(new View.OnTouchListener() {

            ArrayList<ImageButton> previousTouchedImageButton = new ArrayList<>();

            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {

                    // Construir un Rect de los límites de la vista.
                    rect = new Rect(view.getLeft(), view.getTop(), view.getRight(), view.getBottom());

                    if (previousTouchedImageButton.size() == 0) {

                        previousTouchedImageButton.add(saveImageButton);

                        int width = backImageButton.getWidth();
                        int height = backImageButton.getHeight();

                        width += 12;
                        height += 12;

                        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(width, height);

                        params.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
                        params.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);

                        int backImageButtonLayoutMarginDp = 36;

                        int backImageButtonLayoutMarginPx = (int) Utils.convertDpToPixel(context, backImageButtonLayoutMarginDp);

                        params.setMargins(backImageButtonLayoutMarginPx - 6, backImageButtonLayoutMarginPx - 6, 0, 0);

                        backImageButton.setLayoutParams(params);
                    }

                    return true;

                } else if (motionEvent.getAction() == MotionEvent.ACTION_MOVE) {

                    if (!rect.contains(view.getLeft() + (int) motionEvent.getX(), view.getTop() + (int) motionEvent.getY())) {

                        if (previousTouchedImageButton.size() > 0) {
                            decreaseBackImageButton();
                            previousTouchedImageButton.remove(previousTouchedImageButton.size() - 1);
                        }

                    }

                    return true;

                } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {

                    if (rect.contains(view.getLeft() + (int) motionEvent.getX(), view.getTop() + (int) motionEvent.getY())) {

                        if (previousTouchedImageButton.size() > 0) {

                            decreaseBackImageButton();

                            previousTouchedImageButton.remove(previousTouchedImageButton.size() - 1);

                            finish();
                        }

                    }

                    return true;

                } else if (motionEvent.getAction() == MotionEvent.ACTION_OUTSIDE || motionEvent.getAction() == MotionEvent.ACTION_CANCEL) {

                    if (previousTouchedImageButton.size() > 0) {
                        decreaseCancelImageButton();
                        previousTouchedImageButton.remove(previousTouchedImageButton.size() - 1);
                    }

                    return true;
                }

                return false;

            }

        });

        // Cancel event listener.

        cancelImageButton.setOnTouchListener(new View.OnTouchListener() {

            ArrayList<ImageButton> previousTouchedImageButton = new ArrayList<>();

            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {

                    // Construir un Rect de los límites de la vista.
                    rect = new Rect(view.getLeft(), view.getTop(), view.getRight(), view.getBottom());

                    if (previousTouchedImageButton.size() == 0) {

                        previousTouchedImageButton.add(saveImageButton);

                        int width = cancelImageButton.getWidth();
                        int height = cancelImageButton.getHeight();

                        width += 12;
                        height += 12;

                        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(width, height);

                        params.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
                        params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);

                        int cancelImageButtonLayoutMarginDp = 36;

                        int cancelImageButtonLayoutMarginPx = (int) Utils.convertDpToPixel(context, cancelImageButtonLayoutMarginDp);

                        params.setMargins(0, cancelImageButtonLayoutMarginPx - 6, cancelImageButtonLayoutMarginPx - 6, 0);

                        cancelImageButton.setLayoutParams(params);
                    }

                    return true;

                } else if (motionEvent.getAction() == MotionEvent.ACTION_MOVE) {

                    if (!rect.contains(view.getLeft() + (int) motionEvent.getX(), view.getTop() + (int) motionEvent.getY())) {

                        if (previousTouchedImageButton.size() > 0) {
                            decreaseCancelImageButton();
                            previousTouchedImageButton.remove(previousTouchedImageButton.size() - 1);
                        }

                    }

                    return true;

                } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {

                    if (rect.contains(view.getLeft() + (int) motionEvent.getX(), view.getTop() + (int) motionEvent.getY())) {

                        if (previousTouchedImageButton.size() > 0) {

                            decreaseCancelImageButton();

                            previousTouchedImageButton.remove(previousTouchedImageButton.size() - 1);

                            Intent returnIntent = new Intent();

                            returnIntent.putExtra(Common.FINISH, Common.FINISH);

                            setResult(RESULT_OK, returnIntent);

                            finish();
                        }

                    }

                    return true;

                } else if (motionEvent.getAction() == MotionEvent.ACTION_OUTSIDE || motionEvent.getAction() == MotionEvent.ACTION_CANCEL) {

                    if (previousTouchedImageButton.size() > 0) {
                        decreaseCancelImageButton();
                        previousTouchedImageButton.remove(previousTouchedImageButton.size() - 1);
                    }

                    return true;
                }

                return false;
            }

        });

        // Save event listener.

        saveImageButton.setOnTouchListener(new View.OnTouchListener() {

            ArrayList<ImageButton> previousTouchedImageButton = new ArrayList<>();

            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {

                    // Construir un Rect de los límites de la vista.
                    rect = new Rect(view.getLeft(), view.getTop(), view.getRight(), view.getBottom());

                    if (previousTouchedImageButton.size() == 0) {

                        previousTouchedImageButton.add(saveImageButton);

                        int width = saveImageButton.getWidth();
                        int height = saveImageButton.getHeight();

                        width += 12;
                        height += 12;

                        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(width, height);

                        params.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
                        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);

                        int saveImageButtonLayoutMarginDp = 56;

                        int saveImageButtonLayoutMarginPx = (int) Utils.convertDpToPixel(context, saveImageButtonLayoutMarginDp);

                        params.setMargins(saveImageButtonLayoutMarginPx - 6, 0, 0, saveImageButtonLayoutMarginPx - 6);

                        saveImageButton.setLayoutParams(params);
                    }

                    return true;

                } else if (motionEvent.getAction() == MotionEvent.ACTION_MOVE) {

                    if (!rect.contains(view.getLeft() + (int) motionEvent.getX(), view.getTop() + (int) motionEvent.getY())) {

                        if (previousTouchedImageButton.size() > 0) {
                            decreaseSaveImageButton();
                            previousTouchedImageButton.remove(previousTouchedImageButton.size() - 1);
                        }

                    }

                    return true;

                } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {

                    if (rect.contains(view.getLeft() + (int) motionEvent.getX(), view.getTop() + (int) motionEvent.getY())) {

                        if (previousTouchedImageButton.size() > 0) {

                            decreaseSaveImageButton();

                            previousTouchedImageButton.remove(previousTouchedImageButton.size() - 1);

                            if (Build.VERSION.SDK_INT >= 23) {

                                if (checkSelfPermission(RuntimePermissionUtils.PERMISSION_WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                                    requestStoragePermissionTimestamp = Utils.getDeviceCurrentTimestamp();

                                    requestPermissions(
                                            new String[]{
                                                    RuntimePermissionUtils.PERMISSION_WRITE_EXTERNAL_STORAGE
                                            },
                                            RuntimePermissionUtils.REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION
                                    );

                                } else {
                                    savePhotoInDeviceMemory();
                                }

                            } else {
                                savePhotoInDeviceMemory();
                            }

                        }

                    }

                    return true;

                } else if (motionEvent.getAction() == MotionEvent.ACTION_OUTSIDE || motionEvent.getAction() == MotionEvent.ACTION_CANCEL) {

                    if (previousTouchedImageButton.size() > 0) {
                        decreaseSaveImageButton();
                        previousTouchedImageButton.remove(previousTouchedImageButton.size() - 1);
                    }

                    return true;
                }

                return false;
            }

        });

        // Share event listener.

        shareImageButton.setOnTouchListener(new View.OnTouchListener() {

            ArrayList<ImageButton> previousTouchedImageButton = new ArrayList<>();

            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {

                    // Construir un Rect de los límites de la vista.
                    rect = new Rect(view.getLeft(), view.getTop(), view.getRight(), view.getBottom());

                    if (previousTouchedImageButton.size() == 0) {

                        previousTouchedImageButton.add(shareImageButton);

                        int width = shareImageButton.getWidth();
                        int height = shareImageButton.getHeight();

                        width += 12;
                        height += 12;

                        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(width, height);

                        params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
                        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);

                        int shareImageButtonLayoutMarginDp = 56;

                        int shareImageButtonLayoutMarginPx = (int) Utils.convertDpToPixel(context, shareImageButtonLayoutMarginDp);

                        params.setMargins(0, 0, shareImageButtonLayoutMarginPx - 6, shareImageButtonLayoutMarginPx - 6);

                        shareImageButton.setLayoutParams(params);
                    }

                    return true;

                } else if (motionEvent.getAction() == MotionEvent.ACTION_MOVE) {

                    if (!rect.contains(view.getLeft() + (int) motionEvent.getX(), view.getTop() + (int) motionEvent.getY())) {

                        if (previousTouchedImageButton.size() > 0) {
                            decreaseShareImageButton();
                            previousTouchedImageButton.remove(previousTouchedImageButton.size() - 1);
                        }

                    }

                    return true;

                } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {

                    if (rect.contains(view.getLeft() + (int) motionEvent.getX(), view.getTop() + (int) motionEvent.getY())) {

                        if (previousTouchedImageButton.size() > 0) {

                            decreaseShareImageButton();
                            previousTouchedImageButton.remove(previousTouchedImageButton.size() - 1);

                            File srcFile = new File(path);

                            if (Build.VERSION.SDK_INT >= 24) {

                                Uri shareFileUri = FileProvider.getUriForFile(context, Common.PROVIDER, srcFile);

                                Intent shareIntent = ShareCompat.IntentBuilder.from(CooffeeCameraResult.this).setStream(shareFileUri).getIntent();

                                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                                shareIntent.setDataAndType(shareFileUri, getContentResolver().getType(shareFileUri));

                                // Give permissions individually to all apps on the device.
                                List<ResolveInfo> resolvedInfoActivities = getPackageManager().queryIntentActivities(shareIntent, PackageManager.MATCH_DEFAULT_ONLY);

                                for (ResolveInfo ri : resolvedInfoActivities) {
                                    context.grantUriPermission(ri.activityInfo.packageName, shareFileUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                }

                                startActivity(Intent.createChooser(shareIntent, Common.APP_NAME));

                            } else {

                                Intent shareIntent = new Intent(Intent.ACTION_SEND);

                                shareIntent.setType("image/*");

                                Uri uri = Uri.fromFile(srcFile);

                                shareIntent.putExtra(Intent.EXTRA_STREAM, uri);

                                startActivity(Intent.createChooser(shareIntent, Common.APP_NAME));
                            }

                        }

                    }

                    return true;

                } else if (motionEvent.getAction() == MotionEvent.ACTION_OUTSIDE || motionEvent.getAction() == MotionEvent.ACTION_CANCEL) {

                    if (previousTouchedImageButton.size() > 0) {
                        decreaseShareImageButton();
                        previousTouchedImageButton.remove(previousTouchedImageButton.size() - 1);
                    }

                    return true;
                }

                return false;
            }

        });

    }

    /**
     * Decrease the size of backImageButton.
     */
    private void decreaseBackImageButton() {

        int backImageButtonLayoutWidthDp = 40;

        int backImageButtonLayoutWidthPx = (int) Utils.convertDpToPixel(context, backImageButtonLayoutWidthDp);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(backImageButtonLayoutWidthPx, backImageButtonLayoutWidthPx);

        params.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
        params.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);

        int backImageButtonLayoutMarginDp = 36;

        int backImageButtonLayoutMarginPx = (int) Utils.convertDpToPixel(context, backImageButtonLayoutMarginDp);

        params.setMargins(backImageButtonLayoutMarginPx, backImageButtonLayoutMarginPx, 0, 0);

        backImageButton.setLayoutParams(params);
    }

    /**
     * Decrease the size of cancelImageButton.
     */
    private void decreaseCancelImageButton() {

        int cancelImageButtonLayoutWidthDp = 40;

        int cancelImageButtonLayoutWidthPx = (int) Utils.convertDpToPixel(context, cancelImageButtonLayoutWidthDp);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(cancelImageButtonLayoutWidthPx, cancelImageButtonLayoutWidthPx);

        params.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
        params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);

        int cancelImageButtonLayoutMarginDp = 36;

        int cancelImageButtonLayoutMarginPx = (int) Utils.convertDpToPixel(context, cancelImageButtonLayoutMarginDp);

        params.setMargins(0, cancelImageButtonLayoutMarginPx, cancelImageButtonLayoutMarginPx, 0);

        cancelImageButton.setLayoutParams(params);
    }

    /**
     * Decrease the size of saveImageButton.
     */
    private void decreaseSaveImageButton() {

        int saveImageButtonLayoutWidthDp = 40;

        int saveImageButtonLayoutWidthPx = (int) Utils.convertDpToPixel(context, saveImageButtonLayoutWidthDp);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(saveImageButtonLayoutWidthPx, saveImageButtonLayoutWidthPx);

        params.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);

        int saveImageButtonLayoutMarginDp = 56;

        int saveImageButtonLayoutMarginPx = (int) Utils.convertDpToPixel(context, saveImageButtonLayoutMarginDp);

        params.setMargins(saveImageButtonLayoutMarginPx, 0, 0, saveImageButtonLayoutMarginPx);

        saveImageButton.setLayoutParams(params);
    }

    /**
     * Decrease the size of shareImageButton.
     */
    private void decreaseShareImageButton() {

        int shareImageButtonLayoutWidthDp = 40;

        int shareImageButtonLayoutWidthPx = (int) Utils.convertDpToPixel(context, shareImageButtonLayoutWidthDp);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(shareImageButtonLayoutWidthPx, shareImageButtonLayoutWidthPx);

        params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);

        int shareImageButtonLayoutMarginDp = 56;

        int shareImageButtonLayoutMarginPx = (int) Utils.convertDpToPixel(context, shareImageButtonLayoutMarginDp);

        params.setMargins(0, 0, shareImageButtonLayoutMarginPx, shareImageButtonLayoutMarginPx);

        shareImageButton.setLayoutParams(params);
    }

    /**
     * Get Bitmap from a path.
     * Do not modify the algorithm to obtain the Bitmap.
     *
     * @param path
     * Path of the image File.
     */
    private void setBitmapFromFilePath(String path) {

        try {

            Bitmap bitmap = BitmapFactory.decodeFile(path);

            if (bitmap != null) {
                previewImageView.setImageBitmap(bitmap);
            }

        } catch (NullPointerException e) {
            e.printStackTrace();
        }

    }

    /**
     * Create a unique name for the file based on the current time.
     * Save the file with that name.
     * Show the result message.
     */
    private void savePhotoInDeviceMemory() {

        if (!isMediaFileAlreadySaved) {

            String mediaFileName = Utils.getMediaFileNameByCurrentTime(mediaFileType);

            File srcFile = new File(path);

            File dstFile = externalStorage.getPublicAlbumFile(mediaFileName);

            boolean isCopied = Utils.copyFile(srcFile, dstFile);

            if (isCopied) {

                isMediaFileAlreadySaved = true;

                toastTextView.setText(getResources().getString(R.string.saved_photo));

            } else {
                toastTextView.setText(getResources().getString(R.string.error_saving_photo));
            }

        } else {
            toastTextView.setText(getResources().getString(R.string.photo_already_saved));
        }

        messageToast.show();
    }

}