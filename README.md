# HSR Graphic Droid

<div align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" alt="HSR Graphic Droid Logo" width="128"/>
  
  ### Premium Graphics Customization Tool for Honkai: Star Rail
  
  [![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://www.android.com/)
  [![Kotlin](https://img.shields.io/badge/Language-Kotlin-purple.svg)](https://kotlinlang.org/)
  [![Material Design 3](https://img.shields.io/badge/Design-Material%203-blue.svg)](https://m3.material.io/)
  [![Root Required](https://img.shields.io/badge/Root-Required-red.svg)](https://magiskmanager.com/)
</div>

## 🌟 Features

### 🎮 Game Settings Management
- **Read Current Settings**: View your current graphics configuration
- **Advanced Editor**: Modify all graphics parameters with real-time preview
- **Smart Presets**: Quick apply Low, Medium, High, and Ultra presets
- **Backup & Restore**: Save unlimited configurations and restore anytime

### 🎨 Modern Design
- **Material Design 3**: Beautiful, flat, modern Google-style interface
- **Dark/Light Mode**: Automatic theme switching with system preference
- **Smooth Animations**: Fluid transitions and interactions
- **Modern Icons**: Clean, professional Material Design icons

### 🌍 Multi-Language Support
- 🇺🇸 English
- 🇮🇩 Bahasa Indonesia
- 🇨🇳 中文 (Chinese)
- 🇯🇵 日本語 (Japanese)

### ⚙️ Graphics Settings
All settings from `GraphicsSettings_Model`:

| Setting | Range | Description |
|---------|-------|-------------|
| **FPS** | 30-120 | Frame rate limit |
| **VSync** | On/Off | Vertical synchronization |
| **Render Scale** | 0.5-2.0x | Resolution multiplier |
| **Resolution Quality** | 0-5 | Overall resolution quality |
| **Shadow Quality** | 0-5 | Shadow detail level |
| **Light Quality** | 0-5 | Lighting quality |
| **Character Quality** | 0-5 | Character model detail |
| **Environment Quality** | 0-5 | Environment detail |
| **Reflection Quality** | 0-5 | Reflection detail |
| **SFX Quality** | 0-5 | Special effects quality |
| **Bloom Quality** | 0-5 | Bloom effect quality |
| **Anti-Aliasing** | On/Off | Edge smoothing |
| **Self Shadow** | 0-2 | Character self-shadow |

## 📋 Requirements

- **Android 8.0+** (API 26+)
- **Root Access** (Magisk or similar)
- **Honkai: Star Rail** installed (`com.HoYoverse.hkrpgoversea`)
- **Storage**: ~10MB

## 🚀 Installation

1. Download the latest APK from [Releases](https://github.com/yourusername/hsrgraphicdroid/releases)
2. Install the APK on your rooted Android device
3. Launch the app and grant root permissions when prompted
4. Start customizing your game graphics!

## 📱 Screenshots

| Home Screen | Graphics Editor | Backup Manager |
|-------------|----------------|----------------|
| ![Home](screenshots/home.png) | ![Editor](screenshots/editor.png) | ![Backup](screenshots/backup.png) |

## 🔧 How to Use

### First Time Setup
1. Open HSR Graphic Droid
2. Grant root permission when prompted
3. App will verify Honkai: Star Rail installation

### Reading Current Settings
1. Tap "Read Current Settings"
2. View your current graphics configuration
3. Settings are displayed in a formatted dialog

### Editing Graphics
1. Tap "Edit Graphics Settings"
2. Use sliders to adjust each parameter
3. Apply presets for quick configuration
4. Tap "Apply" to save changes to game
5. Restart the game to see effects

### Backup & Restore
1. **Create Backup**: Read current settings → Tap "Create Backup" → Enter name
2. **Restore Backup**: Tap "Restore" on any saved backup
3. **Delete Backup**: Tap delete icon on unwanted backups

### Changing Theme
1. Open Settings from menu
2. Select Light, Dark, or System Default
3. Theme changes immediately

### Changing Language
1. Open Settings from menu
2. Select your preferred language
3. App will restart with new language

## 🛠️ Technical Details

### Architecture
- **MVVM Pattern**: Clean separation of concerns
- **Kotlin Coroutines**: Async operations
- **Material Design 3**: Latest design system
- **LibSU**: Root access management
- **DataStore**: Modern preference storage

### Key Components
- `HsrGameManager`: Handles game data read/write operations
- `GraphicsSettings`: Data model for game settings
- `BackupAdapter`: RecyclerView adapter for backup list
- `PreferenceManager`: App settings management

### File Structure
```
app/
├── data/
│   ├── GraphicsSettings.kt      # Settings data model
│   └── BackupData.kt            # Backup data model
├── ui/
│   ├── MainActivity.kt          # Main screen
│   ├── GraphicsEditorActivity.kt # Settings editor
│   ├── SettingsActivity.kt      # App settings
│   └── BackupAdapter.kt         # Backup list adapter
└── utils/
    ├── HsrGameManager.kt        # Game data manager
    └── PreferenceManager.kt     # App preferences
```

### Game Data Location
```
/data/data/com.HoYoverse.hkrpgoversea/shared_prefs/
└── com.HoYoverse.hkrpgoversea.v2.playerprefs.xml
```

### Settings Format
Settings are stored as URL-encoded JSON:
```xml
<string name="GraphicsSettings_Model">%7B%22FPS%22%3A120%2C%22EnableVSync%22%3Afalse...</string>
```

Decoded format:
```json
{
  "FPS": 120,
  "EnableVSync": false,
  "RenderScale": 1.4,
  "ResolutionQuality": 5,
  ...
}
```

## ⚠️ Important Notes

### Safety
- **Always backup** your settings before making changes
- **Close the game** before applying new settings
- Settings take effect after game restart
- Invalid settings may cause game crashes

### Permissions
- **Root Access**: Required to read/write game data
- **Storage**: For backup file management

### Compatibility
- Tested on Android 8.0 - 15.0
- Works with Honkai: Star Rail Global version
- Requires active internet for first-time setup

## 🐛 Troubleshooting

### Root access denied
- Make sure your device is properly rooted
- Check Magisk/SuperSU app for permission grants
- Try reinstalling the app

### Game not found
- Verify Honkai: Star Rail is installed
- Check package name: `com.HoYoverse.hkrpgoversea`
- Try reinstalling the game

### Settings not applying
- Make sure game is completely closed
- Verify root permissions are granted
- Check if game data path exists
- Try force-closing the game using app button

### Backup restore failed
- Check root permissions
- Ensure game is not running
- Verify backup file is not corrupted

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👨‍💻 Developer

**iRedDragonICY**

## ⚖️ Disclaimer

This is an unofficial tool and is not affiliated with, endorsed by, or connected to HoYoverse or Honkai: Star Rail. Use at your own risk. The developers are not responsible for any bans or issues that may arise from using this tool.

## 🙏 Acknowledgments

- [HoYoverse](https://www.hoyoverse.com/) for Honkai: Star Rail
- [TopJohnWu](https://github.com/topjohnwu) for LibSU
- [Material Design](https://material.io/) for design guidelines
- Android community for support and feedback

---

<div align="center">
  Made with ❤️ for the Honkai: Star Rail community
  
  ⭐ Star this repo if you find it useful!
</div>
