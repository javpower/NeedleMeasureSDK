# Needle Measure SDK 使用示例

这个项目演示了如何在实际项目中引用本地的 `needle-measure-sdk` jar 文件。

## 项目结构

```
example-project/
├── build.gradle              # Gradle 构建配置
├── settings.gradle           # Gradle 设置
├── README.md                 # 本文件
├── libs/                     # 存放本地 jar 文件（需先构建 SDK）
└── src/main/java/com/example/
    └── ExampleApp.java       # 示例代码
```

## 关键配置说明

### 1. 引用本地 jar

在 `build.gradle` 中：

```groovy
dependencies {
    // 引用本地 jar 文件
    implementation files('libs/needle-measure-sdk-1.0.0-desktop.jar')
    
    // OpenCV 依赖（必须，SDK 本身不包含 OpenCV）
    implementation 'org.openpnp:opencv:4.7.0-0'
}
```

### 2. jar 文件位置

- 方式1：放在项目内的 `libs/` 目录（推荐）
- 方式2：使用绝对路径引用
  ```groovy
  implementation files('/absolute/path/to/needle-measure-sdk-1.0.0-desktop.jar')
  ```
- 方式3：放在其他位置，修改 `files()` 中的路径

## 使用步骤

### 1. 先构建 SDK jar

```bash
cd /Volumes/macEx/AI/needle-measure-sdk
./gradlew desktopJar
# 生成的 jar 在 build/libs/needle-measure-sdk-1.0.0-desktop.jar
```

### 2. 复制 jar 到示例项目

```bash
cp build/libs/needle-measure-sdk-1.0.0-desktop.jar example-project/libs/
```

### 3. 运行示例

```bash
cd example-project
../gradlew run
```

### 方式 2：打包后运行

```bash
cd example-project
../gradlew fatJar
java -jar build/libs/example-project-1.0.0-all.jar
```

## 如何在其他项目中使用

### Gradle 项目

```groovy
dependencies {
    // 方式1: 本地文件引用
    implementation files('/path/to/needle-measure-sdk-1.0.0-desktop.jar')
    
    // OpenCV 依赖（必须）
    implementation 'org.openpnp:opencv:4.7.0-0'
}
```

### Maven 项目

```xml
<dependency>
    <groupId>com.edge.vision</groupId>
    <artifactId>needle-measure-sdk</artifactId>
    <version>1.0.0</version>
    <scope>system</scope>
    <systemPath>/path/to/needle-measure-sdk-1.0.0-desktop.jar</systemPath>
</dependency>

<dependency>
    <groupId>org.openpnp</groupId>
    <artifactId>opencv</artifactId>
    <version>4.7.0-0</version>
</dependency>
```

### 纯 Java 命令行

```bash
# 编译
javac -cp ".:libs/needle-measure-sdk-1.0.0-desktop.jar:/path/to/opencv.jar" YourClass.java

# 运行
java -cp ".:libs/needle-measure-sdk-1.0.0-desktop.jar:/path/to/opencv.jar" YourClass
```

## 完整使用示例

```java
import com.edge.vision.platform.OpenCVInitializer;
import com.edge.vision.core.NeedleLengthAnalyzer;
import com.edge.vision.core.MeasurementResult;

public class YourApp {
    public static void main(String[] args) {
        // 1. 初始化 OpenCV
        OpenCVInitializer.initialize();
        
        // 2. 创建分析器（使用模板）
        try (NeedleLengthAnalyzer analyzer = 
                new NeedleLengthAnalyzer("path/to/template.png")) {
            
            // 3. 测量
            MeasurementResult result = analyzer.analyze("path/to/image.jpg");
            
            // 4. 获取结果
            System.out.println("长度: " + result.getLengthMm() + " mm");
            System.out.println("置信度: " + result.getConfidence());
            System.out.println("JSON: " + result.toJsonString());
        }
    }
}
```

## 注意事项

1. **OpenCV 依赖**：SDK 本身不包含 OpenCV，使用时必须单独添加 OpenCV 依赖
2. **平台兼容**：桌面版 jar 适用于 Windows、Mac、Linux
3. **模板文件**：需要准备模板图像（.png）和元数据文件（.meta）

## 更多文档

- [SDK 主 README](../README.md)
- [集成指南](../docs/INTEGRATION_GUIDE.md)
- [架构文档](../docs/ARCHITECTURE.md)
