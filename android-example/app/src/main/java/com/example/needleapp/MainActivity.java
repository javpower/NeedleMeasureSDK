package com.example.needleapp;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.edge.vision.core.MeasurementResult;
import com.edge.vision.core.NeedleLengthAnalyzer;
import com.edge.vision.platform.OpenCVInitializer;

import org.bytedeco.javacv.AndroidFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.opencv_core.Mat;

import java.io.IOException;
import java.io.InputStream;

/**
 * Android 示例 - 使用 JavaCV
 *
 * JavaCV 自动加载原生库，无需手动初始化
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private NeedleLengthAnalyzer analyzer;
    private AndroidFrameConverter converter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 初始化 OpenCV (JavaCV 自动加载，这里只是确认)
        try {
            OpenCVInitializer.initialize(this);
            Log.i(TAG, "JavaCV OpenCV 初始化成功");
        } catch (Exception e) {
            Log.e(TAG, "OpenCV 初始化失败: " + e.getMessage(), e);
        }

        converter = new AndroidFrameConverter();

        // 加载模板
        loadTemplate();

        Log.i(TAG, "MainActivity 创建完成");
    }

    /**
     * 从 assets 加载模板
     */
    private void loadTemplate() {
        new Thread(() -> {
            try {
                InputStream imageStream = getAssets().open("templates/needle_template_50mm.png");
                InputStream metaStream = getAssets().open("templates/needle_template_50mm.meta");

                analyzer = new NeedleLengthAnalyzer(imageStream, metaStream);

                Log.i(TAG, "模板加载成功: " + analyzer.getTemplate().getTemplateId());

                runOnUiThread(() -> {
                    Toast.makeText(this, "模板加载成功", Toast.LENGTH_SHORT).show();
                });

            } catch (IOException e) {
                Log.e(TAG, "模板加载失败: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "模板加载失败", Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    /**
     * 执行测量（使用 JavaCV）
     */
    private void performMeasurement(Bitmap bitmap) {
        if (analyzer == null) {
            Toast.makeText(this, "模板未加载", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try {
                // Bitmap -> Mat (JavaCV)
                Mat mat = new OpenCVFrameConverter.ToMat().convert(converter.convert(bitmap));

                // 执行测量
                MeasurementResult result = analyzer.analyze(mat);

                // 释放 Mat 资源
                mat.close();

                // 更新 UI
                runOnUiThread(() -> onMeasurementResult(result));

            } catch (Exception e) {
                Log.e(TAG, "测量失败: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "测量失败", Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void onMeasurementResult(MeasurementResult result) {
        String message = String.format(
            "长度: %.2f mm\n置信度: %.1f%%",
            result.getLengthMm(),
            result.getConfidence() * 100
        );

        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        Log.i(TAG, "JSON: " + result.toJsonString());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (analyzer != null) {
            analyzer.close();
            analyzer = null;
        }
    }
}
