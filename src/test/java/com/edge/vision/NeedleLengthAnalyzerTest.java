package com.edge.vision;

import com.edge.vision.core.AnalysisTemplate;
import com.edge.vision.core.MeasurementResult;
import com.edge.vision.core.NeedleLengthAnalyzer;
import com.edge.vision.template.TemplateBuilder;
import org.junit.jupiter.api.*;

import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Point;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.bytedeco.opencv.opencv_core.Size;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgproc;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * NeedleLengthAnalyzer 单元测试
 *
 * 使用 JavaCV，原生库自动加载
 *
* @author Coder建设
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class NeedleLengthAnalyzerTest {

    private Path tempDir;
    private String templatePath;

    @BeforeAll
    void setUp() throws Exception {
        // JavaCV 自动加载原生库，无需手动初始化

        // 创建临时目录
        tempDir = Files.createTempDirectory("needle_test");
        templatePath = tempDir.toString() + "/test_template";

        // 创建测试模板
        createTestTemplate();
    }

    @AfterAll
    void tearDown() throws Exception {
        // 清理临时文件
        if (tempDir != null) {
            Files.walk(tempDir)
                .sorted((a, b) -> -a.compareTo(b))
                .forEach(p -> {
                    try {
                        Files.delete(p);
                    } catch (Exception e) {
                        // ignore
                    }
                });
        }
    }

    /**
     * 创建测试用的模板
     */
    private void createTestTemplate() {
        // 创建一个简单的测试图像（600x400，黑色背景，白色水平线）
        Mat image = Mat.zeros(400, 600, opencv_core.CV_8UC3).asMat();

        // 画一条水平线（模拟针）
        Point tip1 = new Point(100, 200);
        Point tip2 = new Point(500, 200);
        opencv_imgproc.line(image, tip1, tip2, new Scalar(255, 255, 255, 0), 3, 0, 0);

        // 画针尖标记
        opencv_imgproc.circle(image, tip1, 5, new Scalar(0, 0, 255, 0), -1, 0, 0);
        opencv_imgproc.circle(image, tip2, 5, new Scalar(0, 0, 255, 0), -1, 0, 0);

        // 创建模板
        TemplateBuilder builder = new TemplateBuilder()
            .setImage(image)
            .setReferenceLength(50.0)  // 50mm
            .setTip1(100, 200)
            .setTip2(500, 200)
            .setTemplateId("test_template");

        builder.buildAndSave(templatePath);
        builder.release();
        image.close();
    }

    @Test
    @DisplayName("测试模板加载")
    void testTemplateLoading() {
        AnalysisTemplate template = new AnalysisTemplate(templatePath + ".png");

        assertNotNull(template);
        assertEquals("test_template", template.getTemplateId());
        assertEquals(50.0, template.getReferenceLengthMm(), 0.001);
        assertTrue(template.getMmPerPixel() > 0);

        template.close();
    }

    @Test
    @DisplayName("测试测量功能")
    void testMeasurement() {
        // 创建目标图像（与模板相同）
        Mat target = Mat.zeros(400, 600, opencv_core.CV_8UC3).asMat();
        Point tip1 = new Point(100, 200);
        Point tip2 = new Point(500, 200);
        opencv_imgproc.line(target, tip1, tip2, new Scalar(255, 255, 255, 0), 3, 0, 0);

        try (NeedleLengthAnalyzer analyzer = new NeedleLengthAnalyzer(templatePath + ".png")) {
            MeasurementResult result = analyzer.analyze(target);

            assertNotNull(result);
            assertEquals(50.0, result.getLengthMm(), 2.0);  // 允许2mm误差
            assertEquals(400.0, result.getPixelLength(), 15.0);  // 400像素，允许15像素误差
            assertTrue(result.getConfidence() > 0.5);
            assertTrue(result.getProcessingTimeMs() > 0);

        } finally {
            target.close();
        }
    }

    @Test
    @DisplayName("测试多尺度匹配")
    void testMultiScaleMatching() {
        // 创建缩放后的目标图像（0.8倍）
        Mat original = Mat.zeros(400, 600, opencv_core.CV_8UC3).asMat();
        opencv_imgproc.line(original, new Point(100, 200), new Point(500, 200),
            new Scalar(255, 255, 255, 0), 3, 0, 0);

        Mat scaled = new Mat();
        opencv_imgproc.resize(original, scaled, new Size(480, 320));  // 0.8倍

        try (NeedleLengthAnalyzer analyzer = new NeedleLengthAnalyzer(templatePath + ".png")) {
            MeasurementResult result = analyzer.analyze(scaled);

            assertNotNull(result);
            // 缩放后长度应该接近40mm（50 * 0.8）
            assertEquals(40.0, result.getLengthMm(), 5.0);

        } finally {
            original.close();
            scaled.close();
        }
    }

    @Test
    @DisplayName("测试TemplateBuilder")
    void testTemplateBuilder() {
        Mat image = Mat.zeros(300, 500, opencv_core.CV_8UC3).asMat();

        TemplateBuilder builder = new TemplateBuilder()
            .setImage(image)
            .setReferenceLength(30.0)
            .setTip1(50, 150)
            .setTip2(450, 150)
            .setTemplateId("builder_test");

        AnalysisTemplate template = builder.build();

        assertNotNull(template);
        assertEquals("builder_test", template.getTemplateId());
        assertEquals(30.0, template.getReferenceLengthMm(), 0.001);

        template.close();
        builder.release();
    }

    @Test
    @DisplayName("测试无效模板路径")
    void testInvalidTemplatePath() {
        assertThrows(RuntimeException.class, () -> {
            new NeedleLengthAnalyzer("/invalid/path/template.png");
        });
    }

    @Test
    @DisplayName("测试无效目标图像")
    void testInvalidTargetImage() {
        try (NeedleLengthAnalyzer analyzer = new NeedleLengthAnalyzer(templatePath + ".png")) {
            assertThrows(RuntimeException.class, () -> {
                analyzer.analyze("/invalid/path/image.jpg");
            });
        }
    }

    @Test
    @DisplayName("测试结果JSON格式")
    void testResultJsonFormat() {
        Mat target = Mat.zeros(400, 600, opencv_core.CV_8UC3).asMat();
        opencv_imgproc.line(target, new Point(100, 200), new Point(500, 200),
            new Scalar(255, 255, 255, 0), 3, 0, 0);

        try (NeedleLengthAnalyzer analyzer = new NeedleLengthAnalyzer(templatePath + ".png")) {
            MeasurementResult result = analyzer.analyze(target);
            String json = result.toJsonString();

            assertNotNull(json);
            assertTrue(json.contains("lengthMm"));
            assertTrue(json.contains("pixelLength"));
            assertTrue(json.contains("confidence"));
            assertTrue(json.contains("templateId"));

        } finally {
            target.close();
        }
    }
}
