package com.example.myapplication;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnTouchListener;
import android.view.SurfaceView;
import android.widget.ImageView;
import android.widget.StackView;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends Activity implements OnTouchListener, CvCameraViewListener2 {
    private static final String TAG = "MainActivity";

    private boolean mIsColorSelected = false;
    private Mat mRgba;
    private Mat mGrey;
    private Scalar mBlobColorRgba;
    private Scalar mBlobColorHsv;
    private ColorBlobDetector mDetector;
    private Mat mSpectrum;
    private Size SPECTRUM_SIZE;
    private Scalar CONTOUR_COLOR;
    private ImageView imageView;
    private TextView textView;
    File cascFile;
    CascadeClassifier faceDetector;
    CascadeClassifier eyeDetector;
    private CameraBridgeViewBase mOpenCvCameraView;
    long timeStamp = 0;
    long now = 1;
    int detected = 0;
    boolean eyeDetected = false;
    boolean faceDetected=false;
    StackView stackView;
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                    mOpenCvCameraView.setCameraIndex(1);
//                    mOpenCvCameraView.setMinimumHeight(mOpenCvCameraView.getHeight());
//                    mOpenCvCameraView.setMinimumWidth(mOpenCvCameraView.getWidth());
                    mOpenCvCameraView.setMaxFrameSize(600, 800);
//                    mOpenCvCameraView.setMaxFrameSize(320, 240);
                    mOpenCvCameraView.setOnTouchListener(MainActivity.this);


                    initializeFaceDetector();
                    initializeEyeDetector();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    private void initializeEyeDetector() {
        InputStream in = getResources().openRawResource(R.raw.haarcascade_eye_tree_eyeglasses);
        File cascadeDir = getDir("cascadeEye", Context.MODE_PRIVATE);
        cascFile = new File(cascadeDir, "haarcascade_eye_tree_eyeglasses.xml");

        try {
            FileOutputStream fos = new FileOutputStream(cascFile);
            byte[] buffer = new byte[4096];
            int bytesRead = -1;
            while ((bytesRead = in.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
            in.close();
            fos.close();
            eyeDetector = new CascadeClassifier(cascFile.getAbsolutePath());

            if (eyeDetector.empty()) {
                faceDetector = null;
            } else {
                cascadeDir.delete();
            }


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initializeFaceDetector() {
        InputStream in = getResources().openRawResource(R.raw.lbpcascade_frontalcatface);
        File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
        cascFile = new File(cascadeDir, "lbpcascade_frontalcatface.xml");

        try {
            FileOutputStream fos = new FileOutputStream(cascFile);
            byte[] buffer = new byte[4096];
            int bytesRead = -1;
            while ((bytesRead = in.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
            in.close();
            fos.close();
            faceDetector = new CascadeClassifier(cascFile.getAbsolutePath());
            if (faceDetector.empty()) {
                faceDetector = null;
            } else {
                cascadeDir.delete();
            }


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public MainActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    private void getPermission() {
        if (Build.VERSION.SDK_INT > 22) {
            if (ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            } else {
                Log.i("MainActivity", "get permission");
            }
        } else {
            Log.i("MainActivity", "no need");
        }
    }

    /**
     * Called when the activity is first created.
     */
    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);
        imageView=findViewById(R.id.imageView8);
        textView=findViewById(R.id.textView);

        getPermission();

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.myCameraView);
        mOpenCvCameraView.setCameraPermissionGranted();
        mOpenCvCameraView.setCvCameraViewListener(this);
        if (!OpenCVLoader.initDebug()) {
            OpenCVLoader.initDebug();
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);

        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

  private void  changeLogo(){
        textView.setText("DETECTED "+detected);
//      imageView.setForeground(getResources().getDrawable(R.drawable.camera_frame_active));
    }
    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mDetector = new ColorBlobDetector();
        mSpectrum = new Mat();
        mBlobColorRgba = new Scalar(255);
        mBlobColorHsv = new Scalar(255);
        SPECTRUM_SIZE = new Size(200, 64);
        CONTOUR_COLOR = new Scalar(255, 0, 0, 255);
    }

    public void onCameraViewStopped() {
        mRgba.release();
    }

    public boolean onTouch(View v, MotionEvent event) {

        int cols = mRgba.cols();
        int rows = mRgba.rows();

        int xOffset = (mOpenCvCameraView.getWidth() - cols) / 2;
        int yOffset = (mOpenCvCameraView.getHeight() - rows) / 2;

        int x = (int) event.getX() - xOffset;
        int y = (int) event.getY() - yOffset;

        Log.i(TAG, "Touch image coordinates: (" + x + ", " + y + ")");

        if ((x < 0) || (y < 0) || (x > cols) || (y > rows)) return false;

        Rect touchedRect = new Rect();

        touchedRect.x = (x > 4) ? x - 4 : 0;
        touchedRect.y = (y > 4) ? y - 4 : 0;

        touchedRect.width = (x + 4 < cols) ? x + 4 - touchedRect.x : cols - touchedRect.x;
        touchedRect.height = (y + 4 < rows) ? y + 4 - touchedRect.y : rows - touchedRect.y;

        Mat touchedRegionRgba = mRgba.submat(touchedRect);

        Mat touchedRegionHsv = new Mat();
        Imgproc.cvtColor(touchedRegionRgba, touchedRegionHsv, Imgproc.COLOR_RGB2HSV_FULL);

        // Calculate average color of touched region
        mBlobColorHsv = Core.sumElems(touchedRegionHsv);
        int pointCount = touchedRect.width * touchedRect.height;
        for (int i = 0; i < mBlobColorHsv.val.length; i++)
            mBlobColorHsv.val[i] /= pointCount;

        mBlobColorRgba = converScalarHsv2Rgba(mBlobColorHsv);

        Log.i(TAG, "Touched rgba color: (" + mBlobColorRgba.val[0] + ", " + mBlobColorRgba.val[1] +
                ", " + mBlobColorRgba.val[2] + ", " + mBlobColorRgba.val[3] + ")");

        mDetector.setHsvColor(mBlobColorHsv);

        Imgproc.resize(mDetector.getSpectrum(), mSpectrum, SPECTRUM_SIZE, 0, 0, Imgproc.INTER_LINEAR_EXACT);

        mIsColorSelected = true;

        touchedRegionRgba.release();
        touchedRegionHsv.release();

        return false; // don't need subsequent touch events
    }
    public static int getScreenWidth() {
        return Resources.getSystem().getDisplayMetrics().widthPixels;
    }

    public static int getScreenHeight() {
        return Resources.getSystem().getDisplayMetrics().heightPixels;
    }
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        getWindowManager().getDefaultDisplay();
        mRgba = inputFrame.rgba();
        Mat matRgbaFlip = new Mat();
timeStamp=System.currentTimeMillis();
        Mat mRgbaT = mRgba.t();
        Core.flip(mRgbaT, matRgbaFlip, -1);
        Imgproc.resize(matRgbaFlip, matRgbaFlip, mRgba.size());
        mRgbaT.release();

        mGrey = inputFrame.gray();


        /// my code
        // detect face
        MatOfRect faceDetection = new MatOfRect();
        MatOfRect eyeDetections = new MatOfRect();
        faceDetector.detectMultiScale(matRgbaFlip, faceDetection);
        eyeDetector.detectMultiScale(matRgbaFlip, eyeDetections);
//        if(eyeDetected)
        if(faceDetection.toArray().length>0) {
            for (Rect rect : faceDetection.toArray()) {
                faceDetected = true;
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        imageView.setForeground(getResources().getDrawable(R.drawable.camera_frame_active));

                        // Stuff that updates the UI

                    }
                });
                if (eyeDetected) {

                    if ((now - timeStamp) < 1000 && (now - timeStamp) > 80) {

                        detected = detected + 1;
                        changeLogo();
                    }
                }
//        Log.d(TAG, "onCameraFrame: DETECTED NOW" + ( now  ));

                Imgproc.rectangle(matRgbaFlip, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(255, 0, 0));
//            imageView.setForeground(getResources().getDrawable(R.drawable.camera_frame_active));
                double area = rect.width * rect.height;

            }
        }
        else{
            faceDetected=false;
//            detected=0;
        }
        int i = 0;

        if (System.currentTimeMillis() - timeStamp < 1500)
            if ((eyeDetections.toArray().length) == 0) {
                Log.d(TAG, "onCameraFrame:  DETECTED EYE" + "FALSE");
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        changeLogo();
                        imageView.setForeground(getResources().getDrawable(R.drawable.camera_frame_inactive));
                        // Stuff that updates the UI

                    }
                });
                if (eyeDetected) {
                    Log.d(TAG, "onCameraFrame: DETECTED BLINK 2" + (now - timeStamp));
                    now = System.currentTimeMillis();
                    if ((now - timeStamp) < 1000 && (now-timeStamp)>50) {
                        if(faceDetected)
                        detected = detected + 1;

                        Log.d(TAG, "onCameraFrame: DETECTED BLINK" + detected);
                    }
                eyeDetected=false;
                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            changeLogo();

                            // Stuff that updates the UI

                        }
                    });

//                    detected=0;
                }
            } else {
                Log.d(TAG, "onCameraFrame:  DETECTED EYE" + "TRUE");
                eyeDetected = true;
                timeStamp = System.currentTimeMillis();
                for (Rect rect : eyeDetections.toArray()) {
                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            changeLogo();

                            // Stuff that updates the UI
                        }
                    });
//                Log.d(TAG, "onCameraFrame:  DETECTED EYE"+eyeDetected);
                    Log.d(TAG, "onCameraFrame: DETECTED BLINK" + detected);
                    eyeDetected = true;
                    if ((now - timeStamp) < 300) {
                        Imgproc.rectangle(matRgbaFlip, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(255, 0, 0));
//
//                    Log.d(TAG, "onCameraFrame: DETECTED" + detected);
                    } else {
                        detected = 0;
                    }
                }
//                Imgproc.rectangle(matRgbaFlip, new Point(100, 100), new Point( 300, 300));
//                textView.setText("DETECTED");
//      double area=    rect.width*rect.height;

//            if(rect.x>(mRgba.width()/2) && rect.y <(mRgba.height()/2))
//            {
//                Log.d(TAG, "AREA: "+area);
//                Log.d(TAG, "onCameraFrame: RECT x"+rect.x);
//                Log.d(TAG, "onCameraFrame: RECT width"+rect.width);
//                Log.d(TAG, "onCameraFrame: RECT y"+rect.y);
//                Log.d(TAG, "onCameraFrame: RECT height"+rect.height);
////                if(area>25)
//                Imgproc.rectangle(matRgbaFlip, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(255, 0, 0));
//
//            }
            }

        return matRgbaFlip;
//        if (mIsColorSelected) {
//            mDetector.process(mRgba);
//            List<MatOfPoint> contours = mDetector.getContours();
//            Log.e(TAG, "Contours count: " + contours.size());
//            Imgproc.drawContours(mRgba, contours, -1, CONTOUR_COLOR);
//
//            Mat colorLabel = mRgba.submat(4, 68, 4, 68);
//            colorLabel.setTo(mBlobColorRgba);
//
//            Mat spectrumLabel = mRgba.submat(4, 4 + mSpectrum.rows(), 70, 70 + mSpectrum.cols());
//            mSpectrum.copyTo(spectrumLabel);
//        }

        /// previous code
//        return mRgba;
    }


    private Scalar converScalarHsv2Rgba(Scalar hsvColor) {
        Mat pointMatRgba = new Mat();
        Mat pointMatHsv = new Mat(1, 1, CvType.CV_8UC3, hsvColor);
        Imgproc.cvtColor(pointMatHsv, pointMatRgba, Imgproc.COLOR_HSV2RGB_FULL, 4);

        return new Scalar(pointMatRgba.get(0, 0));
    }
}