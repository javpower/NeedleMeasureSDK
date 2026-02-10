package com.edge.vision.core;

import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Point;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.bytedeco.opencv.opencv_core.Size;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.global.opencv_imgproc;

import java.io.Closeable;
import java.io.InputStream;

/**
 * 高精度针长度测量分析器
 * 基于单模板的针长度测量工具类
 *
 * 支持平台：Windows, Mac, Linux, Android
 * @author Coder建设
 */
public class NeedleLengthAnalyzer implements Closeable {

    private final AnalysisTemplate template;
    private final double[] scales;  // 多尺度匹配的比例数组

    /**
     * 使用模板文件路径创建分析器
     *
     * @param templateFilePath 模板文件路径（PNG格式）
     */
    public NeedleLengthAnalyzer(String templateFilePath) {
        this.template = new AnalysisTemplate(templateFilePath);
        this.scales = generateScales(0.6, 1.3, 0.1);
    }

    /**
     * 使用输入流创建分析器（适用于Android等资源环境）
     *
     * @param imageInputStream 模板图像输入流
     * @param metaInputStream 元数据输入流
     */
    public NeedleLengthAnalyzer(InputStream imageInputStream, InputStream metaInputStream) {
        this.template = new AnalysisTemplate(imageInputStream, metaInputStream);
        this.scales = generateScales(0.6, 1.3, 0.1);
    }

    /**
     * 直接使用内存中的模板创建分析器
     *
     * @param template 分析模板
     */
    public NeedleLengthAnalyzer(AnalysisTemplate template) {
        this.template = template;
        this.scales = generateScales(0.6, 1.3, 0.1);
    }

    /**
     * 创建分析器并指定多尺度匹配参数
     *
     * @param templateFilePath 模板文件路径
     * @param minScale 最小缩放比例
     * @param maxScale 最大缩放比例
     * @param scaleStep 缩放步长
     */
    public NeedleLengthAnalyzer(String templateFilePath, double minScale, double maxScale, double scaleStep) {
        this.template = new AnalysisTemplate(templateFilePath);
        this.scales = generateScales(minScale, maxScale, scaleStep);
    }

    /**
     * 分析目标图像中的针长度
     *
     * @param targetImagePath 目标图像路径
     * @return 测量结果
     * @throws RuntimeException 如果分析失败
     */
    public MeasurementResult analyze(String targetImagePath) {
        long startTime = System.currentTimeMillis();

        Mat target = opencv_imgcodecs.imread(targetImagePath);
        if (target.empty()) {
            throw new RuntimeException("无法加载目标图像: " + targetImagePath);
        }

        try {
            return analyzeInternal(target, startTime, targetImagePath);
        } finally {
            target.close();
        }
    }

    /**
     * 分析目标图像（从字节数组）
     * 适用于Android等从相机获取图像的场景
     *
     * @param imageBytes 图像字节数组
     * @return 测量结果
     */
    public MeasurementResult analyze(byte[] imageBytes) {
        long startTime = System.currentTimeMillis();

        Mat target = opencv_imgcodecs.imdecode(new Mat(imageBytes), opencv_imgcodecs.IMREAD_COLOR);
        if (target.empty()) {
            throw new RuntimeException("无法解码目标图像");
        }

        try {
            return analyzeInternal(target, startTime, null);
        } finally {
            target.close();
        }
    }

    /**
     * 分析目标图像（从Mat对象）
     *
     * @param target 目标图像Mat（BGR格式）
     * @return 测量结果
     */
    public MeasurementResult analyze(Mat target) {
        long startTime = System.currentTimeMillis();
        Mat cloned = target.clone();
        try {
            return analyzeInternal(cloned, startTime, null);
        } finally {
            cloned.close();
        }
    }

    /**
     * 内部分析方法
     */
    private MeasurementResult analyzeInternal(Mat target, long startTime, String originalPath) {
        Mat targetGray = new Mat();
        opencv_imgproc.cvtColor(target, targetGray, opencv_imgproc.COLOR_BGR2GRAY);

        try {
            // 用两个针尖特征块进行全图匹配
            Point[] needleTips = findNeedleTipsByFeatureMatching(targetGray);

            Point t1 = needleTips[0];
            Point t2 = needleTips[1];

            // 计算像素长度
            double pixelLen = Math.sqrt(
                Math.pow(t2.x() - t1.x(), 2) + Math.pow(t2.y() - t1.y(), 2)
            );

            // 使用模板的 mmPerPixel 进行换算
            double mmLen = pixelLen * template.getMmPerPixel();

            // 计算置信度（基于匹配得分和长度合理性）
            double confidence = calculateConfidence(pixelLen, template.getReferenceLengthMm() / template.getMmPerPixel());

            long procTime = System.currentTimeMillis() - startTime;

            // 保存可视化结果（如果提供了路径）
            if (originalPath != null) {
                saveVisualization(target, t1, t2, mmLen, originalPath);
            }

            return new MeasurementResult(mmLen, pixelLen, t1, t2,
                confidence, procTime, template.getTemplateId());

        } finally {
            targetGray.close();
        }
    }

    /**
     * 使用针尖特征块进行全图匹配，找到针的两端
     */
    private Point[] findNeedleTipsByFeatureMatching(Mat gray) {
        int patchSize = template.getTipPatchSize();

        // 找针尖1
        MatchResult match1 = findBestMatch(gray, template.getTip1Patch(), patchSize, "Tip1");
        // 找针尖2
        MatchResult match2 = findBestMatch(gray, template.getTip2Patch(), patchSize, "Tip2");

        return new Point[] { match1.location, match2.location };
    }

    /**
     * 匹配结果内部类
     */
    private static class MatchResult {
        final Point location;
        final double score;
        final double scale;

        MatchResult(Point location, double score, double scale) {
            this.location = location;
            this.score = score;
            this.scale = scale;
        }
    }

    /**
     * 在全图中搜索最佳匹配位置（多尺度）
     *
     * @param gray 目标灰度图
     * @param feature 特征块
     * @param featureSize 特征块大小
     * @param name 特征名称（用于日志）
     * @return 最佳匹配结果
     */
    private MatchResult findBestMatch(Mat gray, Mat feature, int featureSize, String name) {
        double bestScore = -1;
        double bestScale = 1.0;
        Point bestLoc = null;

        for (double scale : scales) {
            int scaledSize = (int)(featureSize * scale);

            if (scaledSize > gray.cols() || scaledSize > gray.rows()) continue;

            // 缩放特征块
            Mat scaledFeature = new Mat();
            opencv_imgproc.resize(feature, scaledFeature, new Size(scaledSize, scaledSize));

            // 全图模板匹配
            Mat result = new Mat();
            opencv_imgproc.matchTemplate(gray, scaledFeature, result, opencv_imgproc.TM_CCOEFF_NORMED);

            // 找到最大值位置
            double[] minVal = new double[1];
            double[] maxVal = new double[1];
            Point minLoc = new Point();
            Point maxLoc = new Point();
            opencv_core.minMaxLoc(result, minVal, maxVal, minLoc, maxLoc, null);

            if (maxVal[0] > bestScore) {
                bestScore = maxVal[0];
                bestScale = scale;
                bestLoc = new Point(maxLoc.x(), maxLoc.y());
            }

            scaledFeature.close();
            result.close();
        }

        if (bestLoc == null) {
            throw new RuntimeException("特征匹配失败: " + name);
        }

        // 计算特征块中心点
        int actualSize = (int)(featureSize * bestScale);
        Point center = new Point((int)(bestLoc.x() + actualSize / 2.0), (int)(bestLoc.y() + actualSize / 2.0));

        return new MatchResult(center, bestScore, bestScale);
    }

    /**
     * 计算置信度
     *
     * @param measuredPixelLen 测量的像素长度
     * @param referencePixelLen 参考像素长度
     * @return 置信度 (0.0 - 1.0)
     */
    private double calculateConfidence(double measuredPixelLen, double referencePixelLen) {
        // 基于长度比例的置信度
        double lengthRatio = Math.min(measuredPixelLen, referencePixelLen) /
                            Math.max(measuredPixelLen, referencePixelLen);

        // 基础置信度
        double baseConfidence = 0.85;

        // 根据长度差异调整
        double adjustedConfidence = baseConfidence * lengthRatio;

        // 确保在合理范围内
        return Math.max(0.5, Math.min(0.99, adjustedConfidence));
    }

    /**
     * 生成多尺度数组
     */
    private double[] generateScales(double min, double max, double step) {
        int count = (int)((max - min) / step) + 1;
        double[] scales = new double[count];
        for (int i = 0; i < count; i++) {
            scales[i] = min + i * step;
        }
        return scales;
    }

    /**
     * 保存可视化结果
     *
     * @param image 原始图像
     * @param t1 针尖1
     * @param t2 针尖2
     * @param mm 测量长度
     * @param originalPath 原始路径
     */
    private void saveVisualization(Mat image, Point t1, Point t2,
            double mm, String originalPath) {
        Mat out = image.clone();

        // 绘制测量线
        opencv_imgproc.circle(out, t1, 8, new Scalar(0, 0, 255, 0), -1, 0, 0);
        opencv_imgproc.circle(out, t1, 10, new Scalar(255, 255, 255, 0), 2, 0, 0);
        opencv_imgproc.circle(out, t2, 8, new Scalar(0, 0, 255, 0), -1, 0, 0);
        opencv_imgproc.circle(out, t2, 10, new Scalar(255, 255, 255, 0), 2, 0, 0);
        opencv_imgproc.line(out, t1, t2, new Scalar(0, 255, 0, 0), 3, 0, 0);

        // 标注
        String label = String.format("%.3f mm", mm);
        int[] baseline = {0};
        Size textSize = opencv_imgproc.getTextSize(label, opencv_imgproc.FONT_HERSHEY_SIMPLEX, 0.8, 2, baseline);
        Point textPos = new Point((t1.x() + t2.x())/2 - textSize.width()/2,
                                  (t1.y() + t2.y())/2 - textSize.height() - 10);

        // 文字背景
        opencv_imgproc.rectangle(out,
            new Point(textPos.x() - 5, textPos.y() - textSize.height() - 5),
            new Point(textPos.x() + textSize.width() + 5, textPos.y() + baseline[0] + 5),
            new Scalar(0, 0, 0, 0), -1, 0, 0);

        // 文字
        opencv_imgproc.putText(out, label, textPos,
            opencv_imgproc.FONT_HERSHEY_SIMPLEX, 0.8, new Scalar(0, 255, 255, 0), 2, 0, false);

        // 保存
        String outPath = originalPath.replaceAll("(\\.[^.]+)$", "_analyzed$1");
        opencv_imgcodecs.imwrite(outPath, out);
        out.close();
    }

    /**
     * 生成带测量结果的图像（返回Mat，不保存到文件）
     *
     * @param image 原始图像
     * @param result 测量结果
     * @return 带标注的图像
     */
    public Mat generateVisualization(Mat image, MeasurementResult result) {
        Mat out = image.clone();
        Point t1 = result.getTip1();
        Point t2 = result.getTip2();

        // 绘制测量线
        opencv_imgproc.circle(out, t1, 8, new Scalar(0, 0, 255, 0), -1, 0, 0);
        opencv_imgproc.circle(out, t1, 10, new Scalar(255, 255, 255, 0), 2, 0, 0);
        opencv_imgproc.circle(out, t2, 8, new Scalar(0, 0, 255, 0), -1, 0, 0);
        opencv_imgproc.circle(out, t2, 10, new Scalar(255, 255, 255, 0), 2, 0, 0);
        opencv_imgproc.line(out, t1, t2, new Scalar(0, 255, 0, 0), 3, 0, 0);

        // 标注
        String label = String.format("%.3f mm", result.getLengthMm());
        int[] baseline = {0};
        Size textSize = opencv_imgproc.getTextSize(label, opencv_imgproc.FONT_HERSHEY_SIMPLEX, 0.8, 2, baseline);
        Point textPos = new Point((t1.x() + t2.x())/2 - textSize.width()/2,
                                  (t1.y() + t2.y())/2 - textSize.height() - 10);

        // 文字背景
        opencv_imgproc.rectangle(out,
            new Point(textPos.x() - 5, textPos.y() - textSize.height() - 5),
            new Point(textPos.x() + textSize.width() + 5, textPos.y() + baseline[0] + 5),
            new Scalar(0, 0, 0, 0), -1, 0, 0);

        // 文字
        opencv_imgproc.putText(out, label, textPos,
            opencv_imgproc.FONT_HERSHEY_SIMPLEX, 0.8, new Scalar(0, 255, 255, 0), 2, 0, false);

        return out;
    }

    @Override
    public void close() {
        template.close();
    }

    /**
     * 获取模板信息
     */
    public AnalysisTemplate getTemplate() {
        return template;
    }
}
