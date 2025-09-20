# Lofi Music Setup Instructions

This document explains how to add real lofi music tracks to the StudyBuddy app.

## Current Implementation

The app currently uses placeholder audio files (timer_complete.ogg) for all tracks. To add real lofi music:

## Steps to Add Real Lofi Tracks

### 1. Prepare Audio Files
- Convert your lofi tracks to `.ogg` format (recommended) or `.mp3`
- Keep file sizes reasonable (under 5MB each)
- Ensure audio quality is good but not excessive (128kbps is usually sufficient)

### 2. Add Files to Raw Directory
Place your audio files in: `app/src/main/res/raw/`

Example files:
- `lofi_chill_vibes.ogg`
- `lofi_study_focus.ogg`
- `lofi_rainy_day.ogg`
- `lofi_coffee_shop.ogg`
- `lofi_midnight_study.ogg`
- `lofi_peaceful_mind.ogg`
- `lofi_soft_breeze.ogg`
- `lofi_calm_thoughts.ogg`

### 3. Update LofiMusicService.kt
Replace the placeholder resource IDs in the `lofiTracks` list:

```kotlin
private val lofiTracks = listOf(
    LofiTrack("Chill Vibes", R.raw.lofi_chill_vibes, "3:45"),
    LofiTrack("Study Focus", R.raw.lofi_study_focus, "4:12"),
    LofiTrack("Rainy Day", R.raw.lofi_rainy_day, "3:28"),
    LofiTrack("Coffee Shop", R.raw.lofi_coffee_shop, "4:05"),
    LofiTrack("Midnight Study", R.raw.lofi_midnight_study, "3:52"),
    LofiTrack("Peaceful Mind", R.raw.lofi_peaceful_mind, "4:20"),
    LofiTrack("Soft Breeze", R.raw.lofi_soft_breeze, "3:35"),
    LofiTrack("Calm Thoughts", R.raw.lofi_calm_thoughts, "4:08")
)
```

### 4. Update Track Durations
Update the duration strings to match your actual track lengths.

## Features

The music player includes:
- **Play/Pause**: Control playback
- **Next/Previous**: Navigate between tracks
- **Shuffle**: Random track selection
- **Background Playback**: Music continues when app is in background
- **Notification Controls**: Control music from notification panel

## Free Lofi Music Sources

For testing purposes, you can find royalty-free lofi music at:
- [YouTube Audio Library](https://www.youtube.com/audiolibrary)
- [Zapsplat](https://www.zapsplat.com/) (requires free account)
- [Freesound](https://freesound.org/)
- [Chosic](https://www.chosic.com/free-music/all/)

## Legal Considerations

- Ensure you have proper licensing for any music you include
- Consider using Creative Commons licensed music
- For commercial distribution, purchase appropriate licenses
- Always credit artists when required

## APK Size Considerations

- Each audio file will increase your APK size
- Consider using streaming instead of bundled files for production
- Compress audio files appropriately
- Test app performance with all files included

## Advanced Features (Future)

Potential enhancements:
- Online streaming of lofi tracks
- User-uploaded music support
- Playlist creation
- Sleep timer
- Equalizer settings
- Volume fade in/out
