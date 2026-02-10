# Android 项目使用 Needle Measure SDK 示例

这个示例展示了如何在 Android 项目中使用 Needle Measure SDK（基于 JavaCV）。

## 环境要求

- **JDK 11+**
- **Android SDK 34**
- **Gradle 8.2+**（已包含 wrapper）

## 特点

- 使用 **JavaCV**，无需下载 OpenCV Android SDK
- Maven/Gradle 自动下载依赖
- 支持所有 Android ABI（arm, arm64, x86, x86_64）

## 项目结构

```
android-example/
├── app/
│   ├── build.gradle                    # App 模块构建配置
│   ├── libs/                           # SDK jar 放在这里
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/example/needleapp/
│       │   └── MainActivity.java       # 主界面
│       └── assets/templates/           # 模板文件
└── build.gradle
```

## 快速开始

### 1. 构建 SDK jar

```bash
cd /Volumes/macEx/AI/needle-measure-sdk
./gradlew androidJar

# 复制到示例项目
cp build/libs/needle-measure-sdk-1.0.0-android.jar android-example/app/libs/
```

### 2. 配置 build.gradle

**App 级 build.gradle:**

```groovy
android {
    compileSdk 34

    defaultConfig {
        minSdk 21
        targetSdk 34

        ndk {
            // 按需选择架构，减少包体积
            abiFilters 'arm64-v8a'  // 最常用
            // abiFilters 'armeabi-v7a', 'arm64-v8a', 'x86', 'x86_64'  // 全部
        }
    }
}

dependencies {
    // Needle Measure SDK
    implementation files('libs/needle-measure-sdk-1.0.0-android.jar')

    // JavaCV OpenCV（按需选择架构）
    implementation 'org.bytedeco:javacv:1.5.9'
    implementation 'org.bytedeco:opencv:4.7.0-1.5.9:android-arm64'
    // implementation 'org.bytedeco:opencv:4.7.0-1.5.9:android-arm'
    // implementation 'org.bytedeco:opencv:4.7.0-1.5.9:android-x86'
    // implementation 'org.bytedeco:opencv:4.7.0-1.5.9:android-x86_64'

    // AndroidX
    implementation 'androidx.appcompat:appcompat:1.6.1'
}
```

### 3. 准备模板文件

将模板文件放到 `app/src/main/assets/templates/` 目录：

```
app/src/main/assets/templates/
├── needle_template_50mm.png
└── needle_template_50mm.meta
```

### 4. 在 Activity 中使用

```java
import com.edge.vision.core.NeedleLengthAnalyzer;
import com.edge.vision.core.MeasurementResult;
import com.edge.vision.platform.OpenCVInitializer;

import org.bytedeco.javacv.AndroidFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.opencv_core.Mat;

public class MainActivity extends AppCompatActivity {

    private NeedleLengthAnalyzer analyzer;
    private AndroidFrameConverter converter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 初始化 OpenCV（JavaCV 自动加载）
        OpenCVInitializer.initialize(this);

        converter = new AndroidFrameConverter();
        loadTemplate();
    }

    private void loadTemplate() {
        new Thread(() -> {
            try {
                InputStream imageStream = getAssets().open("templates/needle_template_50mm.png");
                InputStream metaStream = getAssets().open("templates/needle_template_50mm.meta");

                analyzer = new NeedleLengthAnalyzer(imageStream, metaStream);

                runOnUiThread(() ->
                    Toast.makeText(this, "模板加载成功", Toast.LENGTH_SHORT).show()
                );
            } catch (IOException e) {
                Log.e("MainActivity", "模板加载失败", e);
            }
        }).start();
    }

    private void performMeasurement(Bitmap bitmap) {
        if (analyzer == null) {
            Toast.makeText(this, "模板未加载", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try {
                // Bitmap -> Mat (JavaCV)
                Mat mat = new OpenCVFrameConverter.ToMat()
                    .convert(converter.convert(bitmap));

                // 执行测量
                MeasurementResult result = analyzer.analyze(mat);

                // 释放资源
                mat.close();

                // 更新 UI
                runOnUiThread(() -> {
                    String msg = String.format("长度: %.2f mm\n置信度: %.1f%%",
                        result.getLengthMm(),
                        result.getConfidence() * 100);
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                });

            } catch (Exception e) {
                Log.e("MainActivity", "测量失败", e);
            }
        }).start();
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
```

## 包体积优化

JavaCV 允许按需选择架构，大幅减小包体积：

| 配置 | 增加大小 |
|------|----------|
| 仅 arm64 | ~12 MB |
| arm + arm64 | ~24 MB |
| 全部架构 | ~48 MB |

推荐：大多数现代 Android 设备使用 arm64，只需添加 `android-arm64` 依赖。

## 常见问题

### Q: UnsatisfiedLinkError

确保：
1. `abiFilters` 配置正确
2. 添加了对应架构的 JavaCV 依赖

### Q: 如何创建模板？

模板需要在桌面端创建：

```java
TemplateBuilder builder = new TemplateBuilder()
    .loadImage("template_image.jpg")
    .setReferenceLength(50.0)  // 实际长度 mm
    .setTip1(100, 200)         // 针尖1坐标
    .setTip2(500, 200)         // 针尖2坐标
    .setTemplateId("needle_50mm");

builder.buildAndSave("output/needle_template");
```

然后将生成的 `.png` 和 `.meta` 文件复制到 Android 项目的 `assets/` 目录。

## 更多文档

- [SDK 主 README](../README.md)
- [JavaCV 官方文档](https://github.com/bytedeco/javacv)
