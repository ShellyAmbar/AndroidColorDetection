package com.example.livecolordetection;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Camera;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.support.v4.view.GravityCompat;
import android.support.v7.app.ActionBarDrawerToggle;
import android.view.MenuItem;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;

import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.zip.ZipInputStream;

public class MainActivity extends AppCompatActivity

        implements NavigationView.OnNavigationItemSelectedListener, View.OnClickListener {
    static final int REQUEST_VIDEO_CAPTURE = 1;
    private FloatingActionButton capturePictureBtn;
    private TextureView textureView;
    private TextView textViewOne,textViewTwo,textViewThree,textViewFour,textViewFive;

    private boolean isDeviceHasCamera = false;
    private Integer facingCamera, backCamera;
    private CameraManager cameraManager;
    private int cameraFacing;
    private Size previewSize;
    private String cameraId ;
    private static final int CAMERA_REQUEST_CODE = 2;
    private static final int STORAGE_REQUEST_CODE = 3;
    private HandlerThread backgroundThread;
    private Handler backgroundHandler;
    private CameraDevice.StateCallback stateCallback;
    private CameraDevice cameraDevice;
    private TextureView.SurfaceTextureListener surfaceTextureListener;
    private CaptureRequest.Builder captureRequestBuilder;
    private CaptureRequest captureRequest;
    private CameraCaptureSession cameraCaptureSession;
    private int[] mRgbBuffer;
    private boolean isAppHasCameraPermissions;
    private ImageReader mImageReader;
    private ImageReader.OnImageAvailableListener mImageAvailable;
    private File galleryFolder;
    private List<Integer> topFiveAppearedRGBValues;

    @TargetApi(Build.VERSION_CODES.M)
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);
        capturePictureBtn = findViewById(R.id.capturePictureBtn);
        capturePictureBtn.setOnClickListener(this);
        textureView = findViewById(R.id.video_view);
        textViewOne = findViewById(R.id.textViewOne);
        textViewTwo = findViewById(R.id.textViewTwo);
        textViewThree = findViewById(R.id.textViewThree);
        textViewFour = findViewById(R.id.textViewFour);
        textViewFive = findViewById(R.id.textViewFive);
        // initialization of fields
        cameraCaptureSession = null;
        captureRequest=null;
        captureRequestBuilder=null;
        surfaceTextureListener=null;
        cameraDevice=null;
        stateCallback=null;
        backgroundHandler=null;
        previewSize=null;
        cameraId="";
        cameraFacing=0;
        cameraManager =null;
        mImageReader=null;
        mImageAvailable=null;
        mRgbBuffer=null;
        isAppHasCameraPermissions = false;
        galleryFolder = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        }
        //set which camera will be open
        cameraFacing = CameraCharacteristics.LENS_FACING_BACK;

        isDeviceHasCamera =checkCameraHardware();
        if(isDeviceHasCamera) {
         checkAndGiveCameraPermissions();
         if (textureView != null ) {

                // set CameraDevice callback
                stateCallback = new CameraDevice.StateCallback() {
                    @Override
                    public void onOpened(CameraDevice camera) {
                        cameraDevice = camera;
                        createPreviewSession();
                    }

                    @Override
                    public void onDisconnected(CameraDevice camera) {
                        cameraDevice.close();
                        cameraDevice = null;
                    }

                    @Override
                    public void onError(CameraDevice camera, int error) {
                        cameraDevice.close();
                        cameraDevice = null;
                    }
                };

                // declare SurfaceTexture Listener
                surfaceTextureListener = new TextureView.SurfaceTextureListener() {
                    @Override
                    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
                        setUpCamera();
                        openCamera();
                    }

                    @Override
                    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {

                    }

                    @Override
                    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
                        return false;
                    }

                    @Override
                    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

                    }
                };

                mImageAvailable = new ImageReader.OnImageAvailableListener() {
                    @Override
                    public void onImageAvailable(ImageReader reader) {
                        Image image = reader.acquireLatestImage();
                        if (image == null)
                            return;

                        // RowStride of planes may differ from width set to image reader
                        final Image.Plane[] planes = image.getPlanes();
                        final int total = planes[0].getRowStride() * previewSize.getHeight();
                        if (mRgbBuffer == null || mRgbBuffer.length < total)
                            mRgbBuffer = new int[total];
                        // get rgb from every pixel of the current frame and calculate top 5 max values with algorithms
                        getRGBIntFromPlanes(planes);
                        image.close();
                    }
                };
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onResume() {
        super.onResume();
        // set SurfaceTexture Listener
        openBackgroundThread();
        if (textureView.isAvailable()) {
            setUpCamera();
            openCamera();
        } else {
            textureView.setSurfaceTextureListener(surfaceTextureListener);
        }
    }

    /** Check if this device has a camera */
    private boolean checkCameraHardware() {
        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }

    private void checkAndGiveCameraPermissions() {
        if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.M
                &&
                (checkSelfPermission(Manifest.permission.CAMERA)!= PackageManager.PERMISSION_GRANTED || checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED ))
        { // request permission if it doesn't granted
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, CAMERA_REQUEST_CODE);
        }else{
            isAppHasCameraPermissions = true;
        }
    }

    /** A safe way to get an instance of the Camera object. */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private   void openCamera(){
        try {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
                cameraManager.openCamera(cameraId, stateCallback, backgroundHandler);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    private void openBackgroundThread() {
        backgroundThread = new HandlerThread("camera_background_thread");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void setUpCamera() {
        try {
            for (String cameraId : cameraManager.getCameraIdList()) {
                CameraCharacteristics cameraCharacteristics =
                        cameraManager.getCameraCharacteristics(cameraId);
                if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) ==
                        cameraFacing) {
                    StreamConfigurationMap streamConfigurationMap = cameraCharacteristics.get(
                            CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                    previewSize = streamConfigurationMap.getOutputSizes(SurfaceTexture.class)[0];
                    mImageReader = ImageReader.newInstance(previewSize.getWidth(),
                            previewSize.getHeight(),
                            ImageFormat.YUV_420_888, 3);
                    mImageReader.setOnImageAvailableListener(mImageAvailable,backgroundHandler);


                    this.cameraId = cameraId;
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void createPreviewSession() {
        try {
            List surfaces = new ArrayList<>();
            SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
            surfaceTexture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
            Surface previewSurface = new Surface(surfaceTexture);
            surfaces.add(previewSurface);

            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(previewSurface);

            Surface readerSurface = mImageReader.getSurface();
            surfaces.add(readerSurface);
            captureRequestBuilder.addTarget(readerSurface);

            cameraDevice.createCaptureSession(surfaces,
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                            if (cameraDevice == null) {
                                return;
                            }

                            try {
                                captureRequest = captureRequestBuilder.build();
                                MainActivity.this.cameraCaptureSession = cameraCaptureSession;
                                MainActivity.this.cameraCaptureSession.setRepeatingRequest(captureRequest,
                                        null, backgroundHandler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {

                        }
                    }, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onStop() {
        super.onStop();
        Log.d("cycle","stop");
        closeCamera();
        closeBackgroundThread();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void closeCamera() {
        if (cameraCaptureSession != null) {
            cameraCaptureSession.close();
            cameraCaptureSession = null;
        }

        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void closeBackgroundThread() {
        if (backgroundHandler != null) {
            backgroundThread.quitSafely();
            backgroundThread = null;
            backgroundHandler = null;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode){

            case CAMERA_REQUEST_CODE:
                if(grantResults.length> 0 && grantResults[0]==PackageManager.PERMISSION_GRANTED ){
                    isAppHasCameraPermissions=true;
                    Toast.makeText(this, "Camera permitted.", Toast.LENGTH_SHORT).show();

                }else{
                    isAppHasCameraPermissions=false;
                    Toast.makeText(this, "Camera fetchers did not permitted.", Toast.LENGTH_SHORT).show();
                }

                if(grantResults.length> 1 && grantResults[1]==PackageManager.PERMISSION_GRANTED){
                    Log.d("storage","Storage permitted" );
                    //create a storage file for this app
                    Toast.makeText(this, "Storage permitted.", Toast.LENGTH_SHORT).show();
                    createImageGallery();

                }else{

                    Toast.makeText(this, "Storage fetchers did not permitted.", Toast.LENGTH_SHORT).show();
                }
               break;
        }

    }

    private void createImageGallery() {
        File storageDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        galleryFolder = new File(storageDirectory, getResources().getString(R.string.app_name));
        if (!galleryFolder.exists()) {
            boolean wasCreated = galleryFolder.mkdirs();
            if (!wasCreated) {
                Log.e("CapturedImages", "Failed to create directory");
            }else{
                Toast.makeText(this, "Succeeded to create directory path  " , Toast.LENGTH_SHORT).show();
                Log.d("path" , galleryFolder.getPath().toString());
            }
        }else{
            Log.d("path" , galleryFolder.getPath().toString());
        }
    }

    private File createImageFile(File galleryFolder) throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "image_" + timeStamp + "_";
        return File.createTempFile(imageFileName, ".jpg", galleryFolder);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void getRGBIntFromPlanes(Image.Plane[] planes) {
        ByteBuffer yPlane = planes[0].getBuffer();
        ByteBuffer uPlane = planes[1].getBuffer();
        ByteBuffer vPlane = planes[2].getBuffer();
        HashMap<Integer, List<Integer>> RGBHashmap = new HashMap<>();
        List<Integer> RGBList = new ArrayList<>();

        int bufferIndex = 0;
        final int total = yPlane.capacity();
        final int uvCapacity = uPlane.capacity();
        final int width = planes[0].getRowStride();

        int yPos = 0;
        for (int i = 0; i < previewSize.getHeight(); i++) {
            int uvPos = (i >> 1) * width;

            for (int j = 0; j < width; j++) {
                if (uvPos >= uvCapacity-1)
                    break;
                if (yPos >= total)
                    break;

                final int y1 = (yPlane.get(yPos++) & 0xff)-16;

            /*
              The ordering of the u (Cb) and v (Cr) bytes inside the planes is a
              bit strange. The _first_ byte of the u-plane and the _second_ byte
              of the v-plane build the u/v pair and belong to the first two pixels
              (y-bytes), thus usual YUV 420 behavior. What the Android devs did
              here (IMHO): just copy the interleaved NV21 U/V data to two planes
              but keep the offset of the interleaving.
             */
                final int u = (uPlane.get(uvPos) & 0xff) - 128;
                final int v = (vPlane.get(uvPos+1) & 0xff) - 128;
                if ((j & 1) == 1) {
                    uvPos += 2;
                }
                RGBList = YUVtoRGB(y1,u,v);

                // This is the integer variant to convert YCbCr to RGB, NTSC values.
                // formulae found at
                // https://software.intel.com/en-us/android/articles/trusted-tools-in-the-new-android-world-optimization-techniques-from-intel-sse-intrinsics-to
                // and on StackOverflow etc.
                final int y1192 = 1192 * y1;
                int r = (y1192 + 1634 * v);
                int g = (y1192 - 833 * v - 400 * u);
                int b = (y1192 + 2066 * u);

                r = (r < 0) ? 0 : ((r > 262143) ? 262143 : r);
                g = (g < 0) ? 0 : ((g > 262143) ? 262143 : g);
                b = (b < 0) ? 0 : ((b > 262143) ? 262143 : b);

                //  combine RGB int value
                mRgbBuffer[bufferIndex++] = ((r << 6) & 0xff0000) | ((g >> 2) & 0xff00)
                        | ((b >> 10) | 0xff);

                RGBHashmap.put(mRgbBuffer[bufferIndex-1],RGBList);
            }
        }
        //find and set top 5 rgb colors from buffer into text views
        getAndSetTopFiveRGB(RGBHashmap);
    }

    public static List<Integer> YUVtoRGB(float y, float u, float v){
        List<Integer> rgb = new ArrayList<Integer>(3);
        float r,g,b;

        r = (float)((y + 0.000 * u + 1.140 * v) );
        g = (float)((y - 0.395 * u - 0.581 * v) );
        b = (float)((y + 2.032 * u + 0.000 * v) );

    // set a block of range to RGB values between (0-255).
        r = Math.max(0,Math.min(255,r));
        g = Math.max(0,Math.min(255,g));
        b = Math.max(0,Math.min(255,b));

        rgb.add((int)r);
        rgb.add((int)g);
        rgb.add ((int)b);

        return rgb;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void getAndSetTopFiveRGB(final HashMap<Integer, List<Integer>> RGBHashmap){
        List<Integer> topFiveRGB = new ArrayList<Integer>();
              topFiveRGB  = findTopFiveRgbValues();
        Handler handler = new Handler(Looper.getMainLooper());
        final List<Integer> finalTopFiveRGB = topFiveRGB;
        handler.post(new Runnable() {

            @SuppressLint("SetTextI18n")
            @Override
            public void run() {
                // set in all 5 text view the RGB values in Int, Hexa-Decimal and r,g,b (0-255).
                textViewOne.setText("First RGB:        "+  createStringFormatForTextviewByIndex(0,RGBHashmap,finalTopFiveRGB));
                textViewTwo.setText("Second RGB:        "+ createStringFormatForTextviewByIndex(1,RGBHashmap,finalTopFiveRGB));
                textViewThree.setText("Third RGB:        "+ createStringFormatForTextviewByIndex(2,RGBHashmap,finalTopFiveRGB));
                textViewFour.setText("Fourth RGB:        "+ createStringFormatForTextviewByIndex(3,RGBHashmap,finalTopFiveRGB));
                textViewFive.setText("Fifth RGB:        "+ createStringFormatForTextviewByIndex(4,RGBHashmap,finalTopFiveRGB));
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private String createStringFormatForTextviewByIndex(int indexOfTextView, HashMap<Integer, List<Integer>> RGBHashmap, final List<Integer> finalTopFiveRGB){

      StringBuilder  output = new StringBuilder();
        output.append(String.format("#%06X", (0xFFFFFF & finalTopFiveRGB.get(indexOfTextView)))) ;
        output.append("               R:"+ Objects.requireNonNull(RGBHashmap.get(finalTopFiveRGB.get(indexOfTextView))).get(0));
        output.append(" G:"+ Objects.requireNonNull(RGBHashmap.get(finalTopFiveRGB.get(indexOfTextView))).get(1));
        output.append(" B:"+ Objects.requireNonNull(RGBHashmap.get(finalTopFiveRGB.get(indexOfTextView))).get(2));
        return output.toString();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private List<Integer> findTopFiveRgbValues() {
        // create Monim-arr to count all RGB appearance
        List<Integer> topFiveRGB = new ArrayList<Integer>();
        //set the list's size to the max value of RGB values
        int nunOfExistenceRGB = 16777216;
        int[] countAppearanceArr = new int[nunOfExistenceRGB];
        for(int rgb : mRgbBuffer){
            countAppearanceArr[rgb]++;
        }
        // find the current max valu and then add it to the forbidden numbers
        topFiveRGB = findCurrentIndexOfMaxValue(countAppearanceArr);
        return topFiveRGB;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private List<Integer> findCurrentIndexOfMaxValue(int[] countAppearanceArr) {
        topFiveAppearedRGBValues = new ArrayList<>();
        List<Integer> topFiveRGB = new ArrayList<Integer>();
        int total_pixels = previewSize.getHeight()*previewSize.getWidth();
        for(int j=0;j<5;j++) {
            int maxValue = 0;
            int rgbColorValue=0;
            for (int i = 0; i < countAppearanceArr.length; i++) {
                if (countAppearanceArr[i] > maxValue && !topFiveAppearedRGBValues.contains(i)) {
                    maxValue = countAppearanceArr[i];
                    rgbColorValue = i;
                }
            }
        // add rgb value to top 5 rgb values so that they will be forbidden in the next search
            topFiveAppearedRGBValues.add(rgbColorValue);
            topFiveRGB.add(rgbColorValue);
        }

        return topFiveRGB;
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_share) {
            actionSendAppDetails();
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void actionSendAppDetails() {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT,
                "Hey check out my app ColorCame");
        sendIntent.setType("text/plain");
        startActivity(sendIntent);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onClick(View v) {
        switch (v.getId()){

            case R.id.capturePictureBtn:
                    capturePictureAndSaveToStorage();
                break;
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void capturePictureAndSaveToStorage(){
       // lock();
        FileOutputStream outputPhoto = null;
        try {
            outputPhoto = new FileOutputStream(createImageFile(galleryFolder));
            textureView.getBitmap()
                    .compress(Bitmap.CompressFormat.PNG, 100, outputPhoto);
            Toast.makeText(this, "The photo has been saved successfully." , Toast.LENGTH_SHORT).show();
            Log.d("path",  galleryFolder.getPath().toString());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
           // unlock();
            try {
                if (outputPhoto != null) {
                    outputPhoto.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void lock() {
        try {
            cameraCaptureSession.capture(captureRequestBuilder.build(),
                    null, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void unlock() {
        try {
            cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(),
                    null, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

}
