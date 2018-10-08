package com.example.alvin.camerasource;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacv.AndroidFrameConverter;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;


/**
 * Created by Alvin on 2016-05-20.
 */
public class MyCameraView  extends SurfaceView implements SurfaceHolder.Callback,Camera.PreviewCallback{
    private final String LOG_TAG = "MyCameraView";


    private SurfaceHolder mHolder;
    private Camera mCamera;
    private int width;
    private int height;
    public ByteArrayOutputStream mFrameBuffer;
    private Context con;
    int cont=0;
    /**
     * For OpenCV
     */
    private byte[] videoSource;
    private Bitmap imageA;
    final boolean LOG_FRAME_RATE = true;
    private boolean bProcessing = false;
    private Handler mHandler=new Handler(Looper.getMainLooper());

    int i = 0;
    long now, oldnow, count = 0;
    static {
        System.loadLibrary("native-lib");
    }
    public static String path;
    private native void computerVision(Bitmap pTarget, byte[] pSource);
    /**
     * Constructor of the MyCameraView
     * @param context
     * @param camera
     */
    public MyCameraView(Context context,Camera camera){
        super(context);
        con=context;
        computerVision(null, null);
        mCamera=camera;
        mHolder=getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }
    /**
     * set preview to the camera
     * @param holder
     */
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            mCamera.setPreviewDisplay(holder);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * surface destroyed function
     * @param holder
     */
    public void surfaceDestroyed(SurfaceHolder holder) {
        mCamera.setPreviewCallback(null);
        mCamera.release();
        mCamera=null;
    }


    /**
     * surface changed function
     * @param holder
     * @param format
     * @param w
     * @param h
     */
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        try{
            mCamera.stopPreview();
        } catch (Exception e){
            e.printStackTrace();
        }
        try{
            //Configration Camera Parameter(full-size)
            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setPreviewSize(320,240);
            this.width=parameters.getPreviewSize().width;
            this.height=parameters.getPreviewSize().height;
            parameters.setPreviewFormat(ImageFormat.NV21);
            mCamera.setParameters(parameters);
           // mCamera.setDisplayOrientation(90);
            mCamera.setPreviewCallback(this);
            mCamera.startPreview();
        }catch(Exception e){
            e.printStackTrace();
        }
    }


    /**
     * frame call back function
     * @param data
     * @param cam
     */
    public void onPreviewFrame(byte[] data,Camera cam){
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
                mHandler.post(DoImageProcessing);
            }
        }
       /* try{
            //convert YuvImage(NV21) to JPEG Image data
            YuvImage yuvimage=new YuvImage(data,ImageFormat.NV21,this.width,this.height,null);
            System.out.println("WidthandHeight"+yuvimage.getHeight()+"::"+yuvimage.getWidth());
            ByteArrayOutputStream baos=new ByteArrayOutputStream();
            yuvimage.compressToJpeg(new Rect(0,0,this.width,this.height),100,baos);

            if(cont%7==0) {
                mFrameBuffer = baos;
            }
            cont++;
        }catch(Exception e){
            Log.d("parse","errpr");
        }*/
    }
    private Runnable DoImageProcessing = new Runnable() {
        public void run() {
            bProcessing = true;
            computerVision(imageA, videoSource);
            mCamera.addCallbackBuffer(videoSource);
            bProcessing = false;
        }
    };


}

