package com.edge.vision.platform;

/**
 * 平台检测工具类
 * 自动检测当前运行平台
 * @author Coder建设
 */
public class PlatformDetector {
    
    public enum Platform {
        WINDOWS,
        MAC,
        LINUX,
        ANDROID,
        UNKNOWN
    }
    
    private static final String OS_NAME = System.getProperty("os.name", "").toLowerCase();
    private static final String OS_ARCH = System.getProperty("os.arch", "").toLowerCase();
    private static final String JAVA_RUNTIME = System.getProperty("java.runtime.name", "").toLowerCase();
    
    /**
     * 检测当前平台
     * 
     * @return 当前平台类型
     */
    public static Platform detect() {
        // 首先检测Android（通过系统属性或运行时环境）
        if (isAndroid()) {
            return Platform.ANDROID;
        }
        
        // 检测Windows
        if (OS_NAME.contains("win")) {
            return Platform.WINDOWS;
        }
        
        // 检测Mac
        if (OS_NAME.contains("mac")) {
            return Platform.MAC;
        }
        
        // 检测Linux
        if (OS_NAME.contains("linux")) {
            return Platform.LINUX;
        }
        
        return Platform.UNKNOWN;
    }
    
    /**
     * 检测是否为Android平台
     * 通过检查Dalvik/ART运行时或Android特定的系统属性
     */
    public static boolean isAndroid() {
        // 检查运行时名称
        if (JAVA_RUNTIME.contains("android")) {
            return true;
        }
        
        // 检查系统属性
        try {
            String vmVersion = System.getProperty("java.vm.version", "");
            String vmName = System.getProperty("java.vm.name", "");
            
            // Dalvik/ART检测
            if (vmName.toLowerCase().contains("dalvik") || 
                vmName.toLowerCase().contains("art")) {
                return true;
            }
            
            // Android SDK检测
            Class.forName("android.app.Activity");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    /**
     * 检测是否为桌面平台（Windows/Mac/Linux）
     */
    public static boolean isDesktop() {
        Platform platform = detect();
        return platform == Platform.WINDOWS || 
               platform == Platform.MAC || 
               platform == Platform.LINUX;
    }
    
    /**
     * 检测是否为64位架构
     */
    public static boolean is64Bit() {
        return OS_ARCH.contains("64");
    }
    
    /**
     * 获取平台特定的库文件扩展名
     */
    public static String getLibraryExtension() {
        Platform platform = detect();
        switch (platform) {
            case WINDOWS:
                return ".dll";
            case MAC:
                return ".dylib";
            case LINUX:
                return ".so";
            case ANDROID:
                return ".so";
            default:
                return "";
        }
    }
    
    /**
     * 获取平台特定的库文件前缀
     */
    public static String getLibraryPrefix() {
        Platform platform = detect();
        switch (platform) {
            case WINDOWS:
                return "";
            case MAC:
            case LINUX:
            case ANDROID:
                return "lib";
            default:
                return "";
        }
    }
    
    /**
     * 获取当前平台的详细信息
     */
    public static String getPlatformInfo() {
        return String.format("Platform: %s, OS: %s, Arch: %s, Java: %s",
            detect(), OS_NAME, OS_ARCH, System.getProperty("java.version"));
    }
}
