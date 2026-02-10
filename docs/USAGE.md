# Needle Measure SDK 使用指南

## 快速开始

### 1. 添加依赖

#### 桌面平台

**Gradle:**
```gradle
dependencies {
    implementation files('libs/needle-measure-sdk-1.0.0-desktop.jar')
    implementation 'org.openpnp:opencv:4.7.0-0'
}
```

**Maven:**
```xml
<dependency>
    <groupId>org.openpnp</groupId>
    <artifactId>opencv</artifactId>
    <version>4.7.0-0</version>
</dependency>
```

#### Android 平台

将 `needle-measure-sdk-1.0.0-android.jar` 复制到 `app/libs/` 目录，并在 `build.gradle` 中添加：

```gradle
dependencies {
    implementation fileTree(dir: 'libs', include: ['*.jar'])
    implementation project(':opencv')  // OpenCV Android SDK
}
```

### 2. 初始化 SDK

```java
import com.edge.vision.core.NeedleLengthAnalyzer;
import com.edge.vision.core.MeasurementResult;

// 初始化（桌面平台）
NeedleLengthAnalyzer analyzer = new NeedleLengthAnalyzer();

// 或者带配置初始化
NeedleAnalyzerConfig config = new NeedleAnalyzerConfig.Builder()
    .setMinNeedleLength(20)
    .setMaxNeedleLength(200)
    .build();
NeedleLengthAnalyzer analyzer = new NeedleLengthAnalyzer(config);
```

### 3. 测量针长

```java
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

// 加载图像
Mat image = Imgcodecs.imread("needle.jpg");

// 执行测量
MeasurementResult result = analyzer.measure(image);

// 获取结果
System.out.println("针长: " + result.getLength() + " mm");
System.out.println("置信度: " + result.getConfidence());
```

## API 文档

详见 `needle-measure-sdk-X.X.X-javadoc.jar` 中的文档。

## 示例项目

- `example-project/` - 桌面平台示例
- `android-example/` - Android 平台示例
