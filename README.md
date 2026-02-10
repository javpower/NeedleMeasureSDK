# Needle Measure SDK

跨平台高精度针长度测量 SDK，基于 **JavaCV**（OpenCV Java 封装）实现。支持 Windows、Mac、Linux、Android 平台。

## 🚀 开箱即用

### 桌面平台

**下载即用，无需配置！**

```bash
# 下载 needle-measure-sdk-X.X.X-desktop-all.jar（Fat-jar，含所有依赖）
# 直接运行
java -cp needle-measure-sdk-1.0.0-desktop-all.jar com.edge.vision.example.DesktopExample
```

### Android 平台

**使用 JavaCV，Maven 依赖自动下载**

```gradle
dependencies {
    implementation files('libs/needle-measure-sdk-1.0.0-android.jar')
    
    // JavaCV for Android
    implementation 'org.bytedeco:javacv:1.5.9'
    implementation 'org.bytedeco:opencv:4.7.0-1.5.9:android-arm64'
}
```

## 📦 快速开始

### 桌面平台

```java
import com.edge.vision.core.NeedleLengthAnalyzer;
import com.edge.vision.core.MeasurementResult;

// 直接使用
NeedleLengthAnalyzer analyzer = new NeedleLengthAnalyzer("template.png");
MeasurementResult result = analyzer.analyze("image.jpg");

System.out.println("长度: " + result.getLengthMm() + " mm");
System.out.println("置信度: " + (result.getConfidence() * 100) + "%");
```

### Android 平台

```java
// 从 assets 加载模板
InputStream imageStream = getAssets().open("templates/needle.png");
InputStream metaStream = getAssets().open("templates/needle.meta");
NeedleLengthAnalyzer analyzer = new NeedleLengthAnalyzer(imageStream, metaStream);

// Bitmap -> 测量（JavaCV 自动处理）
Mat mat = new Mat();
Utils.bitmapToMat(bitmap, mat);
MeasurementResult result = analyzer.analyze(mat);
```

## 📥 下载

访问 [Releases](../../releases) 下载最新版本：

| 文件名 | 说明 |
|--------|------|
| `needle-measure-sdk-X.X.X-desktop-all.jar` | 桌面 Fat-jar（开箱即用）|
| `needle-measure-sdk-X.X.X-android.jar` | Android SDK |
| `needle-measure-sdk-X.X.X-desktop-complete.zip` | 桌面完整包 |
| `needle-measure-sdk-X.X.X-android-complete.zip` | Android 完整包 |

## 🛠️ 技术栈

- **JavaCV 1.5.9** - OpenCV Java 封装
- **OpenCV 4.7.0** - 核心图像处理
- **Java 8+** - 最低版本要求

## 📂 项目结构

```
needle-measure-sdk/
├── src/main/java/com/edge/vision/
│   ├── core/                    # 核心测量类
│   ├── platform/                # 平台适配
│   └── template/                # 模板工具
├── example-project/             # 桌面示例
├── android-example/             # Android 示例
└── template/                    # 示例模板
```

## 🔧 本地构建

```bash
# 构建所有包
./gradlew clean build distDesktop distAndroid

# 桌面端测试
./gradlew desktopFatJar
java -cp "build/libs/needle-measure-sdk-1.0.0-desktop-all.jar" \
    com.edge.vision.example.DesktopExample
```

## 📄 许可证

Apache License 2.0
