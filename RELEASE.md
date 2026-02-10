# 🚀 SDK 自动发布指南

本项目已配置 **GitHub Actions**，可以自动构建并发布开箱即用的 SDK。

## 📦 获取 SDK 的方式

### 方式一：直接从 GitHub Releases 下载（推荐）

每次推送 tag 后，GitHub Actions 会自动创建 Release，你可以直接下载开箱即用的 SDK：

1. 访问项目的 [Releases](../../releases) 页面
2. 下载对应版本的 SDK：

**桌面平台（开箱即用，无需配置 OpenCV）：**
- `needle-measure-sdk-X.X.X-desktop-complete.zip` - 桌面完整版（推荐）
- `needle-measure-sdk-X.X.X-desktop-all.jar` - Fat-jar（含 OpenCV，139MB）

**Android 平台：**
- `needle-measure-sdk-X.X.X-android-complete.zip` - Android 完整版

### 方式二：手动触发构建

1. 进入项目的 [Actions](../../actions) 页面
2. 选择 "Build and Release SDK" workflow
3. 点击 "Run workflow" 按钮
4. 构建完成后，在 Artifacts 中下载 SDK

### 方式三：本地构建

```bash
# 克隆仓库
git clone <repository-url>
cd needle-measure-sdk

# 使用 Gradle 构建
./gradlew clean build desktopFatJar distDesktop distAndroid

# 产物位置：
# - build/libs/ - JAR 文件
# - build/dist/ - 分发包
```

## 🏷️ 发布新版本

要发布一个新版本，只需推送一个 tag：

```bash
# 1. 更新版本号（编辑 build.gradle 和 pom.xml）

# 2. 提交更改
git add .
git commit -m "Release version 1.0.1"

# 3. 打 tag
git tag v1.0.1

# 4. 推送
git push origin main
git push origin v1.0.1
```

推送 tag 后，GitHub Actions 会自动：
1. 在 Ubuntu/Windows/Mac 上运行测试
2. 构建 SDK（桌面版 + Android 版）
3. 使用真实模板和测试图片验证 SDK
4. 创建 GitHub Release
5. 上传所有产物到 Release 页面

## ✅ CI/CD 测试流程

每次构建都会自动执行以下测试：

### 桌面端 SDK 测试
1. **单元测试** - 基础功能验证
2. **SDK 集成测试** - 使用 template/ 目录下的真实模板
3. **端到端测试** - 使用 testimges/ 目录下的测试图片进行测量验证
4. **多平台构建测试** - Ubuntu/Windows/macOS

测试验证点：
- 模板加载 ✅
- 图像分析 ✅
- 测量结果输出 ✅
- JSON 序列化 ✅
- 可视化生成 ✅

### Android SDK 测试（GitHub Actions 自动完成）
1. **结构完整性测试** - 验证 JAR 包含所有必要类
2. **完整构建测试** - 使用 Android SDK + OpenCV Android SDK 构建示例项目
3. **APK 生成验证** - 确保能成功构建 APK
4. **包内容验证** - 确保模板文件包含在分发包中

### 桌面端 SDK 测试（本地进行）
桌面端 SDK 请在本地使用以下文件测试：
- **模板文件**：`template/needle_template_50mm.png`
- **测试图片**：`testimges/` 目录下的 5+ 张测试图片

**本地测试命令：**
```bash
./gradlew desktopFatJar
java -cp "build/libs/needle-measure-sdk-1.0.0-desktop-all.jar:example" \
    com.edge.vision.example.DesktopExample
```

## 📋 CI/CD 流程说明

### 自动触发条件

| 事件 | 触发行为 |
|------|---------|
| 推送到 main/master 分支 | 运行测试和构建 |
| 推送 tag (v*) | 运行测试 + 创建 Release |
| Pull Request | 运行测试 |
| 手动触发 workflow_dispatch | 运行完整流程 |

### 构建产物

每个 Release 包含以下文件：

| 文件名 | 说明 | 大小 |
|--------|------|------|
| `needle-measure-sdk-X.X.X-desktop-all.jar` | 桌面平台 Fat-jar（含 OpenCV，开箱即用） | ~139MB |
| `needle-measure-sdk-X.X.X-desktop-complete.zip` | 桌面完整分发包 | ~138MB |
| `needle-measure-sdk-X.X.X-android-complete.zip` | Android 完整分发包 | ~188KB |
| `needle-measure-sdk-X.X.X-desktop.jar` | 桌面平台标准版（需自行添加 OpenCV） | ~27KB |
| `needle-measure-sdk-X.X.X-android.jar` | Android 平台标准版 | ~27KB |
| `needle-measure-sdk-X.X.X-sources.jar` | 源码包 | ~17KB |
| `needle-measure-sdk-X.X.X-javadoc.jar` | 文档包 | ~70KB |

## 📚 使用示例

### 桌面平台（开箱即用）

**下载并解压：**
```bash
unzip needle-measure-sdk-1.0.0-desktop-complete.zip
cd needle-measure-sdk-1.0.0
```

**直接使用（无需配置 OpenCV）：**
```java
// 编译
javac -cp "needle-measure-sdk-1.0.0-desktop-all.jar" YourApp.java

// 运行
java -cp ".:needle-measure-sdk-1.0.0-desktop-all.jar" YourApp
```

**示例代码：**
```java
import com.edge.vision.core.NeedleLengthAnalyzer;
import com.edge.vision.core.MeasurementResult;

// 直接使用，无需初始化 OpenCV
NeedleLengthAnalyzer analyzer = new NeedleLengthAnalyzer("template.png");
MeasurementResult result = analyzer.analyze("image.jpg");

System.out.println("长度: " + result.getLengthMm() + " mm");
System.out.println("置信度: " + (result.getConfidence() * 100) + "%");
```

### Android 平台

**Gradle 依赖：**
```gradle
dependencies {
    implementation files('libs/needle-measure-sdk-1.0.0-android.jar')
    // 参考 android-example 集成 OpenCV
}
```

详见 `android-complete.zip` 中的示例项目。

## 🔧 高级配置

### 发布到 Maven Central

如需发布到 Maven Central，需要在 GitHub 仓库设置以下 Secrets：

- `MAVEN_USERNAME` - Sonatype OSSRH 用户名
- `MAVEN_PASSWORD` - Sonatype OSSRH 密码
- `SIGNING_KEY_ID` - GPG 签名 Key ID
- `SIGNING_PASSWORD` - GPG 签名密码
- `SIGNING_SECRET_KEY` - GPG 私钥内容

配置完成后，推送 tag 时会自动发布到 Maven Central。
