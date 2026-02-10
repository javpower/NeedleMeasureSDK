package com.edge.vision.core;

import org.opencv.core.Point;

import java.io.Serializable;

/**
 * 测量结果数据类
 * 包含针长度测量的所有结果信息
 * @author Coder建设
 */
public class MeasurementResult implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final double lengthMm;           // 测量长度(毫米)
    private final double pixelLength;        // 像素长度
    private final Point tip1;                // 针尖1坐标(亚像素精度)
    private final Point tip2;                // 针尖2坐标(亚像素精度)
    private final double confidence;         // 测量置信度(0.0-1.0)
    private final long processingTimeMs;     // 处理耗时(毫秒)
    private final String templateId;         // 使用的模板标识

    public MeasurementResult(double mm, double px, Point t1, Point t2,
                             double conf, long time, String template) {
        this.lengthMm = mm;
        this.pixelLength = px;
        this.tip1 = new Point(t1.x, t1.y);
        this.tip2 = new Point(t2.x, t2.y);
        this.confidence = conf;
        this.processingTimeMs = time;
        this.templateId = template;
    }

    public double getLengthMm() {
        return lengthMm;
    }

    public double getPixelLength() {
        return pixelLength;
    }

    public Point getTip1() {
        return new Point(tip1.x, tip1.y);
    }

    public Point getTip2() {
        return new Point(tip2.x, tip2.y);
    }

    public double getConfidence() {
        return confidence;
    }

    public long getProcessingTimeMs() {
        return processingTimeMs;
    }

    public String getTemplateId() {
        return templateId;
    }

    @Override
    public String toString() {
        return String.format(
            "MeasurementResult{length=%.4fmm, pixel=%.3f, confidence=%.3f, time=%dms}",
            lengthMm, pixelLength, confidence, processingTimeMs
        );
    }

    public String toFormattedString() {
        return String.format(
            "测量结果\n========\n长度: %.4f mm\n像素: %.3f px\n置信度: %.2f%%\n耗时: %d ms\n模板: %s",
            lengthMm, pixelLength, confidence * 100, processingTimeMs, templateId
        );
    }
    
    /**
     * 获取JSON格式的结果字符串
     */
    public String toJsonString() {
        return String.format(
            "{\"lengthMm\":%.4f,\"pixelLength\":%.3f,\"tip1\":{\"x\":%.2f,\"y\":%.2f},\"tip2\":{\"x\":%.2f,\"y\":%.2f},\"confidence\":%.3f,\"processingTimeMs\":%d,\"templateId\":\"%s\"}",
            lengthMm, pixelLength, tip1.x, tip1.y, tip2.x, tip2.y, 
            confidence, processingTimeMs, templateId
        );
    }
}
