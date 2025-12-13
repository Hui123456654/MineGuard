plugins {
    id("com.android.application")
}

android {
    namespace = "com.example.mineguard"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.mineguard"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.13.0")
    implementation("androidx.activity:activity:1.10.1")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("com.google.code.gson:gson:2.10.1")
    // Chart library
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation("org.videolan.android:libvlc-all:3.5.1")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    implementation("com.google.android.material:material:1.13.0")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("com.github.bumptech.glide:glide:4.12.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.12.0")
    // 网络请求相关依赖
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.3")


    //推流
    // Media3 ExoPlayer 核心库
    implementation("androidx.media3:media3-exoplayer:1.2.0")
    // 专门用于支持 RTSP 协议的模块
    implementation("androidx.media3:media3-exoplayer-rtsp:1.2.0")
    // 播放器 UI 组件 (PlayerView)
    implementation("androidx.media3:media3-ui:1.2.0")

    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    implementation("org.nanohttpd:nanohttpd:2.3.1")
    implementation ("com.github.bumptech.glide:glide:4.12.0") // 用于加载本地图片
    annotationProcessor ("com.github.bumptech.glide:compiler:4.12.0")
    implementation("com.squareup.okhttp3:okhttp:4.9.0")
    implementation("org.nanohttpd:nanohttpd:2.3.1")

}