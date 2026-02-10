package com.example;

import com.edge.vision.platform.OpenCVInitializer;
import com.edge.vision.core.NeedleLengthAnalyzer;
import com.edge.vision.core.MeasurementResult;
import com.edge.vision.template.TemplateBuilder;

/**
 * 示例：使用 Needle Measure SDK 进行针长度测量
 * 
 * 运行方式:
 * 1. cd example-project
 * 2. ../gradlew run
 * 
 * 或打包后运行:
 * 1. ../gradlew fatJar
 * 2. java -jar build/libs/example-project-1.0.0-all.jar
 */
public class ExampleApp {
    
    public static void main(String[] args) {
        System.out.println("========== Needle Measure SDK 使用示例 ==========");
        
        // 1. 初始化 OpenCV
        System.out.println("\n1. 初始化 OpenCV...");
        try {
            OpenCVInitializer.initialize();
            System.out.println("✓ OpenCV 初始化成功！");
        } catch (Exception e) {
            System.err.println("✗ OpenCV 初始化失败: " + e.getMessage());
            System.err.println("请确保 OpenCV 库已正确配置");
            return;
        }
        
        // 2. 创建模板（这里只是演示，实际使用时需要提供真实图像）
        System.out.println("\n2. 创建测量模板...");
        System.out.println("   模板创建代码示例：");
        System.out.println("   ----------------------------------------");
        System.out.println("   TemplateBuilder builder = new TemplateBuilder()");
        System.out.println("       .loadImage(\"template.jpg\")");
        System.out.println("       .setReferenceLength(50.0)   // 实际长度 50mm");
        System.out.println("       .setTip1(100, 200)          // 针尖1坐标");
        System.out.println("       .setTip2(500, 200)          // 针尖2坐标");
        System.out.println("       .setTemplateId(\"needle_50mm\");");
        System.out.println("   String path = builder.buildAndSave(\"output/template\");");
        System.out.println("   builder.release();");
        System.out.println("   ----------------------------------------");
        
        // 3. 执行测量（这里只是演示代码结构）
        System.out.println("\n3. 执行测量...");
        System.out.println("   测量代码示例：");
        System.out.println("   ----------------------------------------");
        System.out.println("   try (NeedleLengthAnalyzer analyzer = ");
        System.out.println("           new NeedleLengthAnalyzer(\"template.png\")) {");
        System.out.println("       ");
        System.out.println("       MeasurementResult result = analyzer.analyze(\"target.jpg\");");
        System.out.println("       ");
        System.out.println("       System.out.println(\"长度: \" + result.getLengthMm() + \" mm\");");
        System.out.println("       System.out.println(\"置信度: \" + result.getConfidence());");
        System.out.println("       System.out.println(\"耗时: \" + result.getProcessingTimeMs() + \" ms\");");
        System.out.println("       ");
        System.out.println("       // JSON 输出");
        System.out.println("       System.out.println(result.toJsonString());");
        System.out.println("   }");
        System.out.println("   ----------------------------------------");
        
        // 4. 显示 SDK 信息
        System.out.println("\n4. SDK 信息：");
        System.out.println("   - SDK 版本: 1.0.0");
        System.out.println("   - 平台: Desktop");
        System.out.println("   - OpenCV 状态: " + (OpenCVInitializer.isInitialized() ? "已初始化" : "未初始化"));
        
        System.out.println("\n========== 示例结束 ==========");
        System.out.println("\n提示：");
        System.out.println("- 请将真实的模板图像和待测图像放在项目目录中");
        System.out.println("- 修改代码中的路径以指向你的实际图像文件");
        System.out.println("- 详细的 API 文档请参考 SDK 的 README.md");
    }
}
