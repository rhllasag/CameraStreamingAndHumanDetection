package com.example.alvin.camerasource;

import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.TextureView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;


import org.bytedeco.javacv.Frame;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.List;

public class MainActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener, Camera.PreviewCallback{
    private Camera mCamera;
    public TextView serverStatus;
    public static String SERVERIP = "localhost";
    public static final int SERVERPORT = 8080;
    private Handler handler = new Handler();
    private static final String TAG = "MainActivity";
    public ByteArrayOutputStream mFrames;
    static {
        if(OpenCVLoader.initDebug()){
            Log.d(TAG,"OpenCV ok");

        }
        else
            Log.d(TAG,"OpenCV No working");
    }
    private TextureView tv;
    private byte[] videoSource;
    private ImageView imViewA;
    private Bitmap imageA;
    final boolean LOG_FRAME_RATE = true;
    private boolean bProcessing = false;
    //private Handler mHandler=new Handler(Looper.getMainLooper());
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Assetbridge.unpack(this);
        setContentView(R.layout.activity_main);
        serverStatus = (TextView) findViewById(R.id.textView);
        SERVERIP = getLocalIpAddress();
        mCamera = getCameraInstance();
        tv = (TextureView) findViewById(R.id.preview);
        imViewA = (ImageView) findViewById(R.id.imageViewA);
        tv.setSurfaceTextureListener(this);
        serverStatus = (TextView) findViewById(R.id.textView);
        Thread cThread = new Thread(new MyServerThread(this, SERVERIP, SERVERPORT, handler));
        cThread.start();
    }
    static {
        System.loadLibrary("native-lib");
    }
    private native void computerVision(Bitmap pTarget, byte[] pSource);
    private native void opticalComputerVision(Bitmap pTarget, byte[] pSource);

    /**
     * Get local ip address of the phone
     *
     * @return ipAddress
     */
    private String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        return inetAddress.getHostAddress().toString();
                    }
                }
            }
        } catch (SocketException ex) {
            Log.e("ServerActivity", ex.toString());
        }
        return null;
    }

    /**
     * Get camera instance
     *
     * @return
     */
    public static Camera getCameraInstance() {
        Camera c = null;
        try {
            c = Camera.open();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return c;
    }

    int i = 0;
    long now, oldnow, count = 0;
    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if(LOG_FRAME_RATE) {
            /// Measure frame rate:
            i++;
            now = System.nanoTime() / 1000;
            if (i > 3) {
                Log.d("onPreviewFrame: ", "Measured: " + 1000000L / (now - oldnow) + " fps.");
                count++;
            }
            oldnow = now;
        }


        if (mCamera != null){
            if(!bProcessing) {
                videoSource = data;
                handler.post(DoImageProcessing);
            }
        }
    }


    private Runnable DoImageProcessing = new Runnable() {
        public void run() {
            bProcessing = true;
            computerVision(imageA, videoSource);
            imViewA.invalidate();
            mCamera.addCallbackBuffer(videoSource);
            ByteArrayOutputStream baos=new ByteArrayOutputStream();
            imageA.compress(Bitmap.CompressFormat.JPEG,100,baos);
            mFrames=baos;
            bProcessing = false;
        }
    };
    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();

        if (mCamera == null) { /// Xperia LT15i has no front-facing camera, defaults to back camera
            mCamera = Camera.open();
        }


        try{


            mCamera.setPreviewTexture(surface);
            mCamera.setPreviewCallbackWithBuffer(this);
            mCamera.setDisplayOrientation(0);

            Camera.Size size = findBestResolution(width,height);
            PixelFormat pixelFormat = new PixelFormat();
            PixelFormat.getPixelFormatInfo(mCamera.getParameters().getPreviewFormat(), pixelFormat);
            int sourceSize = size.width * size.height * pixelFormat.bitsPerPixel / 8;

            /// Camera size and video format
            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setPreviewSize(size.width, size.height);
            parameters.setPreviewFormat(PixelFormat.YCbCr_420_SP);
            mCamera.setParameters(parameters);

            /// Video buffer and bitmaps
            videoSource = new byte[sourceSize];
            imageA = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888);
            imViewA.setImageBitmap(imageA);

            /// Queue video frame buffer and start camera preview
            mCamera.addCallbackBuffer(videoSource);
            mCamera.startPreview();

        } catch (IOException e){
            mCamera.release();
            mCamera = null;
            throw new IllegalStateException();
        }
    }
    private Camera.Size findBestResolution(int pWidth, int pHeight){
        List<Camera.Size> sizes = mCamera.getParameters().getSupportedPreviewSizes();
        Camera.Size selectedSize = mCamera.new Size(0,0);

        for(Camera.Size size: sizes){
            if ((size.width <= pWidth) && (size.height <= pHeight) && (size.width >= selectedSize.width) && (size.height >= selectedSize.height )){
                selectedSize = size;
            }
        }

        if((selectedSize.width == 0) || (selectedSize.height == 0)){
            selectedSize = sizes.get(0);
        }

        return selectedSize;
    }
    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        // Release camera

        if (mCamera != null){
            mCamera.stopPreview();
            mCamera.release();

            mCamera = null;
            videoSource = null;

            imageA.recycle();; imageA = null;
        }
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }
}
