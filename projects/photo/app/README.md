app/
├── build.gradle.kts
├── src/main/
│   ├── AndroidManifest.xml
│   ├── java/com/example/photouploader/
│   │   ├── MainActivity.kt
│   │   ├── SettingsActivity.kt
│   │   ├── SearchActivity.kt
│   │   ├── CameraActivity.kt
│   │   ├── data/
│   │   │   ├── ApiService.kt
│   │   │   ├── AuthInterceptor.kt
│   │   │   ├── RetrofitClient.kt
│   │   │   ├── SettingsManager.kt
│   │   │   └── model/
│   │   │       ├── SearchResult.kt
│   │   │       └── UploadResponse.kt
│   │   ├── util/
│   │   │   ├── ImageCompressor.kt
│   │   │   └── ExifHelper.kt
│   │   └── view/
│   │       └── CrosshairView.kt
│   └── res/
│       ├── layout/
│       │   ├── activity_main.xml
│       │   ├── activity_settings.xml
│       │   ├── activity_search.xml
│       │   └── activity_camera.xml
│       └── values/
│           └── strings.xml
└── build.gradle.kts (project)