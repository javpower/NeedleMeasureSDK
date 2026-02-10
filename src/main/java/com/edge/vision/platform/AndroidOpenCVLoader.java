package com.edge.vision.platform;

import org.bytedeco.opencv.global.opencv_core;

/**
 * Android 平台 OpenCV 加载器 (JavaCV版本)
 *
 * 使用反射避免编译时对 Android SDK 的依赖
 * 在运行时会自动适配 Android 环境
 *
 * 注意：这个类会自动检测是否在 Android 平台运行
 * @author Coder建设
 */
public class AndroidOpenCVLoader implements OpenCVLoader {

    private static volatile boolean loaded = false;
    private static final Object lock = new Object();

    private Object context;

    /**
     * 创建 Android OpenCV 加载器
     *
     * @param context Android 上下文（android.content.Context）
     */
    public AndroidOpenCVLoader(Object context) {
        this.context = context;
    }

    /**
     * 创建 Android OpenCV 加载器（带静态链接参数，与接口兼容）
     *
     * @param context Android 上下文
     * @param useStaticLinking 是否使用静态链接（JavaCV忽略此参数）
     */
    public AndroidOpenCVLoader(Object context, boolean useStaticLinking) {
        this.context = context;
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
                // JavaCV 自动加载原生库
                // 只需要访问 OpenCV 类即可触发加载
                String version = opencv_core.CV_VERSION;

                loaded = true;

                // 使用反射调用 Android Log（避免编译时依赖）
                logInfo("JavaCV OpenCV loaded on " + getPlatformName() +
                    ", version: " + version);

            } catch (Exception e) {
                throw new RuntimeException("JavaCV OpenCV加载失败: " + e.getMessage(), e);
            }
        }
    }

    @Override
    public boolean isLoaded() {
        return loaded;
    }

    @Override
    public String getPlatformName() {
        try {
            // 使用反射获取 Android 版本信息
            Class<?> buildVersionClass = Class.forName("android.os.Build$VERSION");
            String release = (String) buildVersionClass.getField("RELEASE").get(null);
            int sdkInt = buildVersionClass.getField("SDK_INT").getInt(null);
            return "Android " + release + " (API " + sdkInt + ")";
        } catch (Exception e) {
            return "Android (unknown version)";
        }
    }

    @Override
    public String getOpenCVVersion() {
        if (!loaded) {
            return "Not loaded";
        }
        return opencv_core.CV_VERSION;
    }

    /**
     * 获取 Context
     */
    public Object getContext() {
        return context;
    }

    /**
     * 使用反射调用 Android Log.i（避免编译时依赖）
     */
    private void logInfo(String message) {
        try {
            Class<?> logClass = Class.forName("android.util.Log");
            java.lang.reflect.Method iMethod = logClass.getMethod("i", String.class, String.class);
            iMethod.invoke(null, "AndroidOpenCVLoader", message);
        } catch (Exception e) {
            // 如果不在 Android 环境，使用标准输出
            System.out.println("[INFO] " + message);
        }
    }
}
