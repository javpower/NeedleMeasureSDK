package com.edge.vision.platform;

/**
 * OpenCV加载器接口
 * 不同平台实现此接口来提供平台特定的OpenCV加载方式
 * @author Coder建设
 */
public interface OpenCVLoader {
    
    /**
     * 加载OpenCV库
     * 
     * @throws RuntimeException 如果加载失败
     */
    void load() throws RuntimeException;
    
    /**
     * 检查OpenCV是否已加载
     * 
     * @return true如果已加载
     */
    boolean isLoaded();
    
    /**
     * 获取平台名称
     * 
     * @return 平台标识名称
     */
    String getPlatformName();
    
    /**
     * 获取OpenCV版本
     * 
     * @return OpenCV版本字符串
     */
    String getOpenCVVersion();
}
