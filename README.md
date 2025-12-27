# JNarrator FX

A Text-to-Speech narrator application built with JavaFX featuring a modern dark theme UI.

## Features

- **Text Input**: Large text area for typing or pasting content
- **File Support**: Open `.txt` and `.md` files
- **Voice Controls**: Adjustable speed (0.5x - 2.0x) and volume
- **Audio Export**: Save narration as WAV audio file
- **Playback Controls**: Play, Pause, and Stop functionality
- **Cross-Platform TTS**: Uses native system speech synthesis
  - Windows: SAPI via PowerShell
  - macOS: `say` command
  - Linux: `espeak`

## Screenshots

The application features a clean dark theme with:
- Menu bar (File, Edit, Help)
- Central text input area
- Right sidebar with voice settings
- Bottom control bar with playback buttons

## Requirements

- Java 17 or higher
- Maven 3.6+
- JavaFX 21 (automatically downloaded by Maven)
- Linux only: `espeak` package (`sudo apt install espeak`)

## Build & Run

### Using Maven

```bash
# Clone and navigate to project
cd jnarrator-fx

# Run directly
mvn clean javafx:run

# Or build JAR
mvn clean package
```

### Running the JAR

```bash
# Set JavaFX path and run
java --module-path /path/to/javafx-sdk/lib \
     --add-modules javafx.controls,javafx.fxml,javafx.media \
     -jar target/jnarrator-fx-1.0.0.jar
```

## Usage

1. **Enter Text**: Type or paste text in the main area
2. **Adjust Settings**: Use the right panel to set speed and volume
3. **Play**: Click the Play button to start narration
4. **Export**: Use File > Export to WAV to save audio

## Project Structure

```
jnarrator-fx/
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
