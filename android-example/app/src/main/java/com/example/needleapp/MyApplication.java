package com.example.needleapp;

import android.app.Application;
import android.util.Log;

import com.edge.vision.platform.OpenCVInitializer;

/**
 * Application 类
 * 
 * 用于在应用启动时初始化 OpenCV
 * @author Coder建设
 */
public class MyApplication extends Application {
    
    private static final String TAG = "MyApplication";
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // 初始化 OpenCV
        initOpenCV();
    }
    
    private void initOpenCV() {
        try {
            // 方式1: 使用静态链接（推荐，无需 OpenCV Manager）
            OpenCVInitializer.initialize(this, true);
            Log.i(TAG, "OpenCV 初始化成功");
            
        } catch (Exception e) {
            Log.e(TAG, "OpenCV 初始化失败: " + e.getMessage(), e);
        }
    }
}
