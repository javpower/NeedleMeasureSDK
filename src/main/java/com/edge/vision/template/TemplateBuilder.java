package com.edge.vision.template;

import com.edge.vision.core.AnalysisTemplate;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Point;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.global.opencv_imgcodecs;

/**
 * 模板构建器
 * 用于程序化创建测量模板（无GUI）
 *
 * 使用示例:
 * <pre>
 * TemplateBuilder builder = new TemplateBuilder()
 *     .setImage(image)
 *     .setReferenceLength(50.0)
 *     .setTip1(100, 200)
 *     .setTip2(400, 200);
 *
 * AnalysisTemplate template = builder.build();
 * template.save("/path/to/template");
 * </pre>
 * @author Coder建设
 */
public class TemplateBuilder {

    private Mat image;
    private double referenceLengthMm = 0;
    private Point tip1;
    private Point tip2;
    private String templateId = "template_" + System.currentTimeMillis();
    private int tipPatchSize = AnalysisTemplate.DEFAULT_TIP_PATCH_SIZE;
    private int margin = 30;  // 裁剪边距

    /**
     * 设置模板图像
     *
     * @param image 模板图像（BGR格式）
     * @return this
     */
    public TemplateBuilder setImage(Mat image) {
        this.image = image.clone();
        return this;
    }

    /**
     * 从文件加载模板图像
     *
     * @param imagePath 图像文件路径
     * @return this
     */
    public TemplateBuilder loadImage(String imagePath) {
        this.image = opencv_imgcodecs.imread(imagePath);
        if (this.image.empty()) {
            throw new RuntimeException("无法加载图像: " + imagePath);
        }
        return this;
    }

    /**
     * 设置参考长度（毫米）
     *
     * @param lengthMm 实际长度（毫米）
     * @return this
     */
    public TemplateBuilder setReferenceLength(double lengthMm) {
        if (lengthMm <= 0) {
            throw new IllegalArgumentException("长度必须大于0");
        }
        this.referenceLengthMm = lengthMm;
        return this;
    }

    /**
     * 设置针尖1坐标
     *
     * @param x X坐标
     * @param y Y坐标
     * @return this
     */
    public TemplateBuilder setTip1(double x, double y) {
        this.tip1 = new Point((int)x, (int)y);
        return this;
    }

    /**
     * 设置针尖2坐标
     *
     * @param x X坐标
     * @param y Y坐标
     * @return this
     */
    public TemplateBuilder setTip2(double x, double y) {
        this.tip2 = new Point((int)x, (int)y);
        return this;
    }

    /**
     * 设置两个针尖坐标
     *
     * @param tip1 针尖1
     * @param tip2 针尖2
     * @return this
     */
    public TemplateBuilder setTips(Point tip1, Point tip2) {
        this.tip1 = tip1;
        this.tip2 = tip2;
        return this;
    }

    /**
     * 设置模板ID
     *
     * @param id 模板标识
     * @return this
     */
    public TemplateBuilder setTemplateId(String id) {
        this.templateId = id;
        return this;
    }

    /**
     * 设置针尖特征块大小
     *
     * @param size 特征块大小（像素）
     * @return this
     */
    public TemplateBuilder setTipPatchSize(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("特征块大小必须大于0");
        }
        this.tipPatchSize = size;
        return this;
    }

    /**
     * 设置裁剪边距
     *
     * @param margin 边距（像素）
     * @return this
     */
    public TemplateBuilder setMargin(int margin) {
        if (margin < 0) {
            throw new IllegalArgumentException("边距不能为负数");
        }
        this.margin = margin;
        return this;
    }

    /**
     * 构建模板
     *
     * @return AnalysisTemplate实例
     * @throws IllegalStateException 如果参数不完整
     */
    public AnalysisTemplate build() {
        validate();

        // 计算包围两个端点的矩形区域
        int x1 = (int)Math.min(tip1.x(), tip2.x()) - margin;
        int y1 = (int)Math.min(tip1.y(), tip2.y()) - margin;
        int x2 = (int)Math.max(tip1.x(), tip2.x()) + margin;
        int y2 = (int)Math.max(tip1.y(), tip2.y()) + margin;

        // 确保在图像范围内
        x1 = Math.max(0, x1);
        y1 = Math.max(0, y1);
        x2 = Math.min(image.cols(), x2);
        y2 = Math.min(image.rows(), y2);

        Rect roi = new Rect(x1, y1, x2 - x1, y2 - y1);
        Mat templateRoi = new Mat(image, roi);

        // 计算针尖在裁剪图中的坐标
        Point savedTip1 = new Point(tip1.x() - x1, tip1.y() - y1);
        Point savedTip2 = new Point(tip2.x() - x1, tip2.y() - y1);

        // 创建模板
        AnalysisTemplate template = new AnalysisTemplate(
            templateId, templateRoi, referenceLengthMm, savedTip1, savedTip2, tipPatchSize);

        templateRoi.close();

        return template;
    }

    /**
     * 构建并保存模板
     *
     * @param outputPath 输出路径（不含扩展名）
     * @return 保存的元数据文件路径
     */
    public String buildAndSave(String outputPath) {
        AnalysisTemplate template = build();
        try {
            return template.save(outputPath);
        } finally {
            template.close();
        }
    }

    private void validate() {
        if (image == null || image.empty()) {
            throw new IllegalStateException("模板图像未设置");
        }
        if (referenceLengthMm <= 0) {
            throw new IllegalStateException("参考长度未设置或无效");
        }
        if (tip1 == null || tip2 == null) {
            throw new IllegalStateException("针尖坐标未设置");
        }
        if (tip1.x() < 0 || tip1.x() >= image.cols() || tip1.y() < 0 || tip1.y() >= image.rows()) {
            throw new IllegalStateException("针尖1坐标超出图像范围");
        }
        if (tip2.x() < 0 || tip2.x() >= image.cols() || tip2.y() < 0 || tip2.y() >= image.rows()) {
            throw new IllegalStateException("针尖2坐标超出图像范围");
        }
    }

    /**
     * 清理资源
     */
    public void release() {
        if (image != null) {
            image.close();
            image = null;
        }
    }
}
