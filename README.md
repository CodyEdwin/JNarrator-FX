# JNarrator FX

A Text-to-Speech narrator application built with JavaFX featuring a modern dark theme UI and **high-quality neural voices**.

## Features

- **Piper TTS Integration**: Natural-sounding neural voices (offline, free)
- **Multiple Voice Options**: US/UK Male/Female voices
- **Font Customization**: Change font family and size
- **Text Input**: Large text area for typing or pasting content
- **File Support**: Open `.txt` and `.md` files
- **Voice Controls**: Adjustable speed (0.5x - 2.0x) and volume
- **Audio Export**: Save narration as WAV audio file
- **Playback Controls**: Play, Pause, and Stop functionality
- **Fallback System TTS**: Windows SAPI, macOS `say`, Linux `espeak-ng`

## TTS Engines

### Piper TTS (Recommended)
High-quality neural text-to-speech that runs locally. Voices sound natural and human-like.

**Available Voices:**
- Amy (US Female)
- Ryan (US Male)
- Lessac (US Female)
- Jenny (UK Female)
- Alan (UK Male)

### System Default
Falls back to OS-native TTS (lower quality but no setup required).

## Requirements

- Java 17 or higher
- Maven 3.6+
- JavaFX 21 (automatically downloaded by Maven)

### For Piper TTS (optional but recommended)
1. Download Piper from: https://github.com/rhasspy/piper/releases
2. Extract to `~/piper/`
3. Voice models are downloaded automatically via the app

### For System TTS fallback
- **Linux**: `sudo apt install espeak-ng alsa-utils`
- **Windows/macOS**: Built-in, no installation needed

## Build & Run

```bash
# Clone and navigate to project
cd JNarrator-FX

# Run directly
mvn clean javafx:run

# Or build JAR
mvn clean package
```

## Usage

1. **Select TTS Engine**: Choose "Piper (Neural - Best)" or "System Default"
2. **Download Voice**: Click "Download Voice" to get a Piper voice model
3. **Enter Text**: Type or paste text in the main area
4. **Customize Font**: Adjust font family and size in settings
5. **Play**: Click the Play button to start narration
6. **Export**: Use File > Export to WAV to save audio

## Project Structure

```
JNarrator-FX/
├── pom.xml
├── README.md
└── src/main/
    ├── java/
    │   ├── module-info.java
    │   └── com/narrator/App.java
    └── resources/
        └── styles.css
```

## License

MIT License

## Author

Matrix Agent
