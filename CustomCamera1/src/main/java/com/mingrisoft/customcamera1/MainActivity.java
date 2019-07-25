package com.mingrisoft.customcamera1;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.ImageButton;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends MPermissionsActivity
        implements SurfaceHolder.Callback {
    private final String TAG = MainActivity.class.getSimpleName();//用于打印log日志
    private SurfaceView surface;//用于显示图像的控件
    private SurfaceHolder mHolder;//用来管理图像的绘制
    private ImageButton take_photo;//用于拍照的按钮
    private Camera mCamera;//摄像头对象
    private int degrees;//旋转角度
    private int cameraID = Camera.CameraInfo.CAMERA_FACING_BACK;//摄像ID代表前置摄像头或后置摄像头
    private ProgressDialog dialog;//进度提示窗
    private Handler handler = new Handler(msg -> {
        if (msg.what == 1) {
            mCamera.startPreview();
            dialog.dismiss();//取消进度条
            String imagePath = (String) msg.obj;//获取图片的保存路径
            Intent intent = new Intent(this, ShowPictureActivity.class);
            startActivity(intent.putExtra("image_path", imagePath));//跳转界面
        }
        return false;
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();//初始化控件
        take_photo.setOnClickListener(v -> {//点击事件
            if (mCamera != null) {
                dialog.show();//显示提示窗口
                mCamera.autoFocus(autoFocus);//自动对焦拍照
            }
        });//设置点击监听
    }

    /**
     * 权限获取成功
     */
    @Override
    public void permissionSuccess(int requestCode) {
        super.permissionSuccess(requestCode);

        if ((requestCode == 1000)) {
            try {
                mCamera = getCameraInstance();//获取摄像头对象
                //设置摄像头的预览角度
                setCameraDisplayOrientation(this, cameraID, mCamera);
                mCamera.setPreviewDisplay(mHolder);//使holder与预览相关联
                mCamera.startPreview();//开启预览
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    /**
     * 初始化控件
     */
    private void initView() {
        surface = (SurfaceView) findViewById(R.id.surface);
        take_photo = (ImageButton) findViewById(R.id.take_photo);
        dialog = new ProgressDialog(this);//创建加载等待弹窗
        dialog.setTitle("拍照中，请稍等");//设置弹窗标题
        dialog.setCancelable(false);//设置弹窗不可关闭
        dialog.setCanceledOnTouchOutside(false);//设置点击弹窗外不可关闭弹窗
    }

    /**
     * 检查设备的摄像头是否可用
     *
     * @return 返回true则摄像头可用
     */
    private boolean checkCameraHardware() {
        return this.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }

    /**
     * 获取摄像头对象
     *
     * @return
     */
    private Camera getCameraInstance() {
        Camera camera = null;
        try {
            camera = Camera.open(cameraID);//获取摄像头对象
        } catch (Exception e) {
        }
        return camera;
    }

    /**
     * 当创建完Surface后调用
     *
     * @param holder
     */
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.i(TAG, "surfaceCreated: ");
        try {
            if (mCamera == null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                    //进行权限检车
                    requestPermission(new String[]{Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE},1000);
                }else {
                    mCamera = getCameraInstance();//获取摄像头对象
                    setCameraDisplayOrientation(this, cameraID, mCamera);
                    mCamera.setPreviewDisplay(mHolder);//使holder与预览相关联
                    mCamera.startPreview();//开启预览
                }
            }
        } catch (IOException e) {
            mCamera.release();//释放资源
            Log.i(TAG, "开启预览发生错误" + e.getMessage());
        }
    }

    /**
     * 当绘制发生改变后调用
     *
     * @param holder
     * @param format
     * @param width
     * @param height
     */
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.i(TAG, "surfaceChanged: ");
        if (mHolder.getSurface() == null) {
            Log.i(TAG, "surfaceChanged:纹理未被创建");
            return;
        }
        if (mCamera == null){
            return;
        }
        try {
            mCamera.stopPreview();//先停止预览
            mCamera.setPreviewDisplay(mHolder);//使holder与预览相关联
            mCamera.startPreview();//开启预览
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 当销毁时调用
     *
     * @param holder
     */
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.i(TAG, "surfaceDestroyed: ");
        toReleaseCamera();//释放资源
    }

    /**
     * 释放摄像头的资源
     */
    private void toReleaseCamera() {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);//停止回调监听
            mCamera.stopPreview();//停止预览
            mCamera.release();//释放资源
            mCamera = null;//清空对象
            mHolder = null;//请空对象
        }
    }
    /**
     * 界面运行时调用
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (checkCameraHardware()) {
            mHolder = surface.getHolder();//获取控制绘制的对象
            mHolder.addCallback(this);//设置回调监听
        }
    }
    /**
     * 获取拍照
     */
    private Camera.PictureCallback mPicture = (data, camera) ->
            new Thread(() -> {
                File pictureFile = new File(Environment.getExternalStorageDirectory(),
                        System.currentTimeMillis() + "CS_image.jpg");
                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);//获取bitmap对象
                Matrix matrix = new Matrix();//获取矩阵对象用于图形变换
                matrix.setRotate(degrees);//设置旋转角度
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                BufferedOutputStream buff = null;//使用输出流，将文件写入SD上
                try {
                    buff = new BufferedOutputStream(new FileOutputStream(pictureFile));//获取缓存流
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, buff);//将文件保存到指定路径
                    Message message = Message.obtain();
                    message.what = 1;//设置标记值
                    message.obj = pictureFile.getPath();//获取图片路径
                    handler.sendMessage(message);//发送一条消息
                } catch (FileNotFoundException e) {
                    Log.d(TAG, "文件未被找到" + e.getMessage());
                } finally {
                    if (buff != null) {
                        try {
                            buff.close();//关闭流
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }).start();
    /**
     * 自动对焦
     */
    private Camera.AutoFocusCallback autoFocus = (success, camera) -> {
        if (success) mCamera.takePicture(null, null, mPicture);
    };

    /**
     * 设置图像的预览方向（官方推荐方法）
     *
     * @param activity
     * @param cameraId
     * @param camera
     */
    private void setCameraDisplayOrientation(
            Activity activity, int cameraId, android.hardware.Camera camera) {
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay()
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
            result = (360 - result) % 360;
        } else {
            result = (info.orientation - degrees + 360) % 360;
        }
        this.degrees = result;
        camera.setDisplayOrientation(result);
    }


    /******************************************************* 开启和关闭闪关灯的功能 ***************************************************/
    /**
     * 选择菜单
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu,menu);
        return true;
    }

    private boolean isLightOpen;//闪光灯是否开启
    /**
     * 选择菜单
     * @param item
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.light://用来开启闪光灯
                if (null != mCamera){
                    if (isLightOpen){//关闭闪光灯
                        closeLight(mCamera.getParameters());
                        isLightOpen = false;
                    }else {//开启闪光灯
                        openLight(mCamera.getParameters());
                        isLightOpen = true;
                    }
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * 开启闪光灯
     * @param parameters
     */
    private void openLight(Camera.Parameters parameters){
        parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
        mCamera.setParameters(parameters);
    }

    /**
     * 关闭闪光灯
     * @param parameters
     */
    private void closeLight(Camera.Parameters parameters){
        parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
        mCamera.setParameters(parameters);
    }
}
