package com.xyw.plateocr;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.Toast;

import com.kernal.plateid.Devcode;
import com.kernal.plateid.PlateCfgParameter;
import com.kernal.plateid.PlateRecognitionParameter;
import com.kernal.plateid.RecogService;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

/**
 * Created by 31429 on 2017/11/9.
 */

@SuppressWarnings("ALL")
public class PlateOcr extends AppCompatActivity {
    //预览view
    private SurfaceView surfaceView;
    //快门按钮
    private ImageButton shutterBtn;
    //闪光灯按钮
    private ImageButton flash;

    //相机质量参数
    private List<Camera.Size> pictureSizes, previewSizes;
    //相机 预览宽度，高度，拍照宽度，高度
    private int preW, preH, shutterW, shutterH;
    //屏幕尺寸
    private int screenWidth, screenHeight;
    //surfaceView 尺寸
    //待识别图片长度数据
    private int height, width;
    //相机对象
    private android.hardware.Camera mCamera;
    //识别执行对象
    private RecogService.MyBinder recogBinder;
    //TAG
    private static final String tag = "xyw";
    //context 对象
    private PlateOcr mThis;
    //识别文件路径
    private String recogFilePath;
    //service 绑定对象
    private ServiceConnection recogConn = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            //连接结束时置空
            recogBinder = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            recogBinder = (RecogService.MyBinder) service;
            //设置识别参数
            PlateCfgParameter cfgparameter = new PlateCfgParameter();
            cfgparameter.armpolice = 4;
            cfgparameter.armpolice2 = 16;
            cfgparameter.embassy = 12;
            cfgparameter.individual = 0;
            cfgparameter.nOCR_Th = 0;
            cfgparameter.nPlateLocate_Th = 5;
            cfgparameter.onlylocation = 15;
            cfgparameter.tworowyellow = 2;
            cfgparameter.tworowarmy = 6;
            cfgparameter.szProvince = "";
            cfgparameter.onlytworowyellow = 11;
            cfgparameter.tractor = 8;
            cfgparameter.bIsNight = 1;
            cfgparameter.newEnergy = 24;
            cfgparameter.consulate = 22;
            //装载识别参数
            recogBinder.setRecogArgu(cfgparameter, 0, 0, 0);
            Log.i(tag, "set the recogBinder Arguement!");
            //设置识别对象
            PlateRecognitionParameter prp = new PlateRecognitionParameter();
            prp.width = width; //set bitmap width here;
            prp.height = height; //set bitmap height here;
            prp.pic = recogFilePath; //set bitmap file path here;
            prp.devCode = Devcode.DEVCODE;
            //调用识别
            String[] resultValue = recogBinder.doRecogDetail(prp);
            Log.i(tag, "the status code is : " + String.valueOf(recogBinder.getnRet()));
            try {
                for (String s : resultValue) {
                    if (s != null) {
                        Log.i(tag, s);
                    }
                }
                //返回识别结果
                Intent intent = new Intent();
                intent.putExtra("state", recogBinder.getnRet());
                intent.putExtra("plate", resultValue);
                intent.putExtra("path", recogFilePath);
                mThis.setResult(RESULT_OK, intent);
                //结束当前
                if (recogBinder != null) {
                    mCamera.release();
                    unbindService(recogConn);
                    finish();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.avtivity_plate_ocr);
        findViewAndSetCallback();
        init();
        logCameraInfo();
        setCameraParameters();
    }


    private void init() {
        //设定context
        mThis = PlateOcr.this;
        //初始化相机对象
        mCamera = getCameraInstance();
        getScreenSize();
    }

    private void getScreenSize() {
        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        screenWidth = display.getWidth();
        screenHeight = display.getHeight();
    }

    private void logCameraInfo() {
        //获取相机预览像素，拍摄像素
        pictureSizes = mCamera.getParameters().getSupportedPictureSizes();
        previewSizes = mCamera.getParameters().getSupportedPreviewSizes();
        for (int i = 0; i < pictureSizes.size(); i++) {
            Camera.Size pSize = pictureSizes.get(i);
            Log.i("-------initCamera", "---------------------Picture width =" + pSize.width + "-----------------Picture height =" + pSize.height);
        }

        for (int i = 0; i < previewSizes.size(); i++) {
            Camera.Size pSize = previewSizes.get(i);
            Log.i("--------initCamera", "--------preview width =" + pSize.width + "--------preview height =" + pSize.height);
        }
    }

    private void setCameraParameters() {
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setPictureFormat(PixelFormat.JPEG);
        //--------------------------------------设置拍摄分辨率，小于1920*1080-------------------------------------------------
        int n = 0;
        for (int m = 0; m < pictureSizes.size(); m++) {
            int width = pictureSizes.get(m).width;
            if (width > 1920) continue;
            int height = pictureSizes.get(m).height;
            if (height > 1080) continue;
            n = m;
            break;
        }
        shutterH = pictureSizes.get(n).height;
        shutterW = pictureSizes.get(n).width;
        parameters.setPictureSize(shutterW, shutterH);
        Log.i(tag, "拍摄分辨为： w : " + shutterW + "h : " + shutterH);
        //--------------------------------------设置预览分辨率，动态接近屏幕长宽比---------------------------------------------
        int[] a = new int[previewSizes.size()];
        int[] b = new int[previewSizes.size()];
        for (int i = 0; i < previewSizes.size(); i++) {
            int supportH = previewSizes.get(i).height;
            int supportW = previewSizes.get(i).width;
            a[i] = Math.abs(supportW - screenHeight);
            b[i] = Math.abs(supportH - screenWidth);
            Log.d(tag, "supportW:" + supportW + "supportH:" + supportH);
        }
        int minW = 0, minA = a[0];
        for (int i = 0; i < a.length; i++) {
            if (a[i] <= minA) {
                minW = i;
                minA = a[i];
            }
        }
        int minH = 0, minB = b[0];
        for (int i = 0; i < b.length; i++) {
            if (b[i] < minB) {
                minH = i;
                minB = b[i];
            }
        }
        preH = previewSizes.get(minH).height;
        preW = previewSizes.get(minW).width;
        Log.d(tag, "预览分辨率 y：" + preW + "x" + preH);
        List<Integer> list = parameters.getSupportedPreviewFrameRates();
        parameters.setPreviewSize(previewSizes.get(minW).width, previewSizes.get(minH).height); // 设置预览图像大小
        parameters.setPreviewFrameRate(list.get(list.size() - 1));
        setCameraDisplayOrientation();
        mCamera.setParameters(parameters);
    }

    private void findViewAndSetCallback() {
        shutterBtn = findViewById(R.id.shutter_btn);
        shutterBtn.setOnClickListener(new Shutter());
        flash = findViewById(R.id.flash_btn);
        //flash.setOnClickListener();
        surfaceView = findViewById(R.id.camera_surface);
        surfaceView.setOnClickListener(new Focus());
        surfaceView.getHolder().addCallback(new Preview());
    }

    private void setCameraDisplayOrientation() {
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(mCamera.getNumberOfCameras() - 1, info);
        int rotation = getWindowManager().getDefaultDisplay()
                .getRotation();
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
        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        mCamera.setDisplayOrientation(result);
    }

    private Camera getCameraInstance() {
        Camera c = null;
        try {
            c = Camera.open();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return c;
    }

    private class Shutter implements View.OnClickListener {
        //请在这里做自动对焦，拍摄照片并且保存到文件
        //保存后调用开启线程调用识别服务
        @Override
        public void onClick(View v) {
            mCamera.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean success, Camera camera) {
                    if (success) {
                        mCamera.takePicture(null, null, new SaveJpeg());
                    }
                }
            });
        }

    }

    private class SaveJpeg implements Camera.PictureCallback {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            //获取到屏幕长度
            int x = screenWidth;
            int y = screenHeight;
            int top = (int) (x / 10 * 4);
            int bottom = (int) (x / 10 * 8);
            //压缩到图片
            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            Matrix matrix = new Matrix();
            matrix.postRotate(90);
            //旋转图片
            Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            //压缩到预览分辨率
            bitmap = Bitmap.createScaledBitmap(rotated, preW, preH, true);
            //重新截取图片
            bitmap = Bitmap.createBitmap(bitmap, 0, top, x, bottom - top);
            //保存文件
            makeFile(bitmap);
            height = bitmap.getHeight();
            width = bitmap.getWidth();
            new Thread() {
                @Override
                public void run() {
                    Intent intent = new Intent(mThis, RecogService.class);
                    bindService(intent, recogConn, Service.BIND_AUTO_CREATE);
                }
            }.run();
        }

        private void makeFile(Bitmap bitmap) {
            if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                Toast.makeText(mThis, "未装载SD卡！", Toast.LENGTH_SHORT).show();
                return;
            }
            String SDCARD_ROOT_PATH = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
            String SAVE_PATH_IN_SDCARD = "/DataCollectSysCache/";
            String TEMP = "temp/";
            String fileName = "recog_plate.jpg";
            String path = SDCARD_ROOT_PATH + SAVE_PATH_IN_SDCARD + TEMP;
            File dir = new File(path);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            recogFilePath = path + "//" + fileName;
            File file = new File(recogFilePath);
            if (file.exists()) {
                file.delete();
            }
            try {
                file.createNewFile();
                BufferedOutputStream bos = new BufferedOutputStream(
                        new FileOutputStream(file));
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
                bos.flush();
                bos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    private class Preview implements SurfaceHolder.Callback {

        //在这里预览实时图像，设置自动对焦
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            try {
                mCamera.setPreviewDisplay(holder);
                mCamera.startPreview();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            //mCamera.stopPreview();
        }

    }

    private class Focus implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            mCamera.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean success, Camera camera) {

                }
            });
        }
    }
}
