# Needle Measure SDK 桌面端示例

这个项目演示了如何在桌面应用中使用 Needle Measure SDK（基于 JavaCV）。

## 环境要求

- **JDK 11+**
- **Gradle 7.5+**

## 项目结构

```
example-project/
├── build.gradle              # Gradle 构建配置
├── settings.gradle           # Gradle 设置
├── libs/                     # SDK jar 文件（需先构建）
└── src/main/java/com/example/
    └── ExampleApp.java       # 示例代码
```

## 快速开始

### 1. 构建 SDK jar

```bash
cd /Volumes/macEx/AI/needle-measure-sdk
./gradlew desktopJar

# 复制到示例项目
cp build/libs/needle-measure-sdk-1.0.0-desktop.jar example-project/libs/
```

### 2. 配置 build.gradle

```groovy
dependencies {
    // Needle Measure SDK
    implementation files('libs/needle-measure-sdk-1.0.0-desktop.jar')

    // JavaCV OpenCV（选择你的平台）
    implementation 'org.bytedeco:javacv:1.5.9'
    implementation 'org.bytedeco:opencv:4.7.0-1.5.9:macosx-arm64'  // macOS Apple Silicon
    // implementation 'org.bytedeco:opencv:4.7.0-1.5.9:macosx-x86_64'  // macOS Intel
    // implementation 'org.bytedeco:opencv:4.7.0-1.5.9:windows-x86_64'  // Windows
    // implementation 'org.bytedeco:opencv:4.7.0-1.5.9:linux-x86_64'    // Linux
}
```

### 3. 运行示例

```bash
cd example-project
../gradlew run
```

## 平台选择

根据你的操作系统选择对应的 JavaCV 依赖：

| 平台 | Classifier |
|------|------------|
| macOS Apple Silicon | `macosx-arm64` |
| macOS Intel | `macosx-x86_64` |
| Windows x64 | `windows-x86_64` |
| Linux x64 | `linux-x86_64` |

## 完整使用示例

```java
import com.edge.vision.platform.OpenCVInitializer;
import com.edge.vision.core.NeedleLengthAnalyzer;
import com.edge.vision.core.MeasurementResult;
import com.edge.vision.template.TemplateBuilder;

import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.global.opencv_imgcodecs;

public class ExampleApp {
    public static void main(String[] args) {
        // 1. 初始化 OpenCV（JavaCV 自动加载）
        OpenCVInitializer.initialize();
        System.out.println("JavaCV OpenCV 初始化成功");

        // 2. 使用模板进行测量
        measureExample();

        // 3. 创建新模板示例
        // createTemplateExample();
    }

    static void measureExample() {
        String templatePath = "path/to/template.png";
        String targetPath = "path/to/image.jpg";

        try (NeedleLengthAnalyzer analyzer = new NeedleLengthAnalyzer(templatePath)) {
            MeasurementResult result = analyzer.analyze(targetPath);

            System.out.println("测量完成!");
            System.out.println("长度: " + result.getLengthMm() + " mm");
            System.out.println("置信度: " + (result.getConfidence() * 100) + "%");
            System.out.println("JSON: " + result.toJsonString());

        } catch (Exception e) {
            System.err.println("测量失败: " + e.getMessage());
        }
    }

    static void createTemplateExample() {
        Mat image = opencv_imgcodecs.imread("template_image.jpg");

        TemplateBuilder builder = new TemplateBuilder()
            .setImage(image)
            .setReferenceLength(50.0)  // 实际长度 50mm
            .setTip1(100, 200)         // 针尖1坐标
            .setTip2(500, 200)         // 针尖2坐标
            .setTemplateId("needle_50mm");

        builder.buildAndSave("output/needle_template");
        builder.release();
        image.close();

        System.out.println("模板创建成功!");
    }
}
```

## 在其他项目中使用

### Gradle 项目

```groovy
dependencies {
    implementation files('/path/to/needle-measure-sdk-1.0.0-desktop.jar')
    implementation 'org.bytedeco:javacv:1.5.9'
    implementation 'org.bytedeco:opencv:4.7.0-1.5.9:macosx-arm64'  // 你的平台
}
```

### Maven 项目

```xml
<dependencies>
    <dependency>
        <groupId>com.edge.vision</groupId>
        <artifactId>needle-measure-sdk</artifactId>
        <version>1.0.0</version>
        <scope>system</scope>
        <systemPath>/path/to/needle-measure-sdk-1.0.0-desktop.jar</systemPath>
    </dependency>

    <dependency>
        <groupId>org.bytedeco</groupId>
        <artifactId>javacv</artifactId>
        <version>1.5.9</version>
    </dependency>

    <dependency>
        <groupId>org.bytedeco</groupId>
        <artifactId>opencv</artifactId>
        <version>4.7.0-1.5.9</version>
        <classifier>macosx-arm64</classifier>  <!-- 你的平台 -->
    </dependency>
</dependencies>
```

### 使用 Maven Profiles 选择平台

在项目的 pom.xml 中添加 profile：

```xml
<profiles>
    <profile>
        <id>macos-arm</id>
        <activation>
            <os>
                <family>mac</family>
                <arch>aarch64</arch>
            </os>
        </activation>
        <dependencies>
            <dependency>
                <groupId>org.bytedeco</groupId>
                <artifactId>opencv</artifactId>
                <version>4.7.0-1.5.9</version>
                <classifier>macosx-arm64</classifier>
            </dependency>
        </dependencies>
    </profile>

    <profile>
        <id>windows</id>
        <activation>
            <os>
                <family>windows</family>
            </os>
        </activation>
        <dependencies>
            <dependency>
                <groupId>org.bytedeco</groupId>
                <artifactId>opencv</artifactId>
                <version>4.7.0-1.5.9</version>
                <classifier>windows-x86_64</classifier>
            </dependency>
        </dependencies>
    </profile>
</profiles>
```

## 注意事项

1. **JavaCV 自动加载**：无需手动加载原生库，JavaCV 会自动处理
2. **平台选择**：确保选择正确的平台 classifier
3. **资源释放**：使用 `try-with-resources` 或手动调用 `close()` 释放资源

## 更多文档

- [SDK 主 README](../README.md)
- [Android 示例](../android-example/README.md)
- [JavaCV 官方文档](https://github.com/bytedeco/javacv)
