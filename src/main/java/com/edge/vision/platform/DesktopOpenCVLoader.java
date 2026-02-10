package com.edge.vision.platform;

import org.opencv.core.Core;

import java.io.*;
import java.nio.file.*;

/**
 * 桌面平台OpenCV加载器
 * 支持Windows、Mac、Linux
 * 
 * 支持多种加载方式：
 * 1. 系统库路径（已安装的OpenCV）
 * 2. 内嵌原生库（从jar包提取）
 * 3. nu.pattern.OpenCV（桌面开发依赖）
 * @author Coder建设
 */
public class DesktopOpenCVLoader implements OpenCVLoader {
    
    private static volatile boolean loaded = false;
    private static final Object lock = new Object();
    
    private final PlatformDetector.Platform platform;
    private String nativeLibPath = null;
    
    public DesktopOpenCVLoader() {
        this.platform = PlatformDetector.detect();
    }
    
    /**
     * 指定原生库路径创建加载器
     * 
     * @param nativeLibPath 原生库目录路径（包含dll/so/dylib文件）
     */
    public DesktopOpenCVLoader(String nativeLibPath) {
        this();
        this.nativeLibPath = nativeLibPath;
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
            
            // 尝试多种加载方式
            Exception lastException = null;
            
            // 方式1: 如果指定了原生库路径，优先使用
            if (nativeLibPath != null) {
                try {
                    loadFromPath(nativeLibPath);
                    loaded = true;
                    return;
                } catch (Exception e) {
                    lastException = e;
                    System.err.println("从指定路径加载OpenCV失败: " + e.getMessage());
                }
            }
            
            // 方式2: 尝试使用nu.pattern.OpenCV（如果可用）
            try {
                loadViaNuPattern();
                loaded = true;
                return;
            } catch (Exception e) {
                lastException = e;
                System.err.println("nu.pattern加载失败: " + e.getMessage());
            }
            
            // 方式3: 尝试从系统库路径加载
            try {
                loadFromSystem();
                loaded = true;
                return;
            } catch (Exception e) {
                lastException = e;
                System.err.println("系统库加载失败: " + e.getMessage());
            }
            
            // 方式4: 尝试从jar包提取并加载
            try {
                loadFromEmbedded();
                loaded = true;
                return;
            } catch (Exception e) {
                lastException = e;
                System.err.println("内嵌库加载失败: " + e.getMessage());
            }
            
            // 所有方式都失败
            throw new RuntimeException("无法加载OpenCV库，请确保OpenCV已正确安装或配置。" +
                "最后错误: " + (lastException != null ? lastException.getMessage() : "未知"), lastException);
        }
    }
    
    /**
     * 使用nu.pattern.OpenCV加载（桌面开发常用）
     */
    private void loadViaNuPattern() {
        try {
            Class<?> openCVClass = Class.forName("nu.pattern.OpenCV");
            java.lang.reflect.Method loadSharedMethod = openCVClass.getMethod("loadShared");
            loadSharedMethod.invoke(null);
            System.out.println("OpenCV loaded via nu.pattern");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("nu.pattern.OpenCV未找到，请添加依赖", e);
        } catch (Exception e) {
            throw new RuntimeException("nu.pattern.OpenCV加载失败", e);
        }
    }
    
    /**
     * 从系统库路径加载
     */
    private void loadFromSystem() {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        System.out.println("OpenCV loaded from system library path");
    }
    
    /**
     * 从指定路径加载
     */
    private void loadFromPath(String path) {
        String libName = getLibraryName();
        Path libPath = Paths.get(path, libName);
        
        if (!Files.exists(libPath)) {
            throw new RuntimeException("库文件不存在: " + libPath);
        }
        
        System.load(libPath.toAbsolutePath().toString());
        System.out.println("OpenCV loaded from: " + libPath);
    }
    
    /**
     * 从jar包提取并加载原生库
     */
    private void loadFromEmbedded() throws IOException {
        String libName = getLibraryName();
        String resourcePath = "/native/" + platform.name().toLowerCase() + "/" + libName;
        
        // 从资源中提取
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new RuntimeException("内嵌库资源未找到: " + resourcePath);
            }
            
            // 创建临时文件
            Path tempDir = Files.createTempDirectory("opencv_native");
            tempDir.toFile().deleteOnExit();
            
            Path tempLib = tempDir.resolve(libName);
            Files.copy(is, tempLib, StandardCopyOption.REPLACE_EXISTING);
            tempLib.toFile().deleteOnExit();
            
            // 加载
            System.load(tempLib.toAbsolutePath().toString());
            System.out.println("OpenCV loaded from embedded resource: " + resourcePath);
        }
    }
    
    /**
     * 获取平台特定的库文件名
     */
    private String getLibraryName() {
        String prefix = PlatformDetector.getLibraryPrefix();
        String suffix = PlatformDetector.getLibraryExtension();
        String arch = PlatformDetector.is64Bit() ? "64" : "32";
        
        // OpenCV库名格式: opencv_java{version}{arch}.{ext}
        String version = String.valueOf(Core.VERSION_MAJOR) + Core.VERSION_MINOR;
        
        switch (platform) {
            case WINDOWS:
                return prefix + "opencv_java" + version + arch + suffix;
            case MAC:
                return prefix + "opencv_java" + version + suffix;
            case LINUX:
                return prefix + "opencv_java" + version + suffix;
            default:
                throw new RuntimeException("不支持的平台: " + platform);
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
        return Core.VERSION;
    }
    
    /**
     * 手动设置原生库路径（用于自定义部署场景）
     * 
     * @param path 原生库目录路径
     */
    public static void setNativeLibraryPath(String path) {
        System.setProperty("java.library.path", path);
    }
}
