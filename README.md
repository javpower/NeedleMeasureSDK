# Needle Measure SDK

跨平台高精度针长度测量 SDK，基于 **JavaCV**（OpenCV Java 封装）实现。

支持 **Windows、Linux、macOS（Intel/Apple Silicon）、Android** 全平台。

## 特性

- 🚀 **开箱即用** - JavaCV 自动管理原生库，无需手动配置
- 📦 **瘦身打包** - 支持按平台打包，大幅减小包体积
- 🔧 **跨平台** - 统一 API，支持桌面端和移动端
- 🎯 **高精度** - 基于多尺度模板匹配的亚像素级测量

## 快速开始

### 方式一：直接使用（推荐）

下载对应平台的 Fat-JAR，直接运行：

```bash
# macOS (Apple Silicon)
java -jar needle-measure-sdk-1.0.0-macosx-arm64.jar

# macOS (Intel)
java -jar needle-measure-sdk-1.0.0-macosx-x86_64.jar

# Windows
java -jar needle-measure-sdk-1.0.0-windows-x86_64.jar

# Linux
java -jar needle-measure-sdk-1.0.0-linux-x86_64.jar
```

### 方式二：作为依赖使用

**Maven:**

```xml
<dependency>
    <groupId>com.edge.vision</groupId>
    <artifactId>needle-measure-sdk</artifactId>
    <version>1.0.0</version>
</dependency>

<!-- 添加 JavaCV OpenCV 依赖 -->
<dependency>
    <groupId>org.bytedeco</groupId>
    <artifactId>opencv</artifactId>
    <version>4.7.0-1.5.9</version>
    <!-- 选择你的平台 -->
    <classifier>windows-x86_64</classifier>  <!-- 或 macosx-arm64, linux-x86_64 等 -->
</dependency>
```

**Gradle:**

```groovy
implementation 'com.edge.vision:needle-measure-sdk:1.0.0'
implementation 'org.bytedeco:opencv:4.7.0-1.5.9:macosx-arm64'  // 选择你的平台
```

### 代码示例

```java
import com.edge.vision.core.NeedleLengthAnalyzer;
import com.edge.vision.core.MeasurementResult;

// 方式1：从文件加载模板
NeedleLengthAnalyzer analyzer = new NeedleLengthAnalyzer("template.png");
MeasurementResult result = analyzer.analyze("image.jpg");

// 方式2：从 InputStream 加载（适用于 Android）
InputStream imageStream = assets.open("template.png");
InputStream metaStream = assets.open("template.meta");
NeedleLengthAnalyzer analyzer = new NeedleLengthAnalyzer(imageStream, metaStream);

// 获取结果
System.out.println("长度: " + result.getLengthMm() + " mm");
System.out.println("置信度: " + (result.getConfidence() * 100) + "%");
System.out.println("JSON: " + result.toJsonString());
```

## 下载

| 文件 | 说明 | 大小 |
|------|------|------|
| `needle-measure-sdk-1.0.0-macosx-arm64.jar` | macOS Apple Silicon | ~15MB |
| `needle-measure-sdk-1.0.0-macosx-x86_64.jar` | macOS Intel | ~13MB |
| `needle-measure-sdk-1.0.0-windows-x86_64.jar` | Windows x64 | ~14MB |
| `needle-measure-sdk-1.0.0-linux-x86_64.jar` | Linux x64 | ~14MB |
| `needle-measure-sdk-1.0.0-android.jar` | Android SDK（需配合 JavaCV） | ~50KB |
| `needle-measure-sdk-1.0.0-all-platforms.jar` | 全平台（包含所有原生库） | ~180MB |

> 💡 **瘦身提示**: 使用平台特定的 JAR 可大幅减小体积。全平台版包含 6 个平台的原生库。

## 本地构建

```bash
# 编译 SDK
./gradlew build

# 运行测试
./gradlew test

# 构建特定平台
./gradlew shadowWindows      # Windows
./gradlew shadowLinux        # Linux
./gradlew shadowMacX64       # macOS Intel
./gradlew shadowMacArm64     # macOS Apple Silicon
./gradlew shadowAndroid      # Android

# 构建全平台
./gradlew buildAllPlatforms

# 创建分发包
./gradlew distWindows        # Windows 分发包
./gradlew distLinux          # Linux 分发包
./gradlew distMacOS          # macOS 分发包
./gradlew distAndroid        # Android 分发包
./gradlew distAll            # 所有分发包
```

## Android 集成

```groovy
// app/build.gradle
dependencies {
    // SDK
    implementation files('libs/needle-measure-sdk-1.0.0-android.jar')

    // JavaCV OpenCV（按需选择架构）
    implementation 'org.bytedeco:opencv:4.7.0-1.5.9:android-arm64'
    // implementation 'org.bytedeco:opencv:4.7.0-1.5.9:android-arm'
    // implementation 'org.bytedeco:opencv:4.7.0-1.5.9:android-x86'
    // implementation 'org.bytedeco:opencv:4.7.0-1.5.9:android-x86_64'
}
```

Android 代码示例：

```java
// 初始化（JavaCV 自动加载）
OpenCVInitializer.initialize(context);

// 从 assets 加载模板
InputStream imageStream = getAssets().open("templates/needle.png");
InputStream metaStream = getAssets().open("templates/needle.meta");
NeedleLengthAnalyzer analyzer = new NeedleLengthAnalyzer(imageStream, metaStream);

// Bitmap -> 测量
AndroidFrameConverter converter = new AndroidFrameConverter();
Mat mat = new OpenCVFrameConverter.ToMat().convert(converter.convert(bitmap));
MeasurementResult result = analyzer.analyze(mat);
```

## 项目结构

```
needle-measure-sdk/
├── src/main/java/com/edge/vision/
│   ├── core/                    # 核心测量类
│   │   ├── NeedleLengthAnalyzer.java
│   │   ├── AnalysisTemplate.java
│   │   └── MeasurementResult.java
│   ├── platform/                # 平台适配
│   │   ├── OpenCVInitializer.java
│   │   ├── DesktopOpenCVLoader.java
│   │   └── AndroidOpenCVLoader.java
│   ├── template/                # 模板工具
│   │   └── TemplateBuilder.java
│   └── utils/                   # 工具类
│       └── ImageUtils.java
├── android-example/             # Android 示例
├── example-project/             # 桌面示例
└── template/                    # 示例模板
```

## 技术栈

| 组件 | 版本 |
|------|------|
| JavaCV | 1.5.9 |
| OpenCV | 4.7.0 |
| Java | 11+ |

## 环境要求

- **JDK 11+**（推荐 JDK 11 或 JDK 17）
- **Gradle 7.5+**（已包含 wrapper）

```bash
# 检查 Java 版本
java -version  # 需要 11 或更高

# 如果有多个 Java 版本，设置 JAVA_HOME
export JAVA_HOME=/path/to/java11/or/java17
```

## 许可证

Apache License 2.0
