package com.edge.vision.platform;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * OpenCV初始化管理器
 * 统一处理跨平台的OpenCV初始化
 * 
 * 使用示例:
 * <pre>
 * // 桌面平台
 * OpenCVInitializer.initialize();
 * 
 * // Android平台
 * OpenCVInitializer.initialize(context);
 * </pre>
 * @author Coder建设
 */
public class OpenCVInitializer {
    
    private static final AtomicBoolean initialized = new AtomicBoolean(false);
    private static OpenCVLoader loader = null;
    private static volatile Exception lastError = null;
    
    /**
     * 初始化OpenCV（桌面平台）
     * 
     * @throws RuntimeException 如果初始化失败
     */
    public static void initialize() {
        if (initialized.get()) {
            return;
        }
        
        PlatformDetector.Platform platform = PlatformDetector.detect();
        
        if (platform == PlatformDetector.Platform.ANDROID) {
            throw new IllegalStateException(
                "Android平台需要使用 initialize(Context context) 方法");
        }
        
        synchronized (OpenCVInitializer.class) {
            if (initialized.get()) {
                return;
            }
            
            try {
                loader = new DesktopOpenCVLoader();
                loader.load();
                initialized.set(true);
                System.out.println("OpenCV initialized on " + loader.getPlatformName() + 
                    ", version: " + loader.getOpenCVVersion());
            } catch (Exception e) {
                lastError = e;
                throw new RuntimeException("OpenCV初始化失败: " + e.getMessage(), e);
            }
        }
    }
    
    /**
     * 初始化OpenCV（指定原生库路径，桌面平台）
     * 
     * @param nativeLibPath 原生库目录路径
     * @throws RuntimeException 如果初始化失败
     */
    public static void initialize(String nativeLibPath) {
        if (initialized.get()) {
            return;
        }
        
        synchronized (OpenCVInitializer.class) {
            if (initialized.get()) {
                return;
            }
            
            try {
                loader = new DesktopOpenCVLoader(nativeLibPath);
                loader.load();
                initialized.set(true);
                System.out.println("OpenCV initialized from path: " + nativeLibPath);
            } catch (Exception e) {
                lastError = e;
                throw new RuntimeException("OpenCV初始化失败: " + e.getMessage(), e);
            }
        }
    }
    
    /**
     * 初始化OpenCV（Android平台）
     * 
     * @param context Android上下文（android.content.Context）
     * @throws RuntimeException 如果初始化失败
     */
    public static void initialize(Object context) {
        if (initialized.get()) {
            return;
        }
        
        synchronized (OpenCVInitializer.class) {
            if (initialized.get()) {
                return;
            }
            
            try {
                // 使用反射加载Android特有的加载器，避免桌面平台编译问题
                Class<?> androidLoaderClass = Class.forName(
                    "com.edge.vision.android.AndroidOpenCVLoader");
                loader = (OpenCVLoader) androidLoaderClass
                    .getConstructor(Object.class)
                    .newInstance(context);
                loader.load();
                initialized.set(true);
                System.out.println("OpenCV initialized on " + loader.getPlatformName() + 
                    ", version: " + loader.getOpenCVVersion());
            } catch (ClassNotFoundException e) {
                lastError = e;
                throw new RuntimeException(
                    "AndroidOpenCVLoader未找到，请确保Android依赖已添加", e);
            } catch (Exception e) {
                lastError = e;
                throw new RuntimeException("OpenCV初始化失败: " + e.getMessage(), e);
            }
        }
    }
    
    /**
     * 初始化OpenCV（Android平台，指定是否使用静态链接）
     * 
     * @param context Android上下文（android.content.Context）
     * @param useStaticLinking 是否使用静态链接
     * @throws RuntimeException 如果初始化失败
     */
    public static void initialize(Object context, boolean useStaticLinking) {
        if (initialized.get()) {
            return;
        }
        
        synchronized (OpenCVInitializer.class) {
            if (initialized.get()) {
                return;
            }
            
            try {
                Class<?> androidLoaderClass = Class.forName(
                    "com.edge.vision.android.AndroidOpenCVLoader");
                loader = (OpenCVLoader) androidLoaderClass
                    .getConstructor(Object.class, boolean.class)
                    .newInstance(context, useStaticLinking);
                loader.load();
                initialized.set(true);
                System.out.println("OpenCV initialized on " + loader.getPlatformName() + 
                    " (static=" + useStaticLinking + "), version: " + loader.getOpenCVVersion());
            } catch (ClassNotFoundException e) {
                lastError = e;
                throw new RuntimeException(
                    "AndroidOpenCVLoader未找到，请确保Android依赖已添加", e);
            } catch (Exception e) {
                lastError = e;
                throw new RuntimeException("OpenCV初始化失败: " + e.getMessage(), e);
            }
        }
    }
    
    /**
     * 检查是否已初始化
     * 
     * @return true如果OpenCV已初始化
     */
    public static boolean isInitialized() {
        return initialized.get();
    }
    
    /**
     * 获取当前加载器
     * 
     * @return OpenCVLoader实例，如果未初始化则返回null
     */
    public static OpenCVLoader getLoader() {
        return loader;
    }
    
    /**
     * 获取最后错误
     * 
     * @return 最后的异常，如果没有则返回null
     */
    public static Exception getLastError() {
        return lastError;
    }
    
    /**
     * 重置初始化状态（用于测试）
     */
    public static void reset() {
        synchronized (OpenCVInitializer.class) {
            initialized.set(false);
            loader = null;
            lastError = null;
        }
    }
    
    /**
     * 确保已初始化（如果不初始化则抛出异常）
     * 
     * @throws IllegalStateException 如果未初始化
     */
    public static void ensureInitialized() {
        if (!initialized.get()) {
            throw new IllegalStateException(
                "OpenCV未初始化，请先调用OpenCVInitializer.initialize()");
        }
    }
}
