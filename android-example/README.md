# Android 项目使用 Needle Measure SDK 示例

这个示例展示了如何在 Android 项目中使用 Needle Measure SDK。

## 🚀 快速开始（GitHub Actions 自动构建）

本项目已配置 GitHub Actions，会自动构建 Android 示例项目：
- 下载 OpenCV Android SDK
- 集成 Needle Measure SDK
- 构建完整 APK

你可以在 [Actions](../../actions) 页面查看构建结果，或下载预编译的 SDK。

---

## 📦 手动集成步骤

## 📁 项目结构

```
android-example/
├── app/
│   ├── build.gradle                    # App 模块构建配置
│   ├── libs/                           # SDK jar 和 OpenCV SDK 放在这里
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/example/needleapp/
│       │   ├── MainActivity.java       # 主界面
│       │   ├── AndroidOpenCVLoader.java # OpenCV 加载器（参考实现）
│       │   └── MyApplication.java      # Application 类
│       └── res/                        # 资源文件
├── build.gradle                        # 项目级构建配置
└── settings.gradle
```

## 🚀 快速开始

### 1. 获取 SDK jar

```bash
# 在主项目目录构建 Android 版 jar
cd /Volumes/macEx/AI/needle-measure-sdk
./gradlew androidJar

# 复制到示例项目
cp build/libs/needle-measure-sdk-1.0.0-android.jar android-example/app/libs/
```

### 2. 导入 OpenCV Android SDK

1. 下载 [OpenCV Android SDK](https://opencv.org/releases/)
2. 解压并将 `sdk` 目录复制到 `app/libs/OpenCV-android-sdk/`

### 2. 配置 build.gradle

**项目级 build.gradle:**
```groovy
allprojects {
    repositories {
        google()
        mavenCentral()
    }
}
```

**App 级 build.gradle:**
```groovy
android {
    compileSdk 34
    
    defaultConfig {
        minSdk 21
        targetSdk 34
        
        ndk {
            abiFilters 'armeabi-v7a', 'arm64-v8a', 'x86', 'x86_64'
        }
    }
    
    sourceSets {
        main {
            jniLibs.srcDirs = ['libs/OpenCV-android-sdk/sdk/native/libs']
        }
    }
}

dependencies {
    // SDK jar
    implementation files('libs/needle-measure-sdk-1.0.0-android.jar')
    
    // OpenCV Android SDK
    implementation fileTree(dir: 'libs/OpenCV-android-sdk/sdk/java/libs', include: ['*.jar'])
    
    // 其他 Android 依赖
    implementation 'androidx.appcompat:appcompat:1.6.1'
}
```

### 3. 创建 Application 类初始化 OpenCV

```java
public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        
        // 初始化 OpenCV（静态链接方式）
        try {
            OpenCVInitializer.initialize(this, true);
        } catch (Exception e) {
            Log.e("MyApp", "OpenCV 初始化失败", e);
        }
    }
}
```

### 4. 在 AndroidManifest.xml 中注册 Application

```xml
<application
    android:name=".MyApplication"
    android:allowBackup="true"
    android:icon="@mipmap/ic_launcher"
    android:label="@string/app_name">
    <!-- ... -->
</application>
```

### 5. 在 Activity 中使用 SDK

```java
public class MainActivity extends AppCompatActivity {
    
    private NeedleLengthAnalyzer analyzer;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // 从 assets 加载模板
        loadTemplate();
        
        // 设置测量按钮
        findViewById(R.id.measureButton).setOnClickListener(v -> {
            // 获取 Bitmap（从相机或相册）
            Bitmap bitmap = ...;
            performMeasurement(bitmap);
        });
    }
    
    private void loadTemplate() {
        try {
            InputStream imageStream = getAssets().open("templates/needle.png");
            InputStream metaStream = getAssets().open("templates/needle.meta");
            analyzer = new NeedleLengthAnalyzer(imageStream, metaStream);
        } catch (IOException e) {
            Toast.makeText(this, "模板加载失败", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void performMeasurement(Bitmap bitmap) {
        new Thread(() -> {
            Mat mat = new Mat();
            Utils.bitmapToMat(bitmap, mat);
            
            MeasurementResult result = analyzer.analyze(mat);
            
            runOnUiThread(() -> {
                String msg = String.format("长度: %.2f mm", result.getLengthMm());
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            });
            
            mat.release();
        }).start();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (analyzer != null) {
            analyzer.close();
        }
    }
}
```

## 📋 完整文件示例

### AndroidOpenCVLoader.java（必须在项目中实现）

```java
package com.example.needleapp;

import android.content.Context;
import android.util.Log;
import com.edge.vision.platform.OpenCVLoader;
import org.opencv.core.Core;

public class AndroidOpenCVLoader implements OpenCVLoader {
    
    private static final String TAG = "AndroidOpenCVLoader";
    private static volatile boolean loaded = false;
    private static final Object lock = new Object();
    
    private final Context context;
    
    public AndroidOpenCVLoader(Object context) {
        // 获取 Application Context
        Context ctx = (Context) context;
        this.context = ctx.getApplicationContext();
    }
    
    public AndroidOpenCVLoader(Object context, boolean useStaticLinking) {
        this(context);
    }
    
    @Override
    public void load() throws RuntimeException {
        if (loaded) return;
        
        synchronized (lock) {
            if (loaded) return;
            
            try {
                // 使用 OpenCV Android SDK 的静态链接方式
                boolean success = org.opencv.android.OpenCVLoader.initDebug();
                
                if (!success) {
                    System.loadLibrary("opencv_java4");
                }
                
                loaded = true;
                Log.i(TAG, "OpenCV 加载成功");
            } catch (Exception e) {
                throw new RuntimeException("OpenCV 加载失败: " + e.getMessage(), e);
            }
        }
    }
    
    @Override
    public boolean isLoaded() {
        return loaded;
    }
    
    @Override
    public String getPlatformName() {
        return "Android " + android.os.Build.VERSION.RELEASE;
    }
    
    @Override
    public String getOpenCVVersion() {
        return loaded ? Core.VERSION : "Not loaded";
    }
}
```

### build.gradle (App)

```groovy
plugins {
    id 'com.android.application'
}

android {
    namespace 'com.example.needleapp'
    compileSdk 34

    defaultConfig {
        applicationId "com.example.needleapp"
        minSdk 21
        targetSdk 34
        versionCode 1
        versionName "1.0"

        ndk {
            abiFilters 'armeabi-v7a', 'arm64-v8a', 'x86', 'x86_64'
        }
    }

    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }
    
    sourceSets {
        main {
            jniLibs.srcDirs = ['libs/OpenCV-android-sdk/sdk/native/libs']
        }
    }
}

dependencies {
    // Needle Measure SDK
    implementation files('libs/needle-measure-sdk-1.0.0-android.jar')
    
    // OpenCV Android SDK
    implementation fileTree(dir: 'libs/OpenCV-android-sdk/sdk/java/libs', include: ['*.jar'])
    
    // AndroidX
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'com.google.android.material:material:1.9.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
}
```

## 📝 关键要点

1. **OpenCV 初始化**：在 `Application.onCreate()` 中初始化
2. **模板文件**：放在 `assets/` 目录下
3. **后台线程**：图像处理在后台线程执行，避免阻塞 UI
4. **资源释放**：使用 `analyzer.close()` 和 `mat.release()` 释放资源
5. **ABI 配置**：确保包含设备支持的 ABI 架构

## ⚠ 常见问题

### Q: 报错 `UnsatisfiedLinkError: dlopen failed`

A: 检查以下配置：
- `jniLibs.srcDirs` 路径正确
- OpenCV SDK 的 `.so` 文件在对应目录
- `abiFilters` 包含目标设备的 ABI

### Q: 报错 `ClassNotFoundException: AndroidOpenCVLoader`

A: 确保在项目中实现了 `AndroidOpenCVLoader` 类，并且放在正确的包名下。

### Q: 如何创建模板？

A: 模板需要在桌面端创建：
1. 使用桌面版的 TemplateBuilder 创建模板
2. 将生成的 `.png` 和 `.meta` 文件复制到 Android 项目的 `assets/` 目录