# 🚀 Study Buddy - Setup Guide

This guide will help you set up and run the Study Buddy Android application on your local development environment.

## 📋 Prerequisites

Before you begin, ensure you have the following installed:

### Required Software
- **Android Studio** (Arctic Fox or later) - [Download here](https://developer.android.com/studio)
- **JDK 17 or later** - [Download here](https://www.oracle.com/java/technologies/downloads/)
- **Android SDK** (API 24 or later)
- **Git** - [Download here](https://git-scm.com/)

### System Requirements
- **Operating System**: Windows 10/11, macOS 10.14+, or Linux
- **RAM**: 8GB minimum, 16GB recommended
- **Storage**: 10GB free space
- **Internet**: Required for downloading dependencies

## 🔧 Setup Instructions

### 1. Clone the Repository
```bash
git clone https://github.com/ashutosh48555/Study-Buddy.git
cd Study-Buddy
```

### 2. Open in Android Studio
1. Launch Android Studio
2. Click "Open an existing project"
3. Navigate to the cloned Study-Buddy folder
4. Click "OK"

### 3. Sync Project
1. Android Studio will automatically detect the Gradle files
2. Click "Sync Now" when prompted
3. Wait for the sync to complete (this may take a few minutes)

### 4. Configure Firebase (Optional)
The app includes Firebase configuration, but you can set up your own:

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Create a new project or use existing one
3. Add Android app with package name: `com.example.studybuddy`
4. Download `google-services.json` and replace the existing one in `app/` directory

### 5. Build and Run
1. Connect an Android device or start an emulator
2. Click the "Run" button (green play icon) or press `Shift + F10`
3. Select your target device
4. The app will build and install automatically

## 📱 Testing the Application

### Unit Tests
```bash
./gradlew test
```

### Instrumented Tests
```bash
./gradlew connectedAndroidTest
```

### Build APK
```bash
./gradlew assembleDebug
```

## 🛠️ Project Structure

```
Study-Buddy/
├── app/                          # Main application module
│   ├── src/main/
│   │   ├── java/                # Kotlin source code
│   │   ├── res/                 # Resources (layouts, drawables, etc.)
│   │   └── AndroidManifest.xml  # App configuration
│   ├── build.gradle.kts         # App-level dependencies
│   └── google-services.json     # Firebase configuration
├── gradle/                      # Gradle wrapper
├── .github/                     # GitHub templates
├── build.gradle.kts             # Project-level configuration
├── settings.gradle.kts          # Project settings
└── gradle.properties            # Gradle properties
```

## 🔍 Troubleshooting

### Common Issues

#### 1. Gradle Sync Failed
- **Solution**: Check your internet connection and try again
- **Alternative**: Use a VPN if you're in a restricted region

#### 2. Build Failed
- **Solution**: Clean and rebuild the project
  ```bash
  ./gradlew clean
  ./gradlew build
  ```

#### 3. Firebase Connection Issues
- **Solution**: Ensure `google-services.json` is in the correct location
- **Check**: Verify the package name matches in Firebase console

#### 4. Emulator Issues
- **Solution**: Create a new AVD with API 24 or higher
- **Alternative**: Use a physical device for testing

### Getting Help

If you encounter issues not covered here:

1. Check the [Issues](https://github.com/ashutosh48555/Study-Buddy/issues) page
2. Create a new issue with detailed information
3. Include your Android Studio version and error logs

## 📚 Development

### Code Style
- Follow Kotlin coding conventions
- Use meaningful variable and function names
- Add comments for complex logic
- Keep functions small and focused

### Contributing
1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test thoroughly
5. Submit a pull request

## 🎯 Features Overview

- **Task Management**: Create, edit, and organize tasks
- **Pomodoro Timer**: Focus with time management techniques
- **Analytics**: Track your productivity and progress
- **Profile Management**: Customize your study experience
- **Firebase Integration**: Cloud sync and authentication
- **Dark/Light Theme**: Customizable appearance

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

**Happy Coding! 🚀**

If you find this project helpful, please give it a ⭐ star!
