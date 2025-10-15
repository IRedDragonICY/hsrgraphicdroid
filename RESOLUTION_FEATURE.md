# Fitur Resolusi Layar

## Deskripsi
Fitur ini memungkinkan pengguna untuk mengubah resolusi layar game Honkai: Star Rail menggunakan parameter `Screenmanager Resolution Width` dan `Screenmanager Resolution Height`.

## Fitur yang Ditambahkan

### 1. Data Model
- **File**: `GraphicsSettings.kt`
- Menambahkan field baru:
  - `screenWidth: Int` (default: 1920)
  - `screenHeight: Int` (default: 1080)
- Menambahkan fungsi helper:
  - `getResolutionPreset()`: Mendapatkan nama preset dari resolusi saat ini
  - `setResolutionPreset(preset: String)`: Mengatur resolusi berdasarkan preset

### 2. UI Components
- **File**: `activity_main.xml`
- Komponen yang ditambahkan:
  - Icon resolusi (`ic_screen_resolution.xml`)
  - Chip group untuk preset resolusi (360p, 720p, 1080p, 1440p, 4K)
  - Input field untuk width dan height (custom resolution)
  - TextView untuk menampilkan resolusi saat ini

### 3. Preset Resolusi
Aplikasi mendukung preset resolusi berikut:
- **360p**: 640 × 360
- **720p**: 1280 × 720
- **1080p**: 1920 × 1080 (default)
- **1440p**: 2560 × 1440
- **4K**: 3840 × 2160
- **Custom**: Pengguna dapat memasukkan resolusi manual

### 4. Fungsi MainActivity
- **File**: `MainActivity.kt`
- Fungsi yang ditambahkan:
  - `setResolutionPreset(preset: String)`: Mengatur resolusi berdasarkan preset
  - `updateResolutionFromInput()`: Update resolusi dari input manual
  - `updateResolutionDisplay()`: Update tampilan resolusi saat ini
  - `updateResolutionChips()`: Update chip yang dipilih berdasarkan resolusi

### 5. String Resources
Menambahkan string untuk 4 bahasa:
- **English** (`values/strings.xml`)
- **Bahasa Indonesia** (`values-id/strings.xml`)
- **中文** (`values-zh/strings.xml`)
- **日本語** (`values-ja/strings.xml`)

String yang ditambahkan:
- `screen_resolution`: Judul fitur
- `resolution_width`: Label untuk lebar
- `resolution_height`: Label untuk tinggi
- `resolution_presets`: Label untuk preset
- `custom_resolution`: Label untuk custom
- `screen_resolution_desc`: Deskripsi fitur

## Cara Penggunaan

### Menggunakan Preset
1. Buka aplikasi HSR Graphic Droid
2. Scroll ke bagian "Screen Resolution"
3. Pilih salah satu chip preset (360p, 720p, 1080p, 1440p, 4K)
4. Resolusi akan otomatis ter-set
5. Klik "Apply Settings Now" untuk menyimpan ke game

### Menggunakan Custom Resolution
1. Buka aplikasi HSR Graphic Droid
2. Scroll ke bagian "Screen Resolution"
3. Masukkan nilai width dan height secara manual
4. Tap di luar input field untuk update
5. Klik "Apply Settings Now" untuk menyimpan ke game

### Contoh Penggunaan dalam Code
```kotlin
// Set preset
settings.setResolutionPreset("1080p")
// Result: screenWidth = 1920, screenHeight = 1080

// Manual set
settings.screenWidth = 2772
settings.screenHeight = 1280

// Get preset name
val presetName = settings.getResolutionPreset()
// Result: "2772×1280" (custom resolution)
```

## Format Data XML
Dalam file game preferences, resolusi disimpan sebagai:
```xml
<int name="Screenmanager%20Resolution%20Width" value="1920"/>
<int name="Screenmanager%20Resolution%20Height" value="1080"/>
```

Format JSON (URL-encoded):
```json
{
  "Screenmanager Resolution Width": 1920,
  "Screenmanager Resolution Height": 1080
}
```

## Catatan
- Resolusi yang terlalu tinggi mungkin mempengaruhi performa game
- Pastikan device memiliki RAM dan GPU yang cukup untuk resolusi tinggi
- Game harus di-restart setelah mengubah resolusi
- Fitur ini memerlukan root access untuk mengubah file preferences game

## Screenshot Lokasi UI
Fitur resolusi terletak di bagian bawah settings card, setelah DLSS Quality dan sebelum tombol Apply/Save.

## File yang Dimodifikasi
1. `app/src/main/java/com/ireddragonicy/hsrgraphicdroid/data/GraphicsSettings.kt`
2. `app/src/main/java/com/ireddragonicy/hsrgraphicdroid/ui/MainActivity.kt`
3. `app/src/main/res/layout/activity_main.xml`
4. `app/src/main/res/values/strings.xml`
5. `app/src/main/res/values-id/strings.xml`
6. `app/src/main/res/values-zh/strings.xml`
7. `app/src/main/res/values-ja/strings.xml`
8. `app/src/main/res/drawable/ic_screen_resolution.xml` (baru)
