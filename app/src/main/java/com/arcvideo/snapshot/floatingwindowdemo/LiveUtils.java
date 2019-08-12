package com.arcvideo.snapshot.floatingwindowdemo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.IOException;


/**
 * 创建日期：2019/8/12 on 9:27
 * 描述: Description:初始化直播弹窗工具
 * 作者: liangyang
 */
public class LiveUtils extends AppCompatActivity {

    private static final String TAG = "LiveUtils";
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
    private static ImageView imageViewClose;
    private static SurfaceView surface;

    private static SurfaceHolder mHolder;//用来管理图像的绘制
    private static Camera mCamera;//摄像头对象
    private static int degrees;//旋转角度
    private static int cameraID = Camera.CameraInfo.CAMERA_FACING_BACK;//摄像ID代表前置摄像头或后置摄像头

    @SuppressLint("ClickableViewAccessibility")
    static void initLive(final Context context) {
        try {
            windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            //赋值WindowManager&LayoutParam.
            params = new WindowManager.LayoutParams();
            //设置type.系统提示型窗口，一般都在应用程序窗口之上.
            if (Build.VERSION.SDK_INT >= 26) {//8.0新特性
                params.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
            } else {
                params.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
            }
            //设置效果为背景透明.
            params.format = PixelFormat.RGBA_8888;
            //设置flags.不可聚焦及不可使用按钮对悬浮窗进行操控.
            params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
            //设置窗口坐标参考系
            params.gravity = Gravity.LEFT | Gravity.TOP;
            //用于检测状态栏高度.
            int resourceId = context.getResources().getIdentifier("status_bar_height",
                    "dimen", "android");
            if (resourceId > 0) {
                statusBarHeight = context.getResources().getDimensionPixelSize(resourceId);
            }
            offset = DensityUtil.dp2px(context, 2);//移动偏移量
            //设置原点
            params.x = getScreenWidth(context) - DensityUtil.dp2px(context, 170);
            params.y = getScreenHeight(context) - DensityUtil.dp2px(context, 100 + 72);
            //设置悬浮窗口长宽数据.
//            params.width = DensityUtil.dp2px(context, 180);
//            params.height = DensityUtil.dp2px(context, 100);
            params.width = DensityUtil.dp2px(context, 300);
            params.height = DensityUtil.dp2px(context, 400);

            //获取浮动窗口视图所在布局.
            toucherLayout = new FrameLayout(context);
            View inflateView = LayoutInflater.from(context).inflate(R.layout.flating_window_view_item, null);
            imageViewClose = inflateView.findViewById(R.id.btn_small);
            surface = inflateView.findViewById(R.id.surface);
            ImageButton takePhoto = inflateView.findViewById(R.id.btn_take_photo);

            toucherLayout.addView(inflateView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));

//            imageViewClose = new ImageView(context);
//            imageViewClose.setImageResource(R.mipmap.icon_close);
//            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
//                    DensityUtil.dp2px(context, 26), DensityUtil.dp2px(context, 26));
//            layoutParams.gravity = Gravity.TOP | Gravity.RIGHT;
//            layoutParams.rightMargin = DensityUtil.dp2px(context, 3);
//            layoutParams.topMargin = DensityUtil.dp2px(context, 3);
//            imageViewClose.setLayoutParams(layoutParams);
//
//            toucherLayout.addView(imageViewClose,layoutParams);

            //添加toucherlayout
            if (isInit) {
                windowManager.addView(toucherLayout, params);
            } else {
                windowManager.updateViewLayout(toucherLayout, params);
            }

            isScale = true;
            takePhoto.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isScale){
                        Toast.makeText(context, "点击变小", Toast.LENGTH_SHORT).show();
                        params.width = DensityUtil.dp2px(context, 180);
                        params.height = DensityUtil.dp2px(context, 100);

                        if (isInit) {
                            windowManager.addView(toucherLayout, params);
                        } else {
                            windowManager.updateViewLayout(toucherLayout, params);
                        }
                        isScale = false;
                    }else {
                        Toast.makeText(context, "点击变大", Toast.LENGTH_SHORT).show();
                        params.width = DensityUtil.dp2px(context, 300);
                        params.height = DensityUtil.dp2px(context, 400);

                        if (isInit) {
                            windowManager.addView(toucherLayout, params);
                        } else {
                            windowManager.updateViewLayout(toucherLayout, params);
                        }
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

                                Intent intent = new Intent(context, CameraActivity.class);
                                context.startActivity(intent);

                                remove(context);

                            } else {
                                isMoved = true;
                            }
                            break;
                    }
                    // 如果是移动事件, 则消费掉; 如果不是, 则由其他处理, 比如点击
                    return isMoved;
                }

            });

            //删除
            imageViewClose.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    remove(context);
                    Toast.makeText(context, "点击关闭浮窗", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

        isInit = false;

    }

    private static void remove(Context context) {
        if (windowManager != null && toucherLayout != null) {
            windowManager.removeView(toucherLayout);
            isInit = true;
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

}
