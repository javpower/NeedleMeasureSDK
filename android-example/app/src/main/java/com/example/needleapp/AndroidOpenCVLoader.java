package com.example.needleapp;

import android.content.Context;
import android.util.Log;

import com.edge.vision.platform.OpenCVLoader;

import org.opencv.core.Core;

/**
 * Android 平台 OpenCV 加载器
 * 
 * 注意：这个类需要复制到 Android 项目中，并确保放在正确的包名下。
 * 它实现了 SDK 中的 OpenCVLoader 接口。
 * @author Coder建设
 */
public class AndroidOpenCVLoader implements OpenCVLoader {
    
    private static final String TAG = "AndroidOpenCVLoader";
    private static volatile boolean loaded = false;
    private static final Object lock = new Object();
    
    private final Context context;
    
    /**
     * 创建 Android OpenCV 加载器
     * 
     * @param context Android 上下文（可以是 Activity 或 Application）
     */
    public AndroidOpenCVLoader(Object context) {
        // 转换为 Context 并获取 Application Context
        Context ctx = (Context) context;
        this.context = ctx.getApplicationContext();
    }
    
    /**
     * 创建 Android OpenCV 加载器（带静态链接参数，与接口兼容）
     * 
     * @param context Android 上下文
     * @param useStaticLinking 是否使用静态链接（此实现总是使用静态链接）
     */
    public AndroidOpenCVLoader(Object context, boolean useStaticLinking) {
        this(context);
    }
    
    @Override
    public void load() throws RuntimeException {
        if (loaded) {
            return;
        }
        
        synchronized (lock) {
            if (loaded) {
                return;
            }
            
            try {
                // 方式1: 使用 OpenCV Android SDK 的 initDebug
                boolean success = org.opencv.android.OpenCVLoader.initDebug();
                
                if (!success) {
                    // 方式2: 直接加载库
                    System.loadLibrary("opencv_java4");
                }
                
                loaded = true;
                Log.i(TAG, "OpenCV 加载成功，版本: " + Core.VERSION);
                
            } catch (Exception e) {
                throw new RuntimeException("OpenCV 加载失败: " + e.getMessage(), e);
            }
        }
    }
    
    @Override
    public boolean isLoaded() {
        return loaded;
    }
    
    @Override
    public String getPlatformName() {
        return "Android " + android.os.Build.VERSION.RELEASE + 
               " (API " + android.os.Build.VERSION.SDK_INT + ")";
    }
    
    @Override
    public String getOpenCVVersion() {
        if (!loaded) {
            return "Not loaded";
        }
        return Core.VERSION;
    }
    
    /**
     * 获取 Context
     */
    public Context getContext() {
        return context;
    }
}
