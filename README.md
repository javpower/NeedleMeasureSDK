# Needle Measure SDK

跨平台高精度针长度测量SDK，基于OpenCV实现。支持Windows、Mac、Linux、Android平台。

## 📦 开箱即用

**无需配置 OpenCV，下载即用！**

- **桌面平台**：下载 `desktop-complete.zip`，内含 Fat-jar（139MB），无需任何配置
- **Android 平台**：下载 `android-complete.zip`，内含示例项目和集成指南
- **自动构建**：基于 GitHub Actions，每次 push tag 自动构建并发布
- **自动测试**：使用真实模板和测试图片验证每个版本

👉 [前往 Releases 下载最新版本](../../releases)

## 特性

- **跨平台支持**: Windows、Mac、Linux、Android
- **高精度测量**: 基于模板匹配的亚像素精度测量
- **多尺度匹配**: 自动适应不同尺寸的图像
- **无GUI依赖**: 纯工具类，易于集成到各种应用
- **模板管理**: 支持模板创建、保存、加载
- **结果可视化**: 支持生成带标注的测量结果图

## 项目结构

```
needle-measure-sdk/
├── src/main/java/com/edge/vision/
│   ├── core/                    # 核心类
│   │   ├── MeasurementResult.java   # 测量结果
│   │   ├── AnalysisTemplate.java    # 分析模板
│   │   └── NeedleLengthAnalyzer.java # 测量分析器
│   ├── platform/                # 平台适配层
│   │   ├── OpenCVLoader.java        # OpenCV加载器接口
│   │   ├── PlatformDetector.java    # 平台检测
│   │   ├── DesktopOpenCVLoader.java # 桌面平台加载器
│   │   └── OpenCVInitializer.java   # 初始化管理器
│   ├── template/                # 模板工具
│   │   └── TemplateBuilder.java     # 模板构建器
│   └── utils/                   # 工具类
│       └── ImageUtils.java          # 图像工具
├── src/test/java/               # 测试代码（含PrecisionNeedleLengthAnalyzer可视化GUI直接测试）
├── example-project/             # 桌面示例项目
├── android-example/             # Android示例项目（含AndroidOpenCVLoader实现）
├── docs/                        # 文档
├── build.gradle
├── settings.gradle
└── README.md
```

## 快速开始

### 1. 添加依赖

**Gradle:**
```groovy
dependencies {
    // 桌面平台
    implementation 'com.edge.vision:needle-measure-sdk:1.0.0:desktop'
    
    // Android平台
    implementation 'com.edge.vision:needle-measure-sdk:1.0.0:android'
    
    // OpenCV依赖
    implementation 'org.openpnp:opencv:4.7.0-0'
}
```

**Maven:**
```xml
<dependency>
    <groupId>com.edge.vision</groupId>
    <artifactId>needle-measure-sdk</artifactId>
    <version>1.0.0</version>
    <classifier>desktop</classifier>
</dependency>
```

### 2. 初始化OpenCV

**桌面平台:**
```java
import com.edge.vision.platform.OpenCVInitializer;

// 自动检测平台并初始化
OpenCVInitializer.initialize();

// 或指定原生库路径
OpenCVInitializer.initialize("/path/to/opencv/libs");
```

**Android平台:**
```java
import com.edge.vision.platform.OpenCVInitializer;

// 在Activity中初始化
OpenCVInitializer.initialize(this);

// 或使用静态链接（推荐）
OpenCVInitializer.initialize(this, true);
```

### 3. 创建模板

```java
import com.edge.vision.template.TemplateBuilder;
import com.edge.vision.core.AnalysisTemplate;

// 方式1: 使用TemplateBuilder
TemplateBuilder builder = new TemplateBuilder()
    .loadImage("path/to/template_image.jpg")
    .setReferenceLength(50.0)      // 实际长度50mm
    .setTip1(100, 200)             // 针尖1坐标
    .setTip2(500, 200)             // 针尖2坐标
    .setTemplateId("needle_50mm");

String templatePath = builder.buildAndSave("output/template");
builder.release();
```

### 4. 执行测量

```java
import com.edge.vision.core.NeedleLengthAnalyzer;
import com.edge.vision.core.MeasurementResult;

// 创建分析器
try (NeedleLengthAnalyzer analyzer = new NeedleLengthAnalyzer("output/template.png")) {
    
    // 执行测量
    MeasurementResult result = analyzer.analyze("path/to/target_image.jpg");
    
    // 输出结果
    System.out.println("长度: " + result.getLengthMm() + " mm");
    System.out.println("置信度: " + (result.getConfidence() * 100) + "%");
    System.out.println("耗时: " + result.getProcessingTimeMs() + " ms");
    
    // JSON格式
    System.out.println(result.toJsonString());
}
```

## 平台特定说明

### Windows

- 支持32位和64位系统
- 需要 `opencv_java470.dll` 或 `opencv_java470_64.dll`
- 可通过nu.pattern依赖自动加载

### Mac

- 支持Intel和Apple Silicon (M1/M2)
- 需要 `libopencv_java470.dylib`
- 可能需要安装OpenCV: `brew install opencv@4`

### Linux

- 支持x86_64和ARM64架构
- 需要 `libopencv_java470.so`
- 可通过包管理器安装: `apt-get install libopencv4.2-java`

### Android

- 支持ARMv7、ARM64、x86、x86_64
- 需要将OpenCV Android SDK集成到项目
- 推荐使用静态链接方式

**build.gradle配置:**
```groovy
android {
    // ...
    
    sourceSets {
        main {
            jniLibs.srcDirs = ['libs/OpenCV-android-sdk/sdk/native/libs']
        }
    }
}

dependencies {
    implementation project(':OpenCV')
    implementation 'com.edge.vision:needle-measure-sdk:1.0.0:android'
}
```

## 高级用法

### 多尺度匹配参数

```java
// 自定义多尺度匹配参数
NeedleLengthAnalyzer analyzer = new NeedleLengthAnalyzer(
    "template.png",
    0.5,    // 最小缩放比例
    1.5,    // 最大缩放比例
    0.05    // 缩放步长
);
```

### 从内存中的图像测量

```java
// 从Mat对象测量
Mat image = Imgcodecs.imread("image.jpg");
MeasurementResult result = analyzer.analyze(image);

// 从字节数组测量（适用于网络/相机场景）
byte[] imageBytes = ...;
MeasurementResult result = analyzer.analyze(imageBytes);
```

### 生成可视化结果

```java
// 生成带标注的图像
Mat image = Imgcodecs.imread("image.jpg");
MeasurementResult result = analyzer.analyze(image);
Mat visualization = analyzer.generateVisualization(image, result);
Imgcodecs.imwrite("result.png", visualization);
```

### Android Bitmap转换

```java
// Bitmap -> Mat
Bitmap bitmap = ...;
Mat mat = new Mat();
Utils.bitmapToMat(bitmap, mat);

// Mat -> Bitmap
Mat result = ...;
Bitmap resultBitmap = Bitmap.createBitmap(result.cols(), result.rows(), Bitmap.Config.ARGB_8888);
Utils.matToBitmap(result, resultBitmap);
```

## 模板格式

模板由两个文件组成:

1. **模板图像** (`.png`): 包含针的图像
2. **元数据文件** (`.meta`): 包含测量参数

**元数据格式:**
```properties
# 针模板元数据
template.id=needle_50mm
template.created=2024-01-15T10:30:00
needle.length.mm=50.0
tip1.x=100.0
tip1.y=200.0
tip2.x=500.0
tip2.y=200.0
tip.patch.size=30
mm.per.pixel=0.125
```

## 自动构建与发布（GitHub Actions）

本项目已配置 **GitHub Actions**，可自动构建并发布**开箱即用**的 SDK。

### 📥 下载预编译 SDK（开箱即用）

**方式1：GitHub Releases（推荐）**
- 访问 [Releases](../../releases) 页面
- 下载对应版本的 SDK

**桌面平台（无需配置 OpenCV）：**
```bash
# 1. 下载 desktop-complete.zip
# 2. 解压
# 3. 直接使用（示例代码已包含）
unzip needle-measure-sdk-1.0.0-desktop-complete.zip
cd needle-measure-sdk-1.0.0
java -cp "needle-measure-sdk-1.0.0-desktop-all.jar:example" com.edge.vision.example.DesktopExample
```

**方式2：手动触发构建**
- 进入 [Actions](../../actions) 页面
- 选择 "Build and Release SDK"
- 点击 "Run workflow"

### 📦 发布的产物

| 文件名 | 说明 | 特点 |
|--------|------|------|
| `needle-measure-sdk-X.X.X-desktop-complete.zip` | 桌面完整版 | 开箱即用，含 OpenCV |
| `needle-measure-sdk-X.X.X-desktop-all.jar` | Fat-jar | 单文件，139MB，含所有依赖 |
| `needle-measure-sdk-X.X.X-android-complete.zip` | Android 完整版 | 含示例项目 |
| `needle-measure-sdk-X.X.X-desktop.jar` | 桌面标准版 | 需自行添加 OpenCV |
| `needle-measure-sdk-X.X.X-sources.jar` | 源码包 | - |
| `needle-measure-sdk-X.X.X-javadoc.jar` | 文档包 | - |

### ✅ 自动测试

每个 Release 都经过以下处理：
- **桌面端 SDK**：本地构建测试（使用 `template/` 和 `testimges/` 中的文件验证）
- **Android SDK**：GitHub Actions 自动构建（OpenCV Android SDK + 示例项目）

### 🏷️ 发布新版本

```bash
# 1. 更新版本号（build.gradle 和 pom.xml）
# 2. 提交并推送
git add .
git commit -m "Release version 1.0.1"
git tag v1.0.1
git push origin main
git push origin v1.0.1  # 推送 tag 自动触发 Release
```

详见 [RELEASE.md](RELEASE.md)

## 本地构建

```bash
# 构建所有jar
./gradlew build

# 构建桌面平台jar
./gradlew desktopJar

# 构建Android平台jar
./gradlew androidJar

# 创建分发包
./gradlew dist

# 创建精简分发包
./gradlew distMinimal
```

## 依赖

- **OpenCV 4.x**: 核心图像处理库
- **Java 8+**: 最低Java版本要求

## 许可证

Apache License 2.0

## 常见问题

### Q: OpenCV加载失败怎么办？

A: 检查以下几点:
1. 确保OpenCV库文件在系统路径或指定路径中
2. 检查架构匹配（32位/64位）
3. 尝试使用nu.pattern依赖
4. 在Android上确保已调用初始化方法

### Q: 测量精度不够怎么办？

A: 可以尝试:
1. 使用更高质量的模板图像
2. 调整针尖特征块大小
3. 优化多尺度匹配参数
4. 确保目标图像与模板图像的光照条件一致

### Q: Android上如何处理相机预览？

A: 参考示例代码中的`measureFromCameraFrame`方法，将NV21格式的预览帧转换为Mat进行测量。

