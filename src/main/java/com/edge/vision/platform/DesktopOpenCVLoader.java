package com.edge.vision.platform;

import org.bytedeco.opencv.global.opencv_core;

/**
 * 桌面平台OpenCV加载器
 * 支持Windows、Mac、Linux
 *
 * 使用 JavaCV，原生库自动加载，无需手动配置
 * @author Coder建设
 */
public class DesktopOpenCVLoader implements OpenCVLoader {

    private static volatile boolean loaded = false;
    private static final Object lock = new Object();

    private final PlatformDetector.Platform platform;

    public DesktopOpenCVLoader() {
        this.platform = PlatformDetector.detect();
    }

    /**
     * 兼容旧API的构造函数
     *
     * @param nativeLibPath 原生库目录路径（JavaCV忽略此参数）
     */
    public DesktopOpenCVLoader(String nativeLibPath) {
        this();
        // JavaCV 自动管理原生库，忽略手动指定的路径
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
                System.out.println("JavaCV OpenCV loaded on " + getPlatformName() +
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
        return "Desktop " + platform.name();
    }

    @Override
    public String getOpenCVVersion() {
        if (!loaded) {
            return "Not loaded";
        }
        return opencv_core.CV_VERSION;
    }

    /**
     * 手动设置原生库路径（兼容旧API，JavaCV忽略此设置）
     *
     * @param path 原生库目录路径
     */
    public static void setNativeLibraryPath(String path) {
        // JavaCV 自动管理原生库路径，此方法保留用于兼容性
        System.setProperty("java.library.path", path);
    }
}
