# pixel-dodge-android-game

Retro pixel-art Android game project built with Kotlin and Jetpack Compose

## Pixel Dodge

This project contains a simple pixel-style dodge game for Android. The player slides left and right to avoid incoming red enemy blocks. Each second survived increases the score.

## Run in GitHub Codespaces

1. Open the repository in GitHub Codespaces.
2. Wait for the devcontainer to finish installing Java and Android SDK tools.
3. In the terminal, run:

```bash
cd /workspaces/pixel-dodge-android-game
./gradlew assembleDebug
```

4. The APK will be created here:

```bash
app/build/outputs/apk/debug/app-debug.apk
```

## Notes

- This environment is designed for remote Android project builds without a local PC.
- If you want to run the app in an emulator inside Codespaces, you can also start the Android emulator from the terminal after the SDK setup finishes.

## Game features

- Pixel-art arcade look
- Touch drag movement
- Increasing enemy speed
- Score tracking and retry flow
