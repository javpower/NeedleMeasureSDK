package com.edge.vision.example;

import com.edge.vision.core.AnalysisTemplate;
import com.edge.vision.core.MeasurementResult;
import com.edge.vision.core.NeedleLengthAnalyzer;
import com.edge.vision.platform.OpenCVInitializer;
import com.edge.vision.template.TemplateBuilder;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.imgcodecs.Imgcodecs;

/**
 * 桌面平台使用示例
 * 
 * 运行前请确保：
 * 1. 添加OpenCV依赖（org.openpnp:opencv 或 nu.pattern:opencv）
 * 2. 准备测试图像
 * @author Coder建设
 */
public class DesktopExample {
    
    public static void main(String[] args) {
        // 初始化OpenCV
        try {
            OpenCVInitializer.initialize();
            System.out.println("OpenCV初始化成功");
        } catch (Exception e) {
            System.err.println("OpenCV初始化失败: " + e.getMessage());
            // 尝试使用nu.pattern
            try {
                nu.pattern.OpenCV.loadShared();
                System.out.println("OpenCV通过nu.pattern加载成功");
            } catch (Exception e2) {
                System.err.println("所有加载方式都失败");
                return;
            }
        }
        
        // 示例1: 创建模板
        // createTemplateExample();
        
        // 示例2: 使用已有模板进行测量
        measureExample();
        
        // 示例3: 程序化创建模板并测量
        // programmaticTemplateExample();
    }
    
    /**
     * 示例1: 创建模板
     */
    static void createTemplateExample() {
        System.out.println("\n=== 创建模板示例 ===");
        
        // 加载模板图像
        String imagePath = "path/to/your/template_image.jpg";
        Mat image = Imgcodecs.imread(imagePath);
        
        if (image.empty()) {
            System.err.println("无法加载图像: " + imagePath);
            return;
        }
        
        // 使用TemplateBuilder创建模板
        // 假设针的两个端点坐标为 (100, 200) 和 (500, 200)，实际长度为50mm
        TemplateBuilder builder = new TemplateBuilder()
            .setImage(image)
            .setReferenceLength(50.0)  // 实际长度50mm
            .setTip1(100, 200)         // 针尖1坐标
            .setTip2(500, 200)         // 针尖2坐标
            .setTemplateId("needle_50mm_template")
            .setMargin(30);            // 裁剪边距
        
        try {
            // 构建并保存模板
            String templatePath = "output/needle_template";
            String metaPath = builder.buildAndSave(templatePath);
            
            System.out.println("模板创建成功!");
            System.out.println("模板文件: " + templatePath + ".png");
            System.out.println("元数据文件: " + metaPath);
            
        } finally {
            builder.release();
            image.release();
        }
    }
    
    /**
     * 示例2: 使用已有模板进行测量
     */
    static void measureExample() {
        System.out.println("\n=== 测量示例 ===");
        
        // 模板文件路径（.png文件，同目录下需要有.meta文件）
        String templatePath = "/Users/xiongguochao/Desktop/needle_template_50mm.png";
        
        // 目标图像路径
        String targetImagePath = "/Users/xiongguochao/Desktop/针测试.jpeg";
        
        try (NeedleLengthAnalyzer analyzer = new NeedleLengthAnalyzer(templatePath)) {
            
            // 执行测量
            MeasurementResult result = analyzer.analyze(targetImagePath);
            
            // 输出结果
            System.out.println("测量完成!");
            System.out.println(result.toFormattedString());
            
            // 获取JSON格式结果
            System.out.println("\nJSON格式:");
            System.out.println(result.toJsonString());
            
            // 获取具体数值
            System.out.println("\n具体数值:");
            System.out.println("长度: " + result.getLengthMm() + " mm");
            System.out.println("像素长度: " + result.getPixelLength() + " px");
            System.out.println("置信度: " + (result.getConfidence() * 100) + "%");
            System.out.println("处理时间: " + result.getProcessingTimeMs() + " ms");
            System.out.println("针尖1: (" + result.getTip1().x + ", " + result.getTip1().y + ")");
            System.out.println("针尖2: (" + result.getTip2().x + ", " + result.getTip2().y + ")");
            
        } catch (Exception e) {
            System.err.println("测量失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 示例3: 程序化创建模板并立即使用
     */
    static void programmaticTemplateExample() {
        System.out.println("\n=== 程序化模板示例 ===");
        
        // 加载图像
        String imagePath = "path/to/your/template_image.jpg";
        Mat image = Imgcodecs.imread(imagePath);
        
        if (image.empty()) {
            System.err.println("无法加载图像: " + imagePath);
            return;
        }
        
        // 创建模板
        TemplateBuilder builder = new TemplateBuilder()
            .setImage(image)
            .setReferenceLength(50.0)
            .setTip1(100, 200)
            .setTip2(500, 200);
        
        AnalysisTemplate template = builder.build();
        
        try {
            // 直接使用内存中的模板创建分析器
            try (NeedleLengthAnalyzer analyzer = new NeedleLengthAnalyzer(template)) {
                
                // 测量
                String targetPath = "path/to/target_image.jpg";
                MeasurementResult result = analyzer.analyze(targetPath);
                
                System.out.println("测量结果: " + result.toFormattedString());
            }
            
        } finally {
            template.close();
            builder.release();
        }
    }
    
    /**
     * 示例4: 批量测量
     */
    static void batchMeasureExample() {
        System.out.println("\n=== 批量测量示例 ===");
        
        String templatePath = "path/to/template.png";
        String[] targetImages = {
            "path/to/image1.jpg",
            "path/to/image2.jpg",
            "path/to/image3.jpg"
        };
        
        try (NeedleLengthAnalyzer analyzer = new NeedleLengthAnalyzer(templatePath)) {
            
            for (String targetPath : targetImages) {
                try {
                    MeasurementResult result = analyzer.analyze(targetPath);
                    System.out.println(targetPath + ": " + result.getLengthMm() + " mm");
                } catch (Exception e) {
                    System.err.println(targetPath + ": 测量失败 - " + e.getMessage());
                }
            }
            
        } catch (Exception e) {
            System.err.println("批量测量失败: " + e.getMessage());
        }
    }
    
    /**
     * 示例5: 从内存中的图像进行测量
     */
    static void measureFromMatExample() {
        System.out.println("\n=== 从Mat测量示例 ===");
        
        String templatePath = "path/to/template.png";
        
        // 从某处获取Mat（例如从相机、网络等）
        Mat targetImage = Imgcodecs.imread("path/to/target.jpg");
        
        try (NeedleLengthAnalyzer analyzer = new NeedleLengthAnalyzer(templatePath)) {
            
            // 直接从Mat测量
            MeasurementResult result = analyzer.analyze(targetImage);
            System.out.println("测量结果: " + result.toFormattedString());
            
            // 生成可视化结果
            Mat visualization = analyzer.generateVisualization(targetImage, result);
            Imgcodecs.imwrite("output/result_visualization.png", visualization);
            visualization.release();
            
        } catch (Exception e) {
            System.err.println("测量失败: " + e.getMessage());
        } finally {
            targetImage.release();
        }
    }
}
