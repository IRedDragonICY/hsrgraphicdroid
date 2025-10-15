# Build Instructions

## Prerequisites

1. **Android Studio** (latest version recommended)
   - Download from: https://developer.android.com/studio

2. **JDK 17** or higher
   - Included with Android Studio or download from: https://adoptium.net/

3. **Android SDK**
   - API Level 26 (Android 8.0) minimum
   - API Level 35 (Android 15) target
   - Install via Android Studio SDK Manager

## Building the Project

### Option 1: Using Android Studio (Recommended)

1. **Open Project**
   ```
   File → Open → Select hsrgraphicdroid folder
   ```

2. **Sync Gradle**
   - Wait for Gradle sync to complete
   - If sync fails, check internet connection and retry

3. **Build APK**
   ```
   Build → Build Bundle(s) / APK(s) → Build APK(s)
   ```
   
4. **Output Location**
   ```
   app/build/outputs/apk/debug/app-debug.apk
   ```

### Option 2: Using Gradle Command Line

1. **Debug Build**
   ```bash
   # Windows (PowerShell)
   .\gradlew assembleDebug
   
   # Linux/Mac
   ./gradlew assembleDebug
   ```

2. **Release Build**
   ```bash
   # Windows (PowerShell)
   .\gradlew assembleRelease
   
   # Linux/Mac
   ./gradlew assembleRelease
   ```

## Signing the APK (Release Build)

### Generate Keystore

```bash
keytool -genkey -v -keystore hsrgraphicdroid.keystore -alias hsrgraphicdroid -keyalg RSA -keysize 2048 -validity 10000
```

### Configure Signing

1. Create `keystore.properties` in project root:
   ```properties
   storePassword=your_store_password
   keyPassword=your_key_password
   keyAlias=hsrgraphicdroid
   storeFile=path/to/hsrgraphicdroid.keystore
   ```

2. Update `app/build.gradle.kts`:
   ```kotlin
   android {
       signingConfigs {
           create("release") {
               val keystorePropertiesFile = rootProject.file("keystore.properties")
               val keystoreProperties = Properties()
               keystoreProperties.load(FileInputStream(keystorePropertiesFile))
               
               keyAlias = keystoreProperties["keyAlias"] as String
               keyPassword = keystoreProperties["keyPassword"] as String
               storeFile = file(keystoreProperties["storeFile"] as String)
               storePassword = keystoreProperties["storePassword"] as String
           }
       }
       
       buildTypes {
           release {
               signingConfig = signingConfigs.getByName("release")
               isMinifyEnabled = true
               proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
           }
       }
   }
   ```

3. Build signed APK:
   ```bash
   .\gradlew assembleRelease
   ```

## Installing on Device

### Via ADB

```bash
# Install debug build
adb install app/build/outputs/apk/debug/app-debug.apk

# Install release build
adb install app/build/outputs/apk/release/app-release.apk

# Uninstall
adb uninstall com.ireddragonicy.hsrgraphicdroid
```

### Via Android Studio

1. Connect device via USB
2. Enable USB Debugging on device
3. Click "Run" button (green triangle) in Android Studio
4. Select your device from list

## Troubleshooting

### Gradle Sync Failed

**Problem**: Gradle sync fails with dependency errors

**Solution**:
```bash
# Clear Gradle cache
.\gradlew clean

# Invalidate caches in Android Studio
File → Invalidate Caches → Invalidate and Restart
```

### Build Failed: SDK Not Found

**Problem**: Android SDK path not configured

**Solution**:
1. Open SDK Manager in Android Studio
2. Install required SDK platforms and tools
3. Update `local.properties`:
   ```properties
   sdk.dir=C\:\\Users\\YourName\\AppData\\Local\\Android\\Sdk
   ```

### LibSU Dependency Error

**Problem**: Cannot resolve LibSU dependency

**Solution**:
- Ensure `maven { url = uri("https://jitpack.io") }` is in `settings.gradle.kts`
- Check internet connection
- Try Gradle sync again

### OutOfMemoryError

**Problem**: Gradle runs out of memory during build

**Solution**:
Add to `gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx4096m -XX:MaxPermSize=512m -XX:+HeapDumpOnOutOfMemoryError -Dfile.encoding=UTF-8
```

## Development

### Running Tests

```bash
# Unit tests
.\gradlew test

# Instrumented tests (requires connected device)
.\gradlew connectedAndroidTest
```

### Code Style

This project follows Kotlin coding conventions:
- Use 4 spaces for indentation
- Maximum line length: 120 characters
- Follow Material Design guidelines

### Debugging

1. Enable debugging in Android Studio
2. Set breakpoints in code
3. Run in debug mode (Shift + F9)
4. Use Android Logcat for logs

## Performance Optimization

### ProGuard Rules

For release builds, ProGuard is enabled. Add custom rules in `app/proguard-rules.pro`:

```proguard
# Keep model classes
-keep class com.ireddragonicy.hsrgraphicdroid.data.** { *; }

# Keep LibSU
-keep class com.topjohnwu.superuser.** { *; }

# Keep Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
```

## CI/CD

### GitHub Actions Example

Create `.github/workflows/build.yml`:

```yaml
name: Android CI

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'
        
    - name: Grant execute permission for gradlew
      run: chmod +x gradlew
      
    - name: Build with Gradle
      run: ./gradlew build
      
    - name: Upload APK
      uses: actions/upload-artifact@v3
      with:
        name: app-debug
        path: app/build/outputs/apk/debug/app-debug.apk
```

## Support

For build issues:
1. Check this documentation
2. Search existing GitHub issues
3. Create new issue with:
   - Android Studio version
   - Gradle version
   - Error logs
   - Steps to reproduce

---

Happy Building! 🚀
