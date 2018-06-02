package com.opencooffeecamera;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.opencooffeecamera.application.OpenCooffeeCameraApplication;
import com.opencooffeecamera.library.CustomTextView;
import com.opencooffeecamera.library.FileCache;
import com.opencooffeecamera.utils.RuntimePermissionUtils;
import com.opencooffeecamera.utils.Utils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class CooffeeCamera extends AppCompatActivity implements SurfaceHolder.Callback {

    private static final String LOG_TAG = CooffeeCamera.class.getSimpleName();

    private static final int MEDIA_TYPE_IMAGE = 1;
    private static final int CAMERA_RESULT_REQUEST_CODE = 2;
    private static final int JPEG_QUALITY = 100;

    private Context context;

    private Toast messageToast;

    private Rect rect;

    private Camera camera;

    private SurfaceHolder surfaceHolder;

    private ProgressDialog progressDialog;

    private View decorView;

    private ImageButton changeFlashModeImageButton, changeCameraModeImageButton, takePictureImageButton;

    private CustomTextView toastTextView;

    private File mediaFile;

    // Does the device have a flash?
    private boolean isFlashMode = false;

    // Does the device have a front camera?
    private boolean isFrontCamera = false;

    // Is the camera in preview?
    private boolean isPreview = false;

    // Is the camera in flash mode?
    private boolean isCameraFlashMode = false;

    // Is the camera in front?
    private boolean isCameraFrontCamera = false;

    // It will serve to detect if the onPause() is induced by the user or by the AppCompatActivity.
    // Initialize the inducedOnPause to 'false'.
    private boolean inducedOnPause = false;

    // Is hide system UI?
    private boolean isHideSystemUI = true;

    // If the user had to change the camera permissions from the configuration.
    private boolean isGoToSettingsForCameraPermission = false;

    // Time that elapses from when is asked to go to the configuration to activate camera permission until it is returned from it.
    private long requestCameraPermissionTimestamp = 0;

    // Default initial value of the number of cameras available.
    private int foundId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.cooffee_camera);

        context = getApplicationContext();

        // If the AppCompatActivity passes through the onCreate().
        // It has not been an onPause() caused by the user.
        inducedOnPause = false;

        if (Build.VERSION.SDK_INT >= 23) {

            if (checkSelfPermission(RuntimePermissionUtils.PERMISSION_CAMERA) != PackageManager.PERMISSION_GRANTED) {

                requestCameraPermissionTimestamp = Utils.getDeviceCurrentTimestamp();

                requestPermissions(
                        new String[]{
                                RuntimePermissionUtils.PERMISSION_CAMERA
                        },
                        RuntimePermissionUtils.REQUEST_CAMERA_PERMISSION
                );

            } else {
                initCooffeeCamera();
            }

        } else {
            initCooffeeCamera();
        }

    }

    @Override
    protected void onResume() {
        super.onResume();

        if (Build.VERSION.SDK_INT >= 23 && isGoToSettingsForCameraPermission) {

            isGoToSettingsForCameraPermission = false;

            if (checkSelfPermission(RuntimePermissionUtils.PERMISSION_CAMERA) == PackageManager.PERMISSION_GRANTED) {
                initCooffeeCamera();
            }

        }

        // If the onResume() comes from being induced to pause the AppCompatActivity.
        if (inducedOnPause) {
            rebootCooffeeCamera();
        }

        if (decorView != null) {
            Utils.hideSystemUI(decorView);
        }

    }

    @Override
    protected void onPause() {

        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
            progressDialog = null;
        }

        if (camera != null) {

            camera.stopPreview();
            camera.release();
            camera = null;
            isPreview = false;

            // Change the Boolean value to true.
            // At the moment we do not know if the onPause() is induced by the user or by the AppCompatActivity.
            inducedOnPause = true;
        }

        super.onPause();
    }

    @Override
    protected void onDestroy() {

        if (camera != null) {
            camera.stopPreview();
            camera.release();
            camera = null;
            isPreview = false;
        }

        super.onDestroy();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null) {

            Bundle extras = data.getExtras();

            if (extras != null && extras.containsKey(Common.FINISH)) {
                finish();
            }

        }

        if (resultCode == RESULT_CANCELED) {}

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        switch (requestCode) {

            case RuntimePermissionUtils.REQUEST_CAMERA_PERMISSION:

                if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(RuntimePermissionUtils.PERMISSION_CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    initCooffeeCamera();
                } else {

                    if (!ActivityCompat.shouldShowRequestPermissionRationale(CooffeeCamera.this, Manifest.permission.CAMERA)) {

                        if ((Utils.getDeviceCurrentTimestamp() - requestCameraPermissionTimestamp) <= Common.RESPONSE_TIMEOUT) {

                            requestCameraPermissionTimestamp = 0;

                            RuntimePermissionUtils.showMessage(
                                    CooffeeCamera.this,
                                    getResources().getString(R.string.camera_runtime_permission_text),
                                    new DialogInterface.OnClickListener() {

                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {

                                            if (which == -1) {
                                                isGoToSettingsForCameraPermission = true;
                                                RuntimePermissionUtils.goToSettings(CooffeeCamera.this);
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

        CooffeeCameraSurfaceView cooffeeCameraSurfaceView = findViewById(R.id.cooffee_camera_preview); // CooffeeCameraSurfaceView

        surfaceHolder = cooffeeCameraSurfaceView.getHolder();
        surfaceHolder.addCallback(this);

        LayoutInflater controlInflater = LayoutInflater.from(this);

        View viewControl = controlInflater.inflate(R.layout.cooffee_camera_controls, (ViewGroup) findViewById(R.id.cooffee_camera_id), false);

        LayoutParams layoutParamsControl = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

        this.addContentView(viewControl, layoutParamsControl);

        changeFlashModeImageButton = findViewById(R.id.change_flash_mode); // ImageButton

        changeCameraModeImageButton = findViewById(R.id.change_camera_mode); // ImageButton

        takePictureImageButton = findViewById(R.id.take_picture); // ImageButton

        // Customize the toast to show.
        LayoutInflater layoutInflater = getLayoutInflater();

        View toastLayout = layoutInflater.inflate(R.layout.toast, (ViewGroup) findViewById(R.id.toast_linear_layout));

        toastTextView = toastLayout.findViewById(R.id.toast); // CustomTextView

        messageToast = new Toast(this);
        messageToast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
        messageToast.setDuration(Toast.LENGTH_SHORT);
        messageToast.setView(toastLayout);
    }

    private void initCallbacks() {

        // Touch to change the flash mode (on / off).
        changeFlashModeImageButton.setOnTouchListener(new OnTouchListener() {

            ArrayList<ImageButton> previousTouchedImageButton = new ArrayList<>();

            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                if (camera != null && isFlashMode) {

                    if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {

                        // Build a Rect of the limits of view.
                        rect = new Rect(view.getLeft(), view.getTop(), view.getRight(), view.getBottom());

                        if (previousTouchedImageButton.size() == 0) {

                            previousTouchedImageButton.add(changeFlashModeImageButton);

                            int width = changeFlashModeImageButton.getWidth();
                            int height = changeFlashModeImageButton.getHeight();

                            width += 12;
                            height += 12;

                            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(width, height);

                            params.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
                            params.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);

                            int changeFlashModeImageButtonLayoutMarginDp = 36;

                            int changeFlashModeImageButtonLayoutMarginPx = (int) Utils.convertDpToPixel(context, changeFlashModeImageButtonLayoutMarginDp);

                            params.setMargins(changeFlashModeImageButtonLayoutMarginPx - 6, changeFlashModeImageButtonLayoutMarginPx - 6, 0, 0);

                            changeFlashModeImageButton.setLayoutParams(params);
                        }

                        return true;

                    } else if (motionEvent.getAction() == MotionEvent.ACTION_MOVE) {

                        if (!rect.contains(view.getLeft() + (int) motionEvent.getX(), view.getTop() + (int) motionEvent.getY())) {

                            if (previousTouchedImageButton.size() > 0) {
                                decreaseFlashModeImageButton();
                                previousTouchedImageButton.remove(previousTouchedImageButton.size() - 1);
                            }

                        }

                        return true;

                    } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {

                        if (rect.contains(view.getLeft() + (int) motionEvent.getX(), view.getTop() + (int) motionEvent.getY())) {

                            if (previousTouchedImageButton.size() > 0) {

                                decreaseFlashModeImageButton();

                                previousTouchedImageButton.remove(previousTouchedImageButton.size() - 1);

                                if (isCameraFlashMode) {

                                    // Show ic_flash_off drawable.
                                    changeFlashModeImageButton.setImageResource(R.drawable.ic_flash_off);

                                    OpenCooffeeCameraApplication.setIsCameraFlashMode(false);

                                    if (camera != null) {

                                        try {

                                            setCameraDisplayOrientation();

                                            Parameters parameters = camera.getParameters();

                                            // Check if the device has flash.
                                            if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
                                                // Set flash mode off.
                                                parameters.setFlashMode(Parameters.FLASH_MODE_OFF);
                                            }

                                            camera.setParameters(parameters);

                                            isPreview = true;

                                            camera.setPreviewDisplay(surfaceHolder);
                                            camera.startPreview();

                                            // Change isCameraFlashMode to 'false'.
                                            isCameraFlashMode = false;

                                        } catch (IOException e) {
                                            Log.e(LOG_TAG, "Could not start preview", e);
                                            camera.release();
                                            camera = null;
                                            isPreview = false;
                                        }

                                    }

                                } else {

                                    // Show ic_flash_on drawable.
                                    changeFlashModeImageButton.setImageResource(R.drawable.ic_flash_on);

                                    OpenCooffeeCameraApplication.setIsCameraFlashMode(true);

                                    if (camera != null) {

                                        try {

                                            setCameraDisplayOrientation();

                                            Parameters parameters = camera.getParameters();

                                            // Get all flash modes.
                                            List<String> flashModesList = parameters.getSupportedFlashModes();

                    						/*
                    						if (flashModesList != null) {
                    				    		Iterator<String> fmi = flashModesList.iterator();
                    					    	while (fmi.hasNext()) {
                    					        	String currentFlashModes = fmi.next();
                    					        	Log.v("Flash modes", "Checking " + currentFlashModes);
                    					    	}
                    				    	}*/

                                            // Check if the device has flash.
                                            if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {

                                                if (flashModesList == null) {
                                                    // Set flash mode to on.
                                                    parameters.setFlashMode(Parameters.FLASH_MODE_ON);
                                                } else if (flashModesList.contains(Parameters.FLASH_MODE_ON)) {
                                                    // Set flash mode to off.
                                                    parameters.setFlashMode(Parameters.FLASH_MODE_ON);
                                                }

                                            }

                                            camera.setParameters(parameters);

                                            isPreview = true;

                                            camera.setPreviewDisplay(surfaceHolder);
                                            camera.startPreview();

                                            // Change isCameraFlastMode to 'true'.
                                            isCameraFlashMode = true;

                                        } catch (IOException e) {
                                            Log.e(LOG_TAG, "Could not start preview", e);
                                            camera.release();
                                            camera = null;
                                            isPreview = false;
                                        }

                                    }

                                }

                            }

                        }

                        return true;

                    } else if (motionEvent.getAction() == MotionEvent.ACTION_OUTSIDE || motionEvent.getAction() == MotionEvent.ACTION_CANCEL) {

                        if (previousTouchedImageButton.size() > 0) {
                            decreaseFlashModeImageButton();
                            previousTouchedImageButton.remove(previousTouchedImageButton.size() - 1);
                        }

                        return true;
                    }

                }

                return false;
            }

        });

        // changeCameraModeImageButton event listener.

        changeCameraModeImageButton.setOnTouchListener(new OnTouchListener() {

            ArrayList<ImageButton> previousTouchedImageButton = new ArrayList<>();

            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                if (camera != null && isFrontCamera) {

                    if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {

                        // Build a Rect of the limits of view.
                        rect = new Rect(view.getLeft(), view.getTop(), view.getRight(), view.getBottom());

                        if (previousTouchedImageButton.size() == 0) {

                            previousTouchedImageButton.add(changeCameraModeImageButton);

                            int width = changeCameraModeImageButton.getWidth();
                            int height = changeCameraModeImageButton.getHeight();

                            width += 12;
                            height += 12;

                            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(width, height);

                            params.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
                            params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);

                            int changeCameraModeImageButtonLayoutMarginDp = 36;

                            int changeCameraModeImageButtonLayoutMarginPx = (int) Utils.convertDpToPixel(context, changeCameraModeImageButtonLayoutMarginDp);

                            params.setMargins(0, changeCameraModeImageButtonLayoutMarginPx - 6, changeCameraModeImageButtonLayoutMarginPx - 6, 0);

                            changeCameraModeImageButton.setLayoutParams(params);
                        }

                        return true;

                    } else if (motionEvent.getAction() == MotionEvent.ACTION_MOVE) {

                        if (!rect.contains(view.getLeft() + (int) motionEvent.getX(), view.getTop() + (int) motionEvent.getY())) {

                            if (previousTouchedImageButton.size() > 0) {
                                decreaseCameraModeImageButton();
                                previousTouchedImageButton.remove(previousTouchedImageButton.size() - 1);
                            }

                        }

                        return true;

                    } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {

                        if (rect.contains(view.getLeft() + (int) motionEvent.getX(), view.getTop() + (int) motionEvent.getY())) {

                            if (previousTouchedImageButton.size() > 0) {

                                changeCameraModeImageButton.setEnabled(false);

                                decreaseCameraModeImageButton();

                                previousTouchedImageButton.remove(previousTouchedImageButton.size() - 1);

                                if (camera != null) {
                                    camera.stopPreview();
                                    camera.release();
                                    camera = null;
                                    isPreview = false;
                                }

                                OpenCooffeeCameraApplication.setIsCameraFrontCamera(!isCameraFrontCamera);

                                rebootCooffeeCamera();
                            }

                        }

                        return true;

                    } else if (motionEvent.getAction() == MotionEvent.ACTION_OUTSIDE || motionEvent.getAction() == MotionEvent.ACTION_CANCEL) {

                        if (previousTouchedImageButton.size() > 0) {
                            decreaseCameraModeImageButton();
                            previousTouchedImageButton.remove(previousTouchedImageButton.size() - 1);
                        }

                        return true;
                    }

                }

                return false;
            }

        });

        // takePictureImageButton event listener.

        takePictureImageButton.setOnTouchListener(new OnTouchListener() {

            ArrayList<ImageButton> previousTouchedImageButton = new ArrayList<>();

            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                if (camera != null) {

                    if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {

                        // Build a Rect of the limits of view.
                        rect = new Rect(view.getLeft(), view.getTop(), view.getRight(), view.getBottom());

                        if (previousTouchedImageButton.size() == 0) {

                            previousTouchedImageButton.add(takePictureImageButton);

                            int width = takePictureImageButton.getWidth();
                            int height = takePictureImageButton.getHeight();

                            width += 16;
                            height += 16;

                            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(width, height);

                            params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
                            params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);

                            int takePictureImageButtonLayoutMarginDp = 48;

                            int takePictureImageButtonLayoutMarginPx = (int) Utils.convertDpToPixel(context, takePictureImageButtonLayoutMarginDp);

                            params.setMargins(0, 0, 0, takePictureImageButtonLayoutMarginPx - 8);

                            takePictureImageButton.setLayoutParams(params);
                        }

                        return true;

                    } else if (motionEvent.getAction() == MotionEvent.ACTION_MOVE) {

                        if (!rect.contains(view.getLeft() + (int) motionEvent.getX(), view.getTop() + (int) motionEvent.getY())) {

                            if (previousTouchedImageButton.size() > 0) {
                                decreaseTakePictureImageButton();
                                previousTouchedImageButton.remove(previousTouchedImageButton.size() - 1);
                            }

                        }

                        return true;

                    } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {

                        if (rect.contains(view.getLeft() + (int) motionEvent.getX(), view.getTop() + (int) motionEvent.getY())) {

                            if (previousTouchedImageButton.size() > 0) {

                                takePictureImageButton.setEnabled(false);

                                decreaseTakePictureImageButton();

                                previousTouchedImageButton.remove(previousTouchedImageButton.size() - 1);

                                camera.takePicture(null, null, myPictureCallback_JPEG);
                            }

                        }

                        return true;

                    } else if (motionEvent.getAction() == MotionEvent.ACTION_OUTSIDE || motionEvent.getAction() == MotionEvent.ACTION_CANCEL) {

                        if (previousTouchedImageButton.size() > 0) {
                            decreaseTakePictureImageButton();
                            previousTouchedImageButton.remove(previousTouchedImageButton.size() - 1);
                        }

                        return true;
                    }

                }

                return false;
            }

        });

    }

    /**
     * Initiate Cooffee Camera.
     */
    private void initCooffeeCamera() {
        initFields();
        initCallbacks();
        initBrightness();
        initCamera();
    }

    /**
     * Initiate the brightness at 100%.
     */
    private void initBrightness() {

        if (Build.VERSION.SDK_INT >= 23 && Settings.System.canWrite(this)) {

            // Value in % of the screen brightness.
            // Set 100% brightness.
            float brightnessValue = 1F;

            // Set the brightness level of the screen.
            setBrightness(brightnessValue);
        }

    }

    /**
     * Check the available cameras and start the corresponding one.
     */
    private void initCamera() {

        if (checkCameraHardware()) {
            takePictureImageButton.setVisibility(View.VISIBLE);
        }

        // First search if the device has a front camera.
        foundId = findFirstFrontCamera();

        if (foundId != -1) {

            isFrontCamera = true;

            changeCameraModeImageButton.setVisibility(View.VISIBLE);

            isCameraFrontCamera = OpenCooffeeCameraApplication.getIsCameraFrontCamera();

            if (isCameraFrontCamera) {
                camera = openFrontCamera();
            } else {
                camera = Camera.open(0);
            }

        } else {
            isFrontCamera = false;
        }

    }

    /**
     * Decrease changeFlashModeImageButton.
     */
    private void decreaseFlashModeImageButton() {

        int changeFlashModeImageButtonLayoutWidthDp = 40;

        int changeFlashModeImageButtonLayoutWidthPx = (int) Utils.convertDpToPixel(context, changeFlashModeImageButtonLayoutWidthDp);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(changeFlashModeImageButtonLayoutWidthPx, changeFlashModeImageButtonLayoutWidthPx);

        params.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
        params.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);

        int changeFlashModeImageButtonLayoutMarginDp = 36;

        int changeFlashModeImageButtonLayoutMarginPx = (int) Utils.convertDpToPixel(context, changeFlashModeImageButtonLayoutMarginDp);

        params.setMargins(changeFlashModeImageButtonLayoutMarginPx, changeFlashModeImageButtonLayoutMarginPx, 0, 0);

        changeFlashModeImageButton.setLayoutParams(params);
    }

    /**
     * Decrease cameraModeImageButton.
     */
    private void decreaseCameraModeImageButton() {

        int changeCameraModeImageButtonLayoutWidthDp = 40;

        int changeCameraModeImageButtonLayoutWidthPx = (int) Utils.convertDpToPixel(context, changeCameraModeImageButtonLayoutWidthDp);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(changeCameraModeImageButtonLayoutWidthPx, changeCameraModeImageButtonLayoutWidthPx);

        params.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
        params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);

        int changeCameraModeImageButtonLayoutMarginDp = 36;

        int changeCameraModeImageButtonLayoutMarginPx = (int) Utils.convertDpToPixel(context, changeCameraModeImageButtonLayoutMarginDp);

        params.setMargins(0, changeCameraModeImageButtonLayoutMarginPx, changeCameraModeImageButtonLayoutMarginPx, 0);

        changeCameraModeImageButton.setLayoutParams(params);
    }

    /**
     * Decrease takePictureImageButton.
     */
    private void decreaseTakePictureImageButton() {

        int takePictureImageButtonLayoutWidthDp = 84;

        int takePictureImageButtonLayoutWidthPx = (int) Utils.convertDpToPixel(context, takePictureImageButtonLayoutWidthDp);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(takePictureImageButtonLayoutWidthPx, takePictureImageButtonLayoutWidthPx);

        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
        params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);

        int takePictureImageButtonLayoutMarginDp = 48;

        int takePictureImageButtonLayoutMarginPx = (int) Utils.convertDpToPixel(context, takePictureImageButtonLayoutMarginDp);

        params.setMargins(0, 0, 0, takePictureImageButtonLayoutMarginPx);

        takePictureImageButton.setLayoutParams(params);
    }

    /**
     * Reboot CooffeeCamera.
     */
    private void rebootCooffeeCamera() {
        Intent cooffeeCamera = new Intent(context, CooffeeCamera.class);
        startActivity(cooffeeCamera);
        finish();
    }

    /**
     * Check if the device has a camera.
     *
     * @return Boolean value.
     */
    private boolean checkCameraHardware() {

        PackageManager packageManager = getPackageManager();

        return packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA) || packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT) || (Camera.getNumberOfCameras() > 0);
    }

    /**
     * On touch camera view.
     */
    public void onTouch() {

        if (decorView != null) {

            if (isHideSystemUI) {
                Utils.showSystemUI(decorView);
                isHideSystemUI = false;
            } else {
                Utils.hideSystemUI(decorView);
                isHideSystemUI = true;
            }

        }

    }

    /**
     * Camera screen touch.
     */
    public void cameraScreenTouch() {

        if (isFlashMode) {
            changeFlashModeImageButton.setVisibility(View.VISIBLE);
        }

        if (isFrontCamera) {
            changeCameraModeImageButton.setVisibility(View.VISIBLE);
        }

    }

    /**
     * Double tap on the screen to change the camera.
     */
    public void doubleTap() {

        // If the device has a front camera.
        if (isFrontCamera) {

            int width = changeCameraModeImageButton.getWidth();
            int height = changeCameraModeImageButton.getHeight();

            width += 12;
            height += 12;

            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(width, height);

            params.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
            params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);

            int changeCameraModeLayoutMarginDp = 36;

            int changeCameraModeLayoutMarginPx = (int) Utils.convertDpToPixel(context, changeCameraModeLayoutMarginDp);

            params.setMargins(0, changeCameraModeLayoutMarginPx - 6, changeCameraModeLayoutMarginPx - 6, 0);

            changeCameraModeImageButton.setLayoutParams(params);

            if (camera != null) {
                camera.stopPreview();
                camera.release();
                camera = null;
                isPreview = false;
            }

            OpenCooffeeCameraApplication.setIsCameraFrontCamera(!isCameraFrontCamera);

            rebootCooffeeCamera();
        }

    }

    private PictureCallback myPictureCallback_JPEG = new PictureCallback(){

        @Override
        public void onPictureTaken(byte[] byteArray, Camera camera) {

            AppCompatActivity appCompatActivity = CooffeeCamera.this;

            ProcessFile processFile = new ProcessFile(appCompatActivity, byteArray);

            processFile.execute();
        }

    };

    /**
     * Get the output file type and create File to save it in File Cache.
     *
     * @param type
     *
     * @return file
     */
    private File getOutputMediaFile(int type) {

        File file;

        try {

            if (type == MEDIA_TYPE_IMAGE) {

                // It is a File type image.

                FileCache fileCache = new FileCache(context);

                String fileName = "temporal_file_" + Utils.generateCompressUUIDString() + ".jpg";

                file = fileCache.getFile(fileName);

                return file;
            }

        } catch (NullPointerException e) {
            // If the name is null.
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {

        try {
            if (camera != null) {
                camera.setPreviewDisplay(surfaceHolder);
            }
        } catch (IOException e) {
            // If the method fails (for example, if the surface is not available or is not suitable).
            Log.e(LOG_TAG, "Error configuring preview", e);
            camera.release();
        }

    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {

        if (isPreview) {
            camera.stopPreview();
            isPreview = false;
        }

        if (camera != null) {

            try {

                setCameraDisplayOrientation();

                Parameters parameters = camera.getParameters();

                // Set the type of image format as JPEG.
                parameters.setPictureFormat(ImageFormat.JPEG);

                // Sets Jpeg quality of captured picture.
                // quality: the JPEG quality of captured picture.
                // The range is 1 to 100, with 100 being the best.
                parameters.setJpegQuality(JPEG_QUALITY);

                // The width and the height that the device gives is around in portrait mode.
                Camera.Size previewSize = getBestSupportedSize(parameters.getSupportedPreviewSizes(), height, width);

                Camera.Size pictureSize = getBestSupportedSize(parameters.getSupportedPictureSizes(), height, width);

				/*
				for (Camera.Size s11 : parameters.getSupportedPreviewSizes()) {
					Log.d("Width, height: ",Integer.toString(s11.width) + " " + Integer.toString(s11.height));
				}
				*/

				/*
				for (Camera.Size s22 : parameters.getSupportedPictureSizes()) {
					Log.d("Width, height: ",Integer.toString(s22.width) + " " + Integer.toString(s22.height));
				}
				*/

                parameters.setPreviewSize(previewSize.width, previewSize.height);

                parameters.setPictureSize(pictureSize.width, pictureSize.height);

                // Get all focus modes.
                List<String> focusModesList = parameters.getSupportedFocusModes();

                if (focusModesList != null && !focusModesList.isEmpty()) {

                    if (focusModesList.contains(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                        // Set FOCUS_MODE_CONTINUOUS_PICTURE.
                        parameters.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                    } else if (focusModesList.contains(Parameters.FOCUS_MODE_AUTO)) {
                        // API level 5.
                        // Set FOCUS_MODE_AUTO.
                        parameters.setFocusMode(Parameters.FOCUS_MODE_AUTO);
                    }

                }

                // Check if the device has flash.
                // Most of the time, the getSupportedFlashModes function will return null.
                // To have the minimum number of errors if it does not return null to check if that option exists in the device.
                if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {

                    // Get all flash modes.
                    List<String> flashModesList = parameters.getSupportedFlashModes();

				    /*
				    if (flashModesList != null) {
				    	Iterator<String> fmi = flashModesList.iterator();
					    while (fmi.hasNext()) {
					        String currentFlashModes = fmi.next();
					        Log.v("Flash modes", "Checking " + currentFlashModes);
					    }
				    }
				    */

                    if (flashModesList != null && !flashModesList.isEmpty()) {

                        isFlashMode = true;

                        boolean storedIsCameraFlashMode = OpenCooffeeCameraApplication.getIsCameraFlashMode();

                        if (storedIsCameraFlashMode) {
                            // Flash mode ons.
                            changeFlashModeImageButton.setImageResource(R.drawable.ic_flash_on);
                            changeFlashModeImageButton.setVisibility(View.VISIBLE);
                            isCameraFlashMode = true;
                        } else {
                            // Flash mode off.
                            changeFlashModeImageButton.setImageResource(R.drawable.ic_flash_off);
                            changeFlashModeImageButton.setVisibility(View.VISIBLE);
                            isCameraFlashMode = false;
                        }

                        // Show changeFlashModeImageButton ImageButton.
                        changeFlashModeImageButton.setVisibility(View.VISIBLE);

                    } else {
                        isFlashMode = false;
                        // Hide changeFlashModeImageButton ImageButton.
                        changeFlashModeImageButton.setVisibility(View.INVISIBLE);
                    }

                    // Enable flash property if the device has it.
                    if (isCameraFlashMode) {

                        if (flashModesList == null) {
                            // Set flash mode to on.
                            parameters.setFlashMode(Parameters.FLASH_MODE_ON);
                        } else if (flashModesList.contains(Parameters.FLASH_MODE_ON)) {
                            // Set flash mode to on.
                            parameters.setFlashMode(Parameters.FLASH_MODE_ON);
                        }

                    } else {

                        if (flashModesList == null) {
                            // Set flash mode to off.
                            parameters.setFlashMode(Parameters.FLASH_MODE_OFF);
                        } else if (flashModesList.contains(Parameters.FLASH_MODE_OFF)) {
                            // Set flash mode to off.
                            parameters.setFlashMode(Parameters.FLASH_MODE_OFF);
                        }

                    }

                } else {
                    // Hide changeFlashModeImageButton ImageButton.
                    changeFlashModeImageButton.setVisibility(View.GONE);
                }

                // Get all scene modes.
                List<String> sceneModesList = parameters.getSupportedSceneModes();

                // Set the scene mode parameter to AUTO.
                if (sceneModesList.contains(Parameters.SCENE_MODE_AUTO)) {
                    parameters.setSceneMode(Parameters.SCENE_MODE_AUTO);
                }

			    /*
			    Iterator<String> smi = sceneModesList.iterator();
			    while (smi.hasNext()) {
			        String currentSceneModes = smi.next();
			        Log.v("Scene modes", "Checking " + currentSceneModes);
			    }
			    */

                // Get all antibandings.
                List<String> antibandingList = parameters.getSupportedAntibanding();

                // Set the antibanding parameter.
                if (antibandingList.contains(Parameters.ANTIBANDING_60HZ)) {
                    parameters.setAntibanding(Parameters.ANTIBANDING_60HZ);
                } else if (antibandingList.contains(Parameters.ANTIBANDING_50HZ)) {
                    parameters.setAntibanding(Parameters.ANTIBANDING_50HZ);
                } else if (antibandingList.contains(Parameters.ANTIBANDING_AUTO)) {
                    parameters.setAntibanding(Parameters.ANTIBANDING_AUTO);
                }

			    /*
			    Iterator<String> abi = antibandingList.iterator();
			    while (abi.hasNext()) {
			        String currentAntibanding = abi.next();
			        Log.v("Antibandings", "Checking " + currentAntibanding);
			    }
			    */

                // Color effects List.
                List<String> colorEffectsList = parameters.getSupportedColorEffects();

                // Set the color effect parameter.
                // Do not establish any effect.
                if (colorEffectsList.contains(Parameters.EFFECT_NONE)) {
                    parameters.setColorEffect(Parameters.EFFECT_NONE);
                }

			    /*
			    Iterator<String> cei = colorEffectsList.iterator();
			    while (cei.hasNext()) {
			        String currentColorEffect = cei.next();
			        Log.v("Color effects", "Checking " + currentColorEffect);
			    }
			    */

                // Supported white balance List.
                List<String> whiteBalancesList = parameters.getSupportedWhiteBalance();

                // Set the white balance parameter.
                // Set to WHITE_BALANCE_AUTO.
                if (whiteBalancesList.contains(Parameters.WHITE_BALANCE_AUTO)) {
                    parameters.setWhiteBalance(Parameters.WHITE_BALANCE_AUTO);
                }

			    /*
			    Iterator<String> wb = whiteBalancesList.iterator();
			    while (wb.hasNext()) {
			        String currentWhiteBalance = wb.next();
			        Log.v("White balances", "Checking " + currentWhiteBalance);
			    }
			    */

                camera.setParameters(parameters);

                isPreview = true;

                camera.setPreviewDisplay(surfaceHolder);

                camera.startPreview();

            } catch (RuntimeException e1) {
                // If any parameter is invalid or not supported.
                e1.printStackTrace();
            } catch (IOException e2) {
                // If the method fails (for example, if the surface is not available or is not suitable).
                Log.e(LOG_TAG, "Could not start preview", e2);
                camera.release();
                camera = null;
                isPreview = false;
            }

        }

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

        if (camera != null) {
            camera.stopPreview();
            camera.release();
            camera = null;
            isPreview = false;
        }

    }

    /**
     * Set the orientation of the camera.
     */
    public void setCameraDisplayOrientation() {

        CameraInfo cameraInfo = new CameraInfo();

        if (isCameraFrontCamera) {
            Camera.getCameraInfo(1, cameraInfo);
        } else {
            Camera.getCameraInfo(0, cameraInfo);
        }

        int rotation = getWindowManager().getDefaultDisplay().getRotation();

        int degrees = 0;

        switch (rotation) {

            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;

        }

        int desiredRotation = (cameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT) ? (360 - cameraInfo.orientation) : cameraInfo.orientation;
        int result = (desiredRotation - degrees + 360) % 360;

        camera.setDisplayOrientation(result);
    }

    /**
     * Find first front camera.
     *
     * @return int foundId
     */
    private int findFirstFrontCamera() {

        int numCams = Camera.getNumberOfCameras();

        for (int camId = 0; camId < numCams; camId++) {

            CameraInfo info = new CameraInfo();
            Camera.getCameraInfo(camId, info);

            if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
                foundId = camId;
                break;
            }

        }

        return foundId;
    }

    /**
     * Open the front camera.
     * First attempt to find and open the front camera.
     * If this attempt fails, open the available camera.
     *
     * @return Camera camera
     */
    private Camera openFrontCamera() {

        // Search front camera, using the Gingerbread API.
        // Java Reflection API is used for backward compatibility with the APIs prior to Gingerbread.
        try {

            Class<?> cameraClass = Class.forName("android.hardware.Camera");
            Object cameraInfo = null;
            Field field = null;

            int cameraCount = 0;
            Method getNumberOfCamerasMethod = cameraClass.getMethod("getNumberOfCameras");

            if (getNumberOfCamerasMethod != null) {
                cameraCount = (Integer) getNumberOfCamerasMethod.invoke(null, (Object[]) null);
            }

            Class<?> cameraInfoClass = Class.forName("android.hardware.Camera$CameraInfo");

            if (cameraInfoClass != null) {
                cameraInfo = cameraInfoClass.newInstance();
            }

            if (cameraInfo != null) {
                field = cameraInfo.getClass().getField("facing");
            }

            Method getCameraInfoMethod = cameraClass.getMethod("getCameraInfo", Integer.TYPE, cameraInfoClass);

            if (getCameraInfoMethod != null && cameraInfoClass != null && field != null) {

                for (int camIdx = 0; camIdx < cameraCount; camIdx++) {

                    getCameraInfoMethod.invoke(null, camIdx, cameraInfo);

                    int facing = field.getInt(cameraInfo);

                    // Camera.CameraInfo.CAMERA_FACING_FRONT
                    if (facing == 1) {

                        try {

                            Method cameraOpenMethod = cameraClass.getMethod("open", Integer.TYPE);

                            if (cameraOpenMethod != null) {
                                camera = (Camera) cameraOpenMethod.invoke(null, camIdx);
                            }

                        } catch (RuntimeException e1) {
                            Log.e(LOG_TAG, "Camera failed to open: " + e1.getLocalizedMessage());
                        }

                    }

                }

            }

        }

        // Ignoring the battery of exceptions thrown by Java Reflection API.
        catch (NullPointerException e2          ) {Log.e(LOG_TAG, "NullPointerException" + e2.getLocalizedMessage());}
        catch (ClassNotFoundException e3        ) {Log.e(LOG_TAG, "ClassNotFoundException" + e3.getLocalizedMessage());}
        catch (NoSuchMethodException e4         ) {Log.e(LOG_TAG, "NoSuchMethodException" + e4.getLocalizedMessage());}
        catch (NoSuchFieldException e5          ) {Log.e(LOG_TAG, "NoSuchFieldException" + e5.getLocalizedMessage());}
        catch (IllegalAccessException e6        ) {Log.e(LOG_TAG, "IllegalAccessException" + e6.getLocalizedMessage());}
        catch (InvocationTargetException e7     ) {Log.e(LOG_TAG, "InvocationTargetException" + e7.getLocalizedMessage());}
        catch (InstantiationException e8        ) {Log.e(LOG_TAG, "InstantiationException" + e8.getLocalizedMessage());}
        catch (SecurityException e9             ) {Log.e(LOG_TAG, "SecurityException" + e9.getLocalizedMessage());}

        if (camera == null) {

            // Try to initialize the camera using the pre-Gingerbread API.
            try {
                camera = Camera.open();
            } catch (RuntimeException e10) {
                Log.e(LOG_TAG, "Camera failed to open: " + e10.getLocalizedMessage());
            }

        }

        return camera;
    }

    /**
     * Get the best measure supported by the camera.
     *
     * @param sizesList
     * List of available resolutions.
     *
     * @param width
     * Device current width.
     *
     * @param height
     * Device current height.
     *
     * @return Camera.Size
     */
    private Camera.Size getBestSupportedSize(List<Camera.Size> sizesList, int width, int height) {

        double targetRatio = (double) width / height;

        if (sizesList == null) {
            return null;
        }

        Camera.Size bestSize = null;
        double minDiff = Double.MAX_VALUE;

        // Try to find an aspect ratio and related size.
        for (Camera.Size size : sizesList) {

            double ratio = (double) size.width / size.height;

            if (Math.abs(ratio - targetRatio) < minDiff) {
                bestSize = size;
                minDiff = Math.abs(ratio - targetRatio);
            }

        }

        return bestSize;
    }

    /**
     * Set the screen brightness.
     * brightnessValue (0F to 1F, being 0F 0% brightness and 1F 100%).
     *
     * @param brightness
     * Brightness level in percent.
     */
    public void setBrightness(float brightness) {

        try {

            int brightnessMode = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE);

            if (brightnessMode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
                Settings.System.putInt(
                        getContentResolver(),
                        Settings.System.SCREEN_BRIGHTNESS_MODE,
                        Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
                );
            }

            WindowManager.LayoutParams layoutParams = getWindow().getAttributes();

            layoutParams.screenBrightness = brightness;

            getWindow().setAttributes(layoutParams);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private class ProcessFile extends AsyncTask<Void, Void, Integer> {

        private AppCompatActivity appCompatActivity;
        private byte[] byteArray;

        // Constructor.
        ProcessFile(AppCompatActivity appCompatActivity, byte[] byteArray) {
            this.appCompatActivity = appCompatActivity;
            this.byteArray = byteArray;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            progressDialog = new ProgressDialog(appCompatActivity, R.style.OpenCooffeeCameraProgressDialogStyle);

            progressDialog.setMessage(getResources().getString(R.string.processing_image));
            progressDialog.setIndeterminate(false);
            progressDialog.setCancelable(false);
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.show();
        }

        @Override
        protected Integer doInBackground(Void... params) {

            while (!isCancelled()) {

                FileOutputStream fos = null;
                InputStream is = null;

                int requiredWidth = OpenCooffeeCameraApplication.getDeviceScreenWidth();
                int requiredHeight = OpenCooffeeCameraApplication.getDeviceScreenHeight();

                try {

                    mediaFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);

                    if (mediaFile == null) {
                        return 0;
                    }

                    fos = new FileOutputStream(mediaFile);

                    is = new ByteArrayInputStream(byteArray);

                    BitmapFactory.Options options = new BitmapFactory.Options();

                    // If set to true, the decoder will return null (no bitmap), but the out... fields will still be set, allowing the caller to query the bitmap without having to allocate the memory for its pixels.
                    options.inJustDecodeBounds = true;

                    // When this flag is set, if inDensity and inTargetDensity are not 0, the bitmap will be scaled to match inTargetDensity when loaded, rather than relying on the graphics system scaling it each time it is drawn to a Canvas.
                    // BitmapRegionDecoder ignores this flag, and will not scale output based on density. (though inSampleSize is supported)
                    // This flag is turned on by default and should be turned off if you need a non-scaled version of the bitmap.
                    // Nine-patch bitmaps ignore this flag and are always scaled.
                    // If inPremultiplied is set to false, and the image has alpha, setting this flag to true may result in incorrect colors.
                    options.inScaled = false;

                    // This field was deprecated in API level 24.
                    // As of N, this is ignored. In M and below, if dither is true, the decoder will attempt to dither the decoded image.
                    options.inDither = false;

                    // If this is non-null, the decoder will try to decode into this internal configuration.
                    // If it is null, or the request cannot be met, the decoder will try to pick the best matching config based on the system's screen depth,
                    // and characteristics of the original image such as if it has per-pixel alpha (requiring a config that also does).
                    // Image are loaded with the ARGB_8888 config by default.
                    options.inPreferredConfig = Common.BITMAP_CONFIG;

                    // This field was deprecated in API level 24.
                    // As of N, this is ignored. The output will always be high quality.
                    // In M and below, if inPreferQualityOverSpeed is set to true, the decoder will try to decode the reconstructed image to a higher quality even at the expense of the decoding speed.
                    // Currently the field only affects JPEG decode, in the case of which a more accurate, but slightly slower, IDCT method will be used instead.
                    options.inPreferQualityOverSpeed = true;

                    // If set, decode methods will always return a mutable Bitmap instead of an immutable one.
                    // This can be used for instance to programmatically apply effects to a Bitmap loaded through BitmapFactory.
                    // Can not be set simultaneously with inPreferredConfig = HARDWARE, because hardware bitmaps are always immutable.
                    options.inMutable = false;

                    int widthTemporal = options.outWidth;
                    int heightTemporal = options.outHeight;

                    int scale = 1;

                    while (true) {

                        if (widthTemporal <= requiredWidth || heightTemporal <= requiredHeight) {
                            break;
                        }

                        widthTemporal /= 2;
                        heightTemporal /= 2;
                        scale *= 2;
                    }

                    options.inSampleSize = scale;

                    options.inJustDecodeBounds = false;

                    Bitmap bitmap = BitmapFactory.decodeStream(is, null, options);

                    if (bitmap != null) {

                        Matrix matrix = new Matrix();

                        float degrees = 90;

                        if (isCameraFrontCamera) {
                            matrix.preScale(-1, 1); // Apply the "mirror effect".
                        } else if (OpenCooffeeCameraApplication.getDeviceModel().equals(Common.NEXUS_5X)) {
                            degrees = 270;
                        }

                        matrix.postRotate(degrees);

                        Bitmap rotatedAdjustBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

                        rotatedAdjustBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, fos);

                        fos.flush();

                        Utils.setMetadataToMediaFile(mediaFile);

                        return 1;
                    }

                } catch (FileNotFoundException e1) {
                    // If the file can not be opened or written.
                    Log.d(LOG_TAG, "File not found: " + e1.getMessage());
                } catch (NullPointerException e2) {
                    // If the file is null.
                    Log.d(LOG_TAG, "Null pointer: " + e2.getMessage());
                } catch (IOException e3) {
                    e3.printStackTrace();
                } finally {

                    if (fos != null) {
                        try {
                            fos.close();
                        } catch (IOException e4) {
                            e4.printStackTrace();
                        }
                    }

                    if (is != null) {
                        try {
                            is.close();
                        } catch (IOException e5) {
                            e5.printStackTrace();
                        }
                    }

                }

            }

            return 0;
        }

        @Override
        protected void onPostExecute(Integer i) {

            if (i == 1) {

                Intent cooffeeCameraResult = new Intent(context, CooffeeCameraResult.class);

                Bundle extras = new Bundle();
                extras.clear();

                extras.putString("type", "image");
                extras.putString("path", mediaFile.getPath());

                cooffeeCameraResult.putExtras(extras);

                startActivityForResult(cooffeeCameraResult, CAMERA_RESULT_REQUEST_CODE);

            } else {

                toastTextView.setText(getResources().getString(R.string.an_error_has_ocurred));

                messageToast.show();
            }

            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
                progressDialog = null;
            }

        }


    }

}