package com.arcvideo.snapshot.floatingwindowdemo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cc.ibooker.zcountdownviewlib.SingleCountDownView;

public class CameraActivity extends MPermissionsActivity implements SurfaceHolder.Callback, Camera.PreviewCallback {

    private static final String TAG = CameraActivity.class.getSimpleName();
    @BindView(R.id.iv_back_btn)
    ImageView ivBackBtn;
    @BindView(R.id.iv_small_btn)
    ImageView ivSmallBtn;
    //    @BindView(R.id.surface)
//    SurfaceView surface;
//    @BindView(R.id.take_photo)
//    ImageButton takePhoto;
    @BindView(R.id.video_view)
    VideoView videoView;
    @BindView(R.id.singleCountDownView)
    SingleCountDownView singleCountDownView;
    @BindView(R.id.btn_jump_main)
    Button btnJumpMain;

    //布局参数.
    private static WindowManager.LayoutParams params;
    //实例化的WindowManager.
    private static WindowManager windowManager;
    private static int statusBarHeight = -1;
    private static FrameLayout toucherLayout;

    private static float start_X = 0;
    private static float start_Y = 0;

    // 记录上次移动的位置
    private static float lastX = 0;
    private static float lastY = 0;
    private static int offset;
    // 是否是移动事件
    private static boolean isMoved = false;
    /**
     * 两次点击时间间隔，单位毫秒
     */
    private static final int totalTime = 1000;

    private static boolean isInit = true;
    private static boolean isScale = true;
    private static boolean isShowScale = true;

    private MediaController controller;//控制器
    private MediaController controllerFloat;//控制器

    private SurfaceHolder mHolder;//用来管理图像的绘制
    private Camera mCamera;//摄像头对象
    private int degrees;//旋转角度
    private int cameraID = Camera.CameraInfo.CAMERA_FACING_BACK;//摄像ID代表前置摄像头或后置摄像头
    private ProgressDialog dialog;//进度提示窗

    private Handler handler = new Handler(msg -> {
        if (msg.what == 1) {
            mCamera.startPreview();
            dialog.dismiss();//取消进度条
            String imagePath = (String) msg.obj;//获取图片的保存路径
            Log.e(TAG, "路径: " + imagePath);
//            Intent intent = new Intent(this, ShowPictureActivity.class);
//            startActivity(intent.putExtra("image_path", imagePath));//跳转界面
        }
        return false;
    });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        ButterKnife.bind(this);
        //设置屏幕常亮
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        dialog = new ProgressDialog(this);//创建加载等待弹窗
        dialog.setTitle("拍照中，请稍等");//设置弹窗标题
        dialog.setCancelable(false);//设置弹窗不可关闭
        dialog.setCanceledOnTouchOutside(false);//设置点击弹窗外不可关闭弹窗

//        takePhoto.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (mCamera != null) {
//                    dialog.show();//显示提示窗口
//                    mCamera.autoFocus(autoFocus);//自动对焦拍照
//                }
//            }
//        });

        isShowScale = true;

        //实例化控制器
        controller = new MediaController(this);
        //设置播放文件路径
        String uri = "android.resource://" + getPackageName() + "/" + R.raw.test;
        //设置播放源
        videoView.setVideoURI(Uri.parse(uri));
        //设置控制器
        videoView.setMediaController(controller);
        //给控制器设置播放器控件
        controller.setMediaPlayer(videoView);
        videoView.requestFocus();//获取焦点
        //设置视频加载回调
        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                //播放视频
                videoView.start();
            }
        });

        // 单个倒计时点击事件监听
        singleCountDownView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                singleCountDownView.setText("开始倒计时");
                singleCountDownView.setTextColor(Color.parseColor("#FF7198"));
                // 开启倒计时
                singleCountDownView.setTime(1000)
                        .setTimeColorHex("#FF7198")
                        .setTimePrefixText("倒计时:  ")
                        .setTimeSuffixText("  s")
                        .startCountDown();

            }
        });

        singleCountDownView.setSingleCountDownEndListener(new SingleCountDownView.SingleCountDownEndListener() {
            @Override
            public void onSingleCountDownEnd() {
                Toast.makeText(CameraActivity.this, "倒计时结束", Toast.LENGTH_SHORT).show();
                singleCountDownView.setText("开始倒计时");
                singleCountDownView.setTextColor(Color.parseColor("#BBBBBB"));
            }
        });

        Log.e(TAG, "倒计时: " + singleCountDownView.getDrawingTime());
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.e(TAG, "执行onRestart()");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.e(TAG, "执行onStart()");
    }

    /**
     * 界面运行时调用
     */
    @Override
    protected void onResume() {
        super.onResume();
        Log.e(TAG, "执行onResume()");
//        boolean b = checkCameraHardware();
//        Log.e(TAG, " " + b);
//        if (checkCameraHardware()) {
//            mHolder = surface.getHolder();//获取控制绘制的对象
//            mHolder.addCallback(this);//设置回调监听
//        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.e(TAG, "执行onPause()");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.e(TAG, "执行onStop()");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "执行onDestroy()");
        Log.e(TAG, "isShowScale:  " + isShowScale);
        if (!isShowScale) {
            remove();
        }
    }

    @OnClick({R.id.iv_back_btn, R.id.iv_small_btn, R.id.btn_jump_main})
    public void onViewClicked(View view) {
        switch (view.getId()) {

            case R.id.iv_back_btn:
                finish();
                break;

            case R.id.iv_small_btn:
                Log.e(TAG, "isShowScale:  " + isShowScale);
                if (isShowScale) {
                    showFloatingWindow();
                } else {
                    Toast.makeText(this, "首先关闭悬浮窗", Toast.LENGTH_SHORT).show();
                }
//                finish();
                break;

            case R.id.btn_jump_main:
                Intent intent = new Intent(CameraActivity.this, MainActivity.class);
                startActivity(intent);
                break;

        }
    }


    private void showFloatingWindow() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (!Settings.canDrawOverlays(this)) {
                //没有悬浮窗权限,跳转申请
                Toast.makeText(getApplicationContext(), "请开启悬浮窗权限", Toast.LENGTH_LONG).show();
                //魅族不支持直接打开应用设置
                if (!MEIZU.isMeizuFlymeOS()) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                    startActivityForResult(intent, 0);
                } else {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                    startActivityForResult(intent, 0);
                }
            } else {
                showWindow();
//                finish();
            }
        } else {
            //6.0以下　只有MUI会修改权限
            if (MIUI.rom()) {
                if (PermissionUtils.hasPermission(this)) {
                    showWindow();
//                    finish();
                } else {
                    MIUI.req(this);
                }
            } else {
                showWindow();
//                finish();
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void showWindow() {

//        isShowScale = false;

        //            // 设置显示的模式
        //            mLayout.format = PixelFormat.RGBA_8888;
        //            // 设置对齐的方法
        //            mLayout.gravity = Gravity.TOP | Gravity.LEFT;
        //            // 设置窗体宽度和高度
        //            // 设置视频的播放窗口大小
        //            mLayout.width = 700;
        //            mLayout.height = 400;
        //            mLayout.x = 300;
        //            mLayout.y = 300;
        //            //将指定View解析后添加到窗口管理器里面
        //            mWindowsView = View.inflate(this, R.layout.layout_window, null);

        //取得系统窗体
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        //窗体的布局样式 赋值WindowManager&LayoutParam.
        params = new WindowManager.LayoutParams();
        //设置type.系统提示型窗口，一般都在应用程序窗口之上.
        if (Build.VERSION.SDK_INT >= 26) {//8.0新特性
            params.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            params.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        }
        //设置效果为背景透明.
        params.format = PixelFormat.RGBA_8888;
        // 设置窗体焦点及触摸：
        //设置flags.不可聚焦及不可使用按钮对悬浮窗进行操控(不能获得按键输入焦点).
        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        //设置窗口坐标参考系
        params.gravity = Gravity.LEFT | Gravity.TOP;
        //用于检测状态栏高度.
        int resourceId = getResources().getIdentifier("status_bar_height",
                "dimen", "android");
        if (resourceId > 0) {
            statusBarHeight = getResources().getDimensionPixelSize(resourceId);
        }
        offset = DensityUtil.dp2px(this, 2);//移动偏移量
        //设置原点
        params.x = getScreenWidth(this) - DensityUtil.dp2px(this, 170);
        params.y = getScreenHeight(this) - DensityUtil.dp2px(this, 100 + 72);
        //设置悬浮窗口长宽数据.
        ////            params.width = DensityUtil.dp2px(context, 180);
        ////            params.height = DensityUtil.dp2px(context, 100);
        params.width = DensityUtil.dp2px(this, 300);
        params.height = DensityUtil.dp2px(this, 600);
        //
        //获取浮动窗口视图所在布局.
        toucherLayout = new FrameLayout(this);
        View inflateView = LayoutInflater.from(this).inflate(R.layout.flating_window_view_item, null);
        ImageView imageViewClose = inflateView.findViewById(R.id.btn_small);
        SurfaceView flaotSurface = inflateView.findViewById(R.id.surface);
        ImageButton btnTakePhoto = inflateView.findViewById(R.id.btn_take_photo);
        VideoView videoViewFloat = inflateView.findViewById(R.id.video_view_float);
        SingleCountDownView singleCountDownViewFloat = inflateView.findViewById(R.id.singleCountDownView_Float);

        toucherLayout.addView(inflateView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        //添加toucherlayout
        if (isInit) {
            windowManager.addView(toucherLayout, params);
        } else {
            windowManager.updateViewLayout(toucherLayout, params);
            remove();
        }

        //实例化控制器
        controllerFloat = new MediaController(this);
        //设置播放文件路径
        String uriFloat = "android.resource://" + getPackageName() + "/" + R.raw.test;
        //设置播放源
        videoViewFloat.setVideoURI(Uri.parse(uriFloat));
        //设置控制器
        videoViewFloat.setMediaController(controllerFloat);
        //给控制器设置播放器控件
        controllerFloat.setMediaPlayer(videoViewFloat);
        videoViewFloat.requestFocus();//获取焦点
        //设置视频加载回调
        videoViewFloat.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                //播放视频
                videoViewFloat.start();
            }
        });

        //倒计时
        // 单个倒计时点击事件监听
        singleCountDownViewFloat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                singleCountDownViewFloat.setText("开始倒计时");
                singleCountDownViewFloat.setTextColor(Color.parseColor("#FF7198"));
                // 开启倒计时
                singleCountDownViewFloat.setTime(1000)
                        .setTimeColorHex("#FF7198")
                        .setTimePrefixText("倒计时:  ")
                        .setTimeSuffixText("  s")
                        .startCountDown();

            }
        });

        singleCountDownViewFloat.setSingleCountDownEndListener(new SingleCountDownView.SingleCountDownEndListener() {
            @Override
            public void onSingleCountDownEnd() {
                Toast.makeText(CameraActivity.this, "倒计时结束", Toast.LENGTH_SHORT).show();
                singleCountDownViewFloat.setText("开始倒计时");
                singleCountDownViewFloat.setTextColor(Color.parseColor("#BBBBBB"));
            }
        });

        if (checkCameraHardware()) {
            mHolder = flaotSurface.getHolder();//获取控制绘制的对象
            mHolder.addCallback(this);//设置回调监听
        }

        //删除
        imageViewClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                remove();
                Toast.makeText(CameraActivity.this, "点击关闭浮窗", Toast.LENGTH_SHORT).show();
            }
        });

        isScale = true;
        btnTakePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isScale) {
                    Toast.makeText(CameraActivity.this, "变小", Toast.LENGTH_SHORT).show();
                    btnTakePhoto.setImageDrawable(getResources().getDrawable(R.mipmap.course_fullscrence_icon_defa));

                    params.width = DensityUtil.dp2px(CameraActivity.this, 180);
                    params.height = DensityUtil.dp2px(CameraActivity.this, 280);

                    if (isInit) {
                        windowManager.addView(toucherLayout, params);
                    } else {
                        windowManager.updateViewLayout(toucherLayout, params);
                    }
                    videoViewFloat.setVisibility(View.GONE);
                    isScale = false;
                } else {
                    Toast.makeText(CameraActivity.this, "点击变大", Toast.LENGTH_SHORT).show();
                    btnTakePhoto.setImageDrawable(getResources().getDrawable(R.mipmap.course_fullscrence_icon_hen));

                    params.width = DensityUtil.dp2px(CameraActivity.this, 300);
                    params.height = DensityUtil.dp2px(CameraActivity.this, 600);

                    if (isInit) {
                        windowManager.addView(toucherLayout, params);
                    } else {
                        windowManager.updateViewLayout(toucherLayout, params);
                    }
                    videoViewFloat.setVisibility(View.VISIBLE);
                    isScale = true;
                }

            }
        });

        //主动计算出当前View的宽高信息.
        toucherLayout.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);

        //处理touch
        toucherLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        isMoved = false;
                        // 记录按下位置
                        lastX = event.getRawX();
                        lastY = event.getRawY();

                        start_X = event.getRawX();
                        start_Y = event.getRawY();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        isMoved = true;
                        // 记录移动后的位置
                        float moveX = event.getRawX();
                        float moveY = event.getRawY();
                        // 获取当前窗口的布局属性, 添加偏移量, 并更新界面, 实现移动
                        params.x += (int) (moveX - lastX);
                        params.y += (int) (moveY - lastY);
                        if (toucherLayout != null) {
                            windowManager.updateViewLayout(toucherLayout, params);
                        }
                        lastX = moveX;
                        lastY = moveY;
                        break;
                    case MotionEvent.ACTION_UP:

                        float fmoveX = event.getRawX();
                        float fmoveY = event.getRawY();

                        if (Math.abs(fmoveX - start_X) < offset && Math.abs(fmoveY - start_Y) < offset) {
                            isMoved = false;

                            Log.e(TAG, "isShowScale:  " + isShowScale);
                            if (!isShowScale) {
                                Intent intent = new Intent(CameraActivity.this, CameraActivity.class);
                                CameraActivity.this.startActivity(intent);

//                                remove();
                            }
                        } else {
                            isMoved = true;
                        }
                        isMoved = true;
                        break;
                }
                // 如果是移动事件, 则消费掉; 如果不是, 则由其他处理, 比如点击
                return isMoved;
            }

        });

        isInit = false;
        isShowScale = false;
    }

    public static void remove() {
        if (windowManager != null && toucherLayout != null) {
            windowManager.removeView(toucherLayout);
            isInit = true;
            Log.e(TAG, "isShowScale:  " + isShowScale);
            isShowScale = true;
        }
    }

    /**
     * 获取屏幕宽度(px)
     */
    private static int getScreenWidth(Context context) {
        return context.getResources().getDisplayMetrics().widthPixels;
    }

    /**
     * 获取屏幕高度(px)
     */
    private static int getScreenHeight(Context context) {
        return context.getResources().getDisplayMetrics().heightPixels;
    }

    /**
     * 返回键退出应用(连按两次)
     */
    private long waitTime = 2000;
    private long touchTime = 0;

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN && KeyEvent.KEYCODE_BACK == keyCode) {
            long currentTime = System.currentTimeMillis();
            if ((currentTime - touchTime) >= waitTime) {
                Toast.makeText(CameraActivity.this, "再按一次退出应用", Toast.LENGTH_SHORT).show();
                touchTime = currentTime;
            } else {
                finish();
            }
            return true;
        } else if (KeyEvent.KEYCODE_HOME == keyCode) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
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
            camera.setPreviewCallback(this);
        } catch (Exception e) {
            Log.e("获取摄像头对象失败: ", e.getMessage());
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
        Log.e(TAG, "surfaceCreated: ");
        try {
            if (mCamera == null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    //进行权限检车
                    requestPermission(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1000);
                } else {
                    mCamera = getCameraInstance();//获取摄像头对象
                    setCameraDisplayOrientation(this, cameraID, mCamera);
                    mCamera.setPreviewDisplay(mHolder);//使holder与预览相关联
                    mCamera.startPreview();//开启预览
                }
            }
        } catch (IOException e) {
            mCamera.release();//释放资源
            Log.e(TAG, "开启预览发生错误" + e.getMessage());
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
        Log.e(TAG, "surfaceChanged: ");
        if (mHolder.getSurface() == null) {
            Log.e(TAG, "surfaceChanged:纹理未被创建");
            return;
        }
        if (mCamera == null) {
            return;
        }
        try {
            mCamera.stopPreview();//先停止预览
            mCamera.setPreviewDisplay(mHolder);//使holder与预览相关联
            mCamera.startPreview();//开启预览
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "surfaceChanged: " + e.getMessage());
        }
    }

    /**
     * 当销毁时调用
     *
     * @param holder
     */
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.e(TAG, "surfaceDestroyed: ");
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
                    Log.e(TAG, "文件未被找到" + e.getMessage());
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
            Activity activity, int cameraId, Camera camera) {
        Camera.CameraInfo info =
                new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
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

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.e(TAG, "onPreviewFrame: " + data.toString());
            }
        });
    }

//    /******************************************************* 开启和关闭闪关灯的功能 ***************************************************/
//    /**
//     * 选择菜单
//     * @param menu
//     * @return
//     */
//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
////        getMenuInflater().inflate(R.menu.menu,menu);
//        return true;
//    }
//
//    private boolean isLightOpen;//闪光灯是否开启
//    /**
//     * 选择菜单
//     * @param item
//     * @return
//     */
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        switch (item.getItemId()) {
//            case R.id.light://用来开启闪光灯
//                if (null != mCamera){
//                    if (isLightOpen){//关闭闪光灯
//                        closeLight(mCamera.getParameters());
//                        isLightOpen = false;
//                    }else {//开启闪光灯
//                        openLight(mCamera.getParameters());
//                        isLightOpen = true;
//                    }
//                }
//                break;
//        }
//        return super.onOptionsItemSelected(item);
//    }
//
//    /**
//     * 开启闪光灯
//     * @param parameters
//     */
//    private void openLight(Camera.Parameters parameters){
//        parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
//        mCamera.setParameters(parameters);
//    }
//
//    /**
//     * 关闭闪光灯
//     * @param parameters
//     */
//    private void closeLight(Camera.Parameters parameters){
//        parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
//        mCamera.setParameters(parameters);
//    }

}
