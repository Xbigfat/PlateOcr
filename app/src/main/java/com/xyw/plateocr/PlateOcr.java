package com.xyw.plateocr;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
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
    //-------------------------------------------------------------------

    //蒙版层的矩形区域的宽度和长度
    private final int rectangleWidth = 300;
    private final int rectangleHeight = 100;


    //-------------------------------------------------------------------
    //预览view
    private SurfaceView surfaceView;
    //快门按钮
    private ImageButton shutterBtn;
    //闪光灯按钮
    private ImageButton flash;

    //相机质量参数
    private List<Camera.Size> pictureSizes, previewSizes;
    //相机 预览宽度，高度，拍照宽度，高度
    private int preW, preH, picW, picH;
    //屏幕尺寸
    private int screenPxWidth, screenPxHeight;
    //屏幕像素密度
    private float density;
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
        setMaskRect();
    }

    private void setMaskRect() {
        //根据比例将 dp 转换成 px 设置一个矩形区域，添加到蒙版上
        //解释一下：屏幕高度的一半，减去矩形宽度的一半
        int x1 = screenPxWidth / 2 - dip2px(rectangleWidth) / 2;
        int y1 = screenPxHeight / 3 - dip2px(rectangleHeight) / 2;
        int x2 = x1 + dip2px(rectangleWidth);
        int y2 = y1 + dip2px(rectangleHeight);
        Rect maskRect = new Rect(x1, y1, x2, y2);
        DrawImageView div = findViewById(R.id.drawIV);
        div.setRect(maskRect);
    }

    /**
     * 将 dp 转换成 px
     *
     * @param dipValue 需要转换的dp
     * @return px
     */
    private int dip2px(float dipValue) {
        return (int) (dipValue * density + 0.5f);
    }

    private void init() {
        //设定context
        mThis = PlateOcr.this;
        //初始化相机对象
        mCamera = getCameraInstance();
        getScreenSize();
    }

    /**
     * 获取屏幕尺寸保存到成员变量中
     * 宽、高、像素密度
     * 单位为 像素
     */
    private void getScreenSize() {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        screenPxWidth = dm.widthPixels;
        screenPxHeight = dm.heightPixels;
        density = dm.density;
        Log.i(tag, "Screen---Width =" + screenPxWidth + "Height =" + screenPxHeight + "densityDpi =" + dm.densityDpi);
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
        picH = pictureSizes.get(n).height;
        picW = pictureSizes.get(n).width;
        parameters.setPictureSize(picW, picH);
        Log.i(tag, "拍摄分辨为： w : " + picW + "h : " + picH);
        //--------------------------------------设置预览分辨率，动态接近屏幕长宽比---------------------------------------------
        int[] a = new int[previewSizes.size()];
        int[] b = new int[previewSizes.size()];
        for (int i = 0; i < previewSizes.size(); i++) {
            int supportH = previewSizes.get(i).height;
            int supportW = previewSizes.get(i).width;
            a[i] = Math.abs(supportW - screenPxHeight);
            b[i] = Math.abs(supportH - screenPxWidth);
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
        parameters.setPreviewSize(preW, preH);
        //设置最大刷新率
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
            Point rectPictureSize = createCenterPictureRect(dip2px(rectangleWidth), dip2px(rectangleHeight));
            int DST_RECT_WIDTH = rectPictureSize.x;
            int DST_RECT_HEIGHT = rectPictureSize.y;
            //压缩到图片
            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            Matrix matrix = new Matrix();
            matrix.postRotate(90);
            //旋转图片
            Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            //压缩到预览分辨率
            int x = rotated.getWidth()/2 - DST_RECT_WIDTH/2;
            int y = rotated.getHeight()/3 - DST_RECT_HEIGHT/2;
            //重新截取图片
            bitmap = Bitmap.createBitmap(rotated, x, y, DST_RECT_WIDTH, DST_RECT_HEIGHT);
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

    private Point createCenterPictureRect(int w, int h) {

        float wRate = (float) (picH) / (float) (screenPxWidth);
        float hRate = (float) (picW) / (float) (screenPxHeight);
        float rate = (wRate <= hRate) ? wRate : hRate;
        int wRectPicture = (int) (w * wRate);
        int hRectPicture = (int) (h * hRate);
        return new Point(wRectPicture, hRectPicture);
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
