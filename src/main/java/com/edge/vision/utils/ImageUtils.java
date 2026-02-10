package com.edge.vision.utils;

import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.opencv.global.opencv_imgcodecs;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 图像工具类
 * 提供跨平台的图像处理辅助方法
 * @author Coder建设
 */
public class ImageUtils {

    /**
     * 从输入流读取图像
     *
     * @param is 输入流
     * @return Mat图像
     */
    public static Mat readFromStream(InputStream is) {
        byte[] bytes = readAllBytes(is);
        return opencv_imgcodecs.imdecode(new Mat(bytes), opencv_imgcodecs.IMREAD_COLOR);
    }

    /**
     * 从字节数组读取图像
     *
     * @param bytes 图像字节数组
     * @return Mat图像
     */
    public static Mat readFromBytes(byte[] bytes) {
        return opencv_imgcodecs.imdecode(new Mat(bytes), opencv_imgcodecs.IMREAD_COLOR);
    }

    /**
     * 将图像编码为字节数组
     *
     * @param image 图像
     * @param format 格式（如".jpg", ".png"）
     * @return 字节数组
     */
    public static byte[] toBytes(Mat image, String format) {
        BytePointer buf = new BytePointer();
        opencv_imgcodecs.imencode(format, image, buf);
        byte[] bytes = new byte[(int)buf.limit()];
        buf.get(bytes);
        buf.close();
        return bytes;
    }

    /**
     * 将图像编码为PNG字节数组
     *
     * @param image 图像
     * @return PNG字节数组
     */
    public static byte[] toPngBytes(Mat image) {
        return toBytes(image, ".png");
    }

    /**
     * 将图像编码为JPEG字节数组
     *
     * @param image 图像
     * @return JPEG字节数组
     */
    public static byte[] toJpegBytes(Mat image) {
        return toBytes(image, ".jpg");
    }

    /**
     * 读取输入流的所有字节
     */
    private static byte[] readAllBytes(InputStream is) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) != -1) {
                baos.write(buffer, 0, read);
            }
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("读取输入流失败", e);
        }
    }

    /**
     * 安全释放Mat资源
     *
     * @param mats 要释放的Mat数组
     */
    public static void safeRelease(Mat... mats) {
        for (Mat mat : mats) {
            if (mat != null && !mat.empty()) {
                mat.close();
            }
        }
    }

    /**
     * 检查图像是否有效
     *
     * @param mat 图像
     * @return true如果图像有效
     */
    public static boolean isValid(Mat mat) {
        return mat != null && !mat.empty() && mat.cols() > 0 && mat.rows() > 0;
    }

    /**
     * 获取图像信息字符串
     *
     * @param mat 图像
     * @return 信息字符串
     */
    public static String getInfo(Mat mat) {
        if (mat == null) {
            return "Mat is null";
        }
        if (mat.empty()) {
            return "Mat is empty";
        }
        return String.format("Mat[%dx%d, channels=%d, type=%d]",
            mat.cols(), mat.rows(), mat.channels(), mat.type());
    }
}
