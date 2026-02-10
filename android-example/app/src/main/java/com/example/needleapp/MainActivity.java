package com.example.needleapp;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.edge.vision.core.MeasurementResult;
import com.edge.vision.core.NeedleLengthAnalyzer;
import com.edge.vision.platform.OpenCVInitializer;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.IOException;
import java.io.InputStream;

/**
 * 主界面 Activity
 * 
 * 演示如何在 Android 中使用 Needle Measure SDK 进行测量
 * @author Coder建设
 */
public class MainActivity extends AppCompatActivity {
    
    private static final String TAG = "MainActivity";
    
    private NeedleLengthAnalyzer analyzer;
    private TextView tvResult;
    private ImageView ivImage;
    private Button btnMeasure;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 注意：实际项目中需要创建对应的布局文件 activity_main.xml
        // setContentView(R.layout.activity_main);
        
        // 检查 OpenCV 是否已初始化
        if (!OpenCVInitializer.isInitialized()) {
            Toast.makeText(this, "OpenCV 未初始化", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        
        // 初始化视图
        initViews();
        
        // 加载模板
        loadTemplate();
        
        Log.i(TAG, "MainActivity 创建完成");
    }
    
    private void initViews() {
        // 注意：这里假设布局中有这些视图，实际项目中需要根据真实布局修改
        // tvResult = findViewById(R.id.tv_result);
        // ivImage = findViewById(R.id.iv_image);
        // btnMeasure = findViewById(R.id.btn_measure);
        
        // btnMeasure.setOnClickListener(v -> onMeasureClick());
    }
    
    /**
     * 从 assets 加载模板
     */
    private void loadTemplate() {
        new Thread(() -> {
            try {
                // 从 assets 加载模板文件
                // 注意：需要先将模板文件放在 app/src/main/assets/templates/ 目录下
                InputStream imageStream = getAssets().open("templates/needle_template.png");
                InputStream metaStream = getAssets().open("templates/needle_template.meta");
                
                analyzer = new NeedleLengthAnalyzer(imageStream, metaStream);
                
                Log.i(TAG, "模板加载成功: " + analyzer.getTemplate().getTemplateId());
                
                runOnUiThread(() -> {
                    Toast.makeText(this, "模板加载成功", Toast.LENGTH_SHORT).show();
                });
                
            } catch (IOException e) {
                Log.e(TAG, "模板加载失败: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "模板加载失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }
    
    /**
     * 测量按钮点击事件
     */
    private void onMeasureClick() {
        if (analyzer == null) {
            Toast.makeText(this, "模板未加载", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 获取 Bitmap（这里假设从相机或相册获取）
        Bitmap bitmap = getBitmapFromSource();
        if (bitmap == null) {
            Toast.makeText(this, "请先选择图片", Toast.LENGTH_SHORT).show();
            return;
        }
        
        performMeasurement(bitmap);
    }
    
    /**
     * 执行测量
     */
    private void performMeasurement(Bitmap bitmap) {
        // 在后台线程执行测量
        new Thread(() -> {
            Mat mat = new Mat();
            
            try {
                // Bitmap -> Mat
                Utils.bitmapToMat(bitmap, mat);
                
                // 执行测量
                MeasurementResult result = analyzer.analyze(mat);
                
                // 更新 UI
                runOnUiThread(() -> onMeasurementResult(result));
                
            } catch (Exception e) {
                Log.e(TAG, "测量失败: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "测量失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            } finally {
                // 释放资源
                mat.release();
            }
        }).start();
    }
    
    /**
     * 从相机预览帧测量（NV21 格式）
     */
    public void measureFromCameraFrame(byte[] data, int width, int height) {
        if (analyzer == null) {
            Log.e(TAG, "分析器未初始化");
            return;
        }
        
        new Thread(() -> {
            // NV21 -> Mat
            Mat yuv = new Mat(height + height / 2, width, org.opencv.core.CvType.CV_8UC1);
            yuv.put(0, 0, data);
            
            Mat bgr = new Mat();
            org.opencv.imgproc.Imgproc.cvtColor(yuv, bgr, org.opencv.imgproc.Imgproc.COLOR_YUV2BGR_NV21);
            
            try {
                MeasurementResult result = analyzer.analyze(bgr);
                runOnUiThread(() -> onMeasurementResult(result));
            } catch (Exception e) {
                Log.e(TAG, "测量失败: " + e.getMessage(), e);
            } finally {
                yuv.release();
                bgr.release();
            }
        }).start();
    }
    
    /**
     * 测量结果回调
     */
    private void onMeasurementResult(MeasurementResult result) {
        Log.i(TAG, "测量完成");
        Log.i(TAG, "长度: " + result.getLengthMm() + " mm");
        Log.i(TAG, "置信度: " + (result.getConfidence() * 100) + "%");
        Log.i(TAG, "耗时: " + result.getProcessingTimeMs() + " ms");
        
        // 显示结果
        String message = String.format(
            "长度: %.4f mm\n置信度: %.2f%%\n耗时: %d ms",
            result.getLengthMm(),
            result.getConfidence() * 100,
            result.getProcessingTimeMs()
        );
        
        // tvResult.setText(message);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        
        // JSON 格式
        String json = result.toJsonString();
        Log.i(TAG, "JSON: " + json);
    }
    
    /**
     * 生成带标注的结果图像
     */
    public Bitmap generateAnnotatedBitmap(Bitmap originalBitmap, MeasurementResult result) {
        Mat mat = new Mat();
        Utils.bitmapToMat(originalBitmap, mat);
        
        // 生成可视化结果
        Mat annotated = analyzer.generateVisualization(mat, result);
        
        // Mat -> Bitmap
        Bitmap resultBitmap = Bitmap.createBitmap(
            annotated.cols(), annotated.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(annotated, resultBitmap);
        
        // 释放资源
        mat.release();
        annotated.release();
        
        return resultBitmap;
    }
    
    /**
     * 获取 Bitmap（示例，实际项目中从相机或相册获取）
     */
    private Bitmap getBitmapFromSource() {
        // 实际项目中：
        // 1. 从相机获取预览帧
        // 2. 从相册选择图片
        // 3. 拍照获取图片
        
        // 这里返回 null 作为示例
        return null;
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // 释放分析器资源
        if (analyzer != null) {
            analyzer.close();
            analyzer = null;
        }
        
        Log.i(TAG, "MainActivity 销毁，资源已释放");
    }
}
