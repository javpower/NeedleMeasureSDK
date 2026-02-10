package com.edge.vision.core;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

/**
 * 分析模板数据类
 * 包含模板图像、参考长度、针尖位置等信息
 * @author Coder建设
 */
public class AnalysisTemplate implements Closeable {
    
    private final String templateId;
    private final Mat templateImage;
    private final Mat grayImage;
    private final double referenceLengthMm;
    private final double mmPerPixel;
    private final Point referenceTip1;
    private final Point referenceTip2;
    private final Mat tip1Patch;  // 针尖1的小区域特征
    private final Mat tip2Patch;  // 针尖2的小区域特征
    private final LocalDateTime createdAt;
    private final int tipPatchSize;

    public static final int DEFAULT_TIP_PATCH_SIZE = 30;  // 针尖特征块大小

    /**
     * 从建模结果创建模板
     * 
     * @param id 模板ID
     * @param image 模板图像
     * @param lengthMm 参考长度（毫米）
     * @param tip1 针尖1坐标
     * @param tip2 针尖2坐标
     * @param tipPatchSize 针尖特征块大小
     */
    public AnalysisTemplate(String id, Mat image, double lengthMm,
                            Point tip1, Point tip2, int tipPatchSize) {
        this.templateId = id;
        this.templateImage = image.clone();
        this.referenceLengthMm = lengthMm;
        this.referenceTip1 = new Point(tip1.x, tip1.y);
        this.referenceTip2 = new Point(tip2.x, tip2.y);
        this.tipPatchSize = tipPatchSize;
        this.createdAt = LocalDateTime.now();

        // 计算像素比例
        double pixelDist = Math.sqrt(
            Math.pow(tip2.x - tip1.x, 2) +
            Math.pow(tip2.y - tip1.y, 2)
        );
        this.mmPerPixel = lengthMm / pixelDist;

        // 提取灰度图
        this.grayImage = new Mat();
        if (templateImage.channels() >= 3) {
            Imgproc.cvtColor(templateImage, grayImage, Imgproc.COLOR_BGR2GRAY);
        } else {
            templateImage.copyTo(grayImage);
        }

        // 提取针尖区域特征
        this.tip1Patch = extractTipPatch(grayImage, tip1);
        this.tip2Patch = extractTipPatch(grayImage, tip2);
    }
    
    /**
     * 使用默认特征块大小创建模板
     */
    public AnalysisTemplate(String id, Mat image, double lengthMm,
                            Point tip1, Point tip2) {
        this(id, image, lengthMm, tip1, tip2, DEFAULT_TIP_PATCH_SIZE);
    }

    /**
     * 从文件加载模板
     * 
     * @param templateFilePath 模板文件路径（PNG格式）
     * @throws RuntimeException 如果加载失败
     */
    public AnalysisTemplate(String templateFilePath) {
        // 读取模板图像
        this.templateImage = Imgcodecs.imread(templateFilePath);
        if (templateImage.empty()) {
            throw new RuntimeException("无法加载模板文件: " + templateFilePath);
        }

        // 读取同目录下的元数据文件
        String metaPath = templateFilePath.replaceAll("\\.[^.]+$", "") + ".meta";
        Properties props = new Properties();

        try (InputStream is = new FileInputStream(metaPath)) {
            props.load(is);
            this.templateId = props.getProperty("template.id", "UNKNOWN");
            this.referenceLengthMm = Double.parseDouble(props.getProperty("needle.length.mm", "0"));
            this.referenceTip1 = new Point(
                Double.parseDouble(props.getProperty("tip1.x", "0")),
                Double.parseDouble(props.getProperty("tip1.y", "0"))
            );
            this.referenceTip2 = new Point(
                Double.parseDouble(props.getProperty("tip2.x", "0")),
                Double.parseDouble(props.getProperty("tip2.y", "0"))
            );
            this.tipPatchSize = Integer.parseInt(props.getProperty("tip.patch.size", 
                String.valueOf(DEFAULT_TIP_PATCH_SIZE)));
        } catch (Exception e) {
            throw new RuntimeException("无法加载模板元数据: " + metaPath, e);
        }

        this.createdAt = LocalDateTime.now();

        double pixelDist = Math.sqrt(
            Math.pow(referenceTip2.x - referenceTip1.x, 2) +
            Math.pow(referenceTip2.y - referenceTip1.y, 2)
        );
        this.mmPerPixel = referenceLengthMm / pixelDist;

        // 提取灰度图
        this.grayImage = new Mat();
        if (templateImage.channels() >= 3) {
            Imgproc.cvtColor(templateImage, grayImage, Imgproc.COLOR_BGR2GRAY);
        } else {
            templateImage.copyTo(grayImage);
        }

        // 提取针尖特征块
        this.tip1Patch = extractTipPatch(grayImage, referenceTip1);
        this.tip2Patch = extractTipPatch(grayImage, referenceTip2);
    }
    
    /**
     * 从输入流加载模板（适用于Android等资源环境）
     * 
     * @param imageInputStream 模板图像输入流
     * @param metaInputStream 元数据输入流
     * @throws RuntimeException 如果加载失败
     */
    public AnalysisTemplate(InputStream imageInputStream, InputStream metaInputStream) {
        // 读取模板图像
        byte[] imageBytes = readAllBytes(imageInputStream);
        this.templateImage = Imgcodecs.imdecode(new MatOfByte(imageBytes), Imgcodecs.IMREAD_COLOR);
        
        if (templateImage.empty()) {
            throw new RuntimeException("无法从输入流加载模板图像");
        }

        // 读取元数据
        Properties props = new Properties();
        try {
            props.load(metaInputStream);
            this.templateId = props.getProperty("template.id", "UNKNOWN");
            this.referenceLengthMm = Double.parseDouble(props.getProperty("needle.length.mm", "0"));
            this.referenceTip1 = new Point(
                Double.parseDouble(props.getProperty("tip1.x", "0")),
                Double.parseDouble(props.getProperty("tip1.y", "0"))
            );
            this.referenceTip2 = new Point(
                Double.parseDouble(props.getProperty("tip2.x", "0")),
                Double.parseDouble(props.getProperty("tip2.y", "0"))
            );
            this.tipPatchSize = Integer.parseInt(props.getProperty("tip.patch.size", 
                String.valueOf(DEFAULT_TIP_PATCH_SIZE)));
        } catch (Exception e) {
            throw new RuntimeException("无法从输入流加载模板元数据", e);
        }

        this.createdAt = LocalDateTime.now();

        double pixelDist = Math.sqrt(
            Math.pow(referenceTip2.x - referenceTip1.x, 2) +
            Math.pow(referenceTip2.y - referenceTip1.y, 2)
        );
        this.mmPerPixel = referenceLengthMm / pixelDist;

        // 提取灰度图
        this.grayImage = new Mat();
        if (templateImage.channels() >= 3) {
            Imgproc.cvtColor(templateImage, grayImage, Imgproc.COLOR_BGR2GRAY);
        } else {
            templateImage.copyTo(grayImage);
        }

        // 提取针尖特征块
        this.tip1Patch = extractTipPatch(grayImage, referenceTip1);
        this.tip2Patch = extractTipPatch(grayImage, referenceTip2);
    }

    /**
     * 提取针尖周围的小区域特征
     */
    private Mat extractTipPatch(Mat gray, Point tip) {
        int halfSize = tipPatchSize / 2;
        int x1 = Math.max(0, (int)tip.x - halfSize);
        int y1 = Math.max(0, (int)tip.y - halfSize);
        int x2 = Math.min(gray.cols(), (int)tip.x + halfSize);
        int y2 = Math.min(gray.rows(), (int)tip.y + halfSize);

        if (x2 - x1 < tipPatchSize || y2 - y1 < tipPatchSize) {
            // 边界情况，创建空白特征
            return new Mat(tipPatchSize, tipPatchSize, gray.type(), new Scalar(0));
        }

        Rect roi = new Rect(x1, y1, tipPatchSize, tipPatchSize);
        return new Mat(gray, roi).clone();
    }
    
    private byte[] readAllBytes(InputStream is) {
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
     * 保存模板到文件
     * 
     * @param outputPath 输出路径（不含扩展名）
     * @return 保存的元数据文件路径
     */
    public String save(String outputPath) {
        String imagePath = outputPath + ".png";
        String metaPath = outputPath + ".meta";
        
        // 保存图像
        boolean saved = Imgcodecs.imwrite(imagePath, templateImage);
        if (!saved) {
            throw new RuntimeException("保存模板图像失败: " + imagePath);
        }
        
        // 保存元数据
        try (FileWriter writer = new FileWriter(metaPath)) {
            writer.write("# 针模板元数据\n");
            writer.write("template.id=" + templateId + "\n");
            writer.write("template.created=" +
                createdAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "\n");
            writer.write("needle.length.mm=" + referenceLengthMm + "\n");
            writer.write("tip1.x=" + referenceTip1.x + "\n");
            writer.write("tip1.y=" + referenceTip1.y + "\n");
            writer.write("tip2.x=" + referenceTip2.x + "\n");
            writer.write("tip2.y=" + referenceTip2.y + "\n");
            writer.write("tip.patch.size=" + tipPatchSize + "\n");
            writer.write("mm.per.pixel=" + mmPerPixel + "\n");
        } catch (IOException e) {
            throw new RuntimeException("保存模板元数据失败: " + metaPath, e);
        }
        
        return metaPath;
    }

    @Override
    public void close() {
        templateImage.release();
        grayImage.release();
        tip1Patch.release();
        tip2Patch.release();
    }

    // Getters
    public String getTemplateId() { return templateId; }
    public double getMmPerPixel() { return mmPerPixel; }
    public double getReferenceLengthMm() { return referenceLengthMm; }
    public Point getReferenceTip1() { return new Point(referenceTip1.x, referenceTip1.y); }
    public Point getReferenceTip2() { return new Point(referenceTip2.x, referenceTip2.y); }
    public Mat getTip1Patch() { return tip1Patch; }
    public Mat getTip2Patch() { return tip2Patch; }
    public int getTipPatchSize() { return tipPatchSize; }
    public Mat getTemplateImage() { return templateImage.clone(); }
    public Mat getGrayImage() { return grayImage.clone(); }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
