package com.narrator;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class App extends Application {

    private TextArea textArea;
    private ComboBox<String> engineSelector;
    private ComboBox<String> voiceSelector;
    private ComboBox<String> fontFamilySelector;
    private Slider fontSizeSlider;
    private Slider speedSlider;
    private Slider volumeSlider;
    private Button playButton;
    private Button stopButton;
    private Label statusLabel;
    private ProgressBar progressBar;
    private TextField piperPathField;
    
    private Process ttsProcess;
    private volatile boolean isSpeaking = false;
    
    // Piper voice models (name -> download URL)
    private static final String[][] PIPER_VOICES = {
        {"Amy (US Female)", "en_US-amy-medium", "https://huggingface.co/rhasspy/piper-voices/resolve/main/en/en_US/amy/medium/en_US-amy-medium.onnx"},
        {"Ryan (US Male)", "en_US-ryan-medium", "https://huggingface.co/rhasspy/piper-voices/resolve/main/en/en_US/ryan/medium/en_US-ryan-medium.onnx"},
        {"Lessac (US Female)", "en_US-lessac-medium", "https://huggingface.co/rhasspy/piper-voices/resolve/main/en/en_US/lessac/medium/en_US-lessac-medium.onnx"},
        {"Jenny (UK Female)", "en_GB-jenny_dioco-medium", "https://huggingface.co/rhasspy/piper-voices/resolve/main/en/en_GB/jenny_dioco/medium/en_GB-jenny_dioco-medium.onnx"},
        {"Alan (UK Male)", "en_GB-alan-medium", "https://huggingface.co/rhasspy/piper-voices/resolve/main/en/en_GB/alan/medium/en_GB-alan-medium.onnx"},
    };

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("JNarrator FX - Text to Speech");

        BorderPane root = new BorderPane();
        root.getStyleClass().add("root-pane");

        // Menu Bar
        root.setTop(createMenuBar(primaryStage));

        // Center - Text Area
        textArea = new TextArea();
        textArea.setPromptText("Enter or paste text here to narrate...");
        textArea.setWrapText(true);
        textArea.getStyleClass().add("text-input");
        VBox centerBox = new VBox(10, new Label("Text to Narrate:"), textArea);
        centerBox.setPadding(new Insets(15));
        VBox.setVgrow(textArea, Priority.ALWAYS);
        root.setCenter(centerBox);

        // Right - Settings Panel
        root.setRight(createSettingsPanel(primaryStage));

        // Bottom - Controls
        root.setBottom(createControlBar());

        Scene scene = new Scene(root, 950, 650);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(550);
        primaryStage.show();
    }

    private MenuBar createMenuBar(Stage stage) {
        MenuBar menuBar = new MenuBar();
        
        // File Menu
        Menu fileMenu = new Menu("File");
        MenuItem openItem = new MenuItem("Open Text File...");
        openItem.setOnAction(e -> openFile(stage));
        MenuItem exportItem = new MenuItem("Export to WAV...");
        exportItem.setOnAction(e -> exportAudio(stage));
        MenuItem exitItem = new MenuItem("Exit");
        exitItem.setOnAction(e -> Platform.exit());
        fileMenu.getItems().addAll(openItem, exportItem, new SeparatorMenuItem(), exitItem);

        // Edit Menu
        Menu editMenu = new Menu("Edit");
        MenuItem clearItem = new MenuItem("Clear Text");
        clearItem.setOnAction(e -> textArea.clear());
        MenuItem pasteItem = new MenuItem("Paste");
        pasteItem.setOnAction(e -> textArea.paste());
        editMenu.getItems().addAll(clearItem, pasteItem);

        // Help Menu
        Menu helpMenu = new Menu("Help");
        MenuItem setupPiperItem = new MenuItem("Setup Piper TTS...");
        setupPiperItem.setOnAction(e -> showPiperSetupDialog());
        MenuItem aboutItem = new MenuItem("About");
        aboutItem.setOnAction(e -> showAbout());
        helpMenu.getItems().addAll(setupPiperItem, new SeparatorMenuItem(), aboutItem);

        menuBar.getMenus().addAll(fileMenu, editMenu, helpMenu);
        return menuBar;
    }

    private VBox createSettingsPanel(Stage stage) {
        VBox settingsPanel = new VBox(12);
        settingsPanel.setPadding(new Insets(15));
        settingsPanel.setPrefWidth(260);
        settingsPanel.getStyleClass().add("settings-panel");

        // Font Settings Section
        Label fontSectionLabel = new Label("Font Settings");
        fontSectionLabel.setStyle("-fx-font-weight: bold;");

        // Font Family
        Label fontFamilyLabel = new Label("Font Family:");
        fontFamilySelector = new ComboBox<>();
        fontFamilySelector.getItems().addAll(
            "Consolas", "Courier New", "Monospace", 
            "Arial", "Verdana", "Tahoma", "Georgia", 
            "Times New Roman", "Segoe UI", "System"
        );
        fontFamilySelector.setValue("Consolas");
        fontFamilySelector.setMaxWidth(Double.MAX_VALUE);
        fontFamilySelector.setOnAction(e -> updateTextAreaFont());

        // Font Size
        Label fontSizeLabel = new Label("Font Size: 16px");
        fontSizeSlider = new Slider(10, 32, 16);
        fontSizeSlider.setShowTickLabels(true);
        fontSizeSlider.setShowTickMarks(true);
        fontSizeSlider.setMajorTickUnit(6);
        fontSizeSlider.setBlockIncrement(2);
        fontSizeSlider.valueProperty().addListener((obs, old, val) -> {
            fontSizeLabel.setText(String.format("Font Size: %.0fpx", val.doubleValue()));
            updateTextAreaFont();
        });

        // Voice Settings Section
        Label voiceSectionLabel = new Label("Voice Settings");
        voiceSectionLabel.setStyle("-fx-font-weight: bold;");

        // TTS Engine Selection
        Label engineLabel = new Label("TTS Engine:");
        engineSelector = new ComboBox<>();
        engineSelector.getItems().addAll("Piper (Neural - Best)", "System Default");
        engineSelector.setValue("Piper (Neural - Best)");
        engineSelector.setMaxWidth(Double.MAX_VALUE);
        engineSelector.setOnAction(e -> updateVoiceOptions());

        // Voice Selection
        Label voiceLabel = new Label("Voice:");
        voiceSelector = new ComboBox<>();
        voiceSelector.setMaxWidth(Double.MAX_VALUE);
        updateVoiceOptions();

        // Piper Path
        Label piperPathLabel = new Label("Piper Path:");
        piperPathField = new TextField();
        piperPathField.setPromptText("Path to piper executable");
        piperPathField.setText(getDefaultPiperPath());
        
        Button browsePiperBtn = new Button("...");
        browsePiperBtn.setOnAction(e -> browsePiperPath(stage));
        HBox piperPathBox = new HBox(5, piperPathField, browsePiperBtn);
        HBox.setHgrow(piperPathField, Priority.ALWAYS);

        // Download voice button
        Button downloadVoiceBtn = new Button("Download Voice");
        downloadVoiceBtn.setMaxWidth(Double.MAX_VALUE);
        downloadVoiceBtn.setOnAction(e -> downloadSelectedVoice());

        // Speed Control
        Label speedLabel = new Label("Speed: 1.0x");
        speedSlider = new Slider(0.5, 2.0, 1.0);
        speedSlider.setShowTickLabels(true);
        speedSlider.setShowTickMarks(true);
        speedSlider.setMajorTickUnit(0.5);
        speedSlider.valueProperty().addListener((obs, old, val) -> 
            speedLabel.setText(String.format("Speed: %.1fx", val.doubleValue())));

        // Volume Control
        Label volumeLabel = new Label("Volume: 100%");
        volumeSlider = new Slider(0, 100, 100);
        volumeSlider.setShowTickLabels(true);
        volumeSlider.setMajorTickUnit(25);
        volumeSlider.valueProperty().addListener((obs, old, val) -> 
            volumeLabel.setText(String.format("Volume: %.0f%%", val.doubleValue())));

        settingsPanel.getChildren().addAll(
            fontSectionLabel, new Separator(),
            fontFamilyLabel, fontFamilySelector,
            fontSizeLabel, fontSizeSlider,
            new Separator(),
            voiceSectionLabel, new Separator(),
            engineLabel, engineSelector,
            voiceLabel, voiceSelector,
            piperPathLabel, piperPathBox,
            downloadVoiceBtn,
            new Separator(),
            speedLabel, speedSlider,
            volumeLabel, volumeSlider
        );

        return settingsPanel;
    }

    private void updateVoiceOptions() {
        voiceSelector.getItems().clear();
        if (engineSelector.getValue().contains("Piper")) {
            for (String[] voice : PIPER_VOICES) {
                voiceSelector.getItems().add(voice[0]);
            }
            voiceSelector.setValue(PIPER_VOICES[0][0]);
        } else {
            voiceSelector.getItems().addAll("Default", "Male", "Female");
            voiceSelector.setValue("Default");
        }
    }

    private String getDefaultPiperPath() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return System.getProperty("user.home") + "\\piper\\piper.exe";
        } else {
            return System.getProperty("user.home") + "/piper/piper";
        }
    }

    private void browsePiperPath(Stage stage) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select Piper Executable");
        File file = fc.showOpenDialog(stage);
        if (file != null) {
            piperPathField.setText(file.getAbsolutePath());
        }
    }

    private void downloadSelectedVoice() {
        String selectedVoice = voiceSelector.getValue();
        if (selectedVoice == null || !engineSelector.getValue().contains("Piper")) {
            showAlert("Select a Piper voice first");
            return;
        }

        String[] voiceInfo = null;
        for (String[] v : PIPER_VOICES) {
            if (v[0].equals(selectedVoice)) {
                voiceInfo = v;
                break;
            }
        }

        if (voiceInfo == null) return;

        final String modelName = voiceInfo[1];
        final String modelUrl = voiceInfo[2];
        final String jsonUrl = modelUrl + ".json";

        statusLabel.setText("Downloading voice...");
        progressBar.setProgress(-1);

        Task<Void> downloadTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                Path voicesDir = Path.of(System.getProperty("user.home"), "piper", "voices");
                Files.createDirectories(voicesDir);

                // Download model
                Path modelPath = voicesDir.resolve(modelName + ".onnx");
                downloadFile(modelUrl, modelPath);

                // Download config
                Path jsonPath = voicesDir.resolve(modelName + ".onnx.json");
                downloadFile(jsonUrl, jsonPath);

                return null;
            }

            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    statusLabel.setText("Voice downloaded: " + modelName);
                    progressBar.setProgress(1);
                });
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    statusLabel.setText("Download failed: " + getException().getMessage());
                    progressBar.setProgress(0);
                });
            }
        };

        new Thread(downloadTask).start();
    }

    private void downloadFile(String urlStr, Path destination) throws IOException {
        URL url = new URL(urlStr);
        try (InputStream in = url.openStream()) {
            Files.copy(in, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void updateTextAreaFont() {
        String fontFamily = fontFamilySelector.getValue();
        int fontSize = (int) fontSizeSlider.getValue();
        textArea.setStyle(String.format(
            "-fx-font-family: '%s'; -fx-font-size: %dpx; -fx-text-fill: #FFFFFF;",
            fontFamily, fontSize
        ));
    }

    private HBox createControlBar() {
        HBox controlBar = new HBox(15);
        controlBar.setPadding(new Insets(15));
        controlBar.setAlignment(Pos.CENTER);
        controlBar.getStyleClass().add("control-bar");

        playButton = new Button("Play");
        playButton.getStyleClass().add("play-button");
        playButton.setPrefWidth(100);
        playButton.setOnAction(e -> togglePlay());

        stopButton = new Button("Stop");
        stopButton.getStyleClass().add("stop-button");
        stopButton.setPrefWidth(100);
        stopButton.setDisable(true);
        stopButton.setOnAction(e -> stopSpeaking());

        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(200);
        HBox.setHgrow(progressBar, Priority.ALWAYS);

        statusLabel = new Label("Ready");
        statusLabel.getStyleClass().add("status-label");

        controlBar.getChildren().addAll(playButton, stopButton, progressBar, statusLabel);
        return controlBar;
    }

    private void togglePlay() {
        if (isSpeaking) {
            stopSpeaking();
        } else {
            startSpeaking();
        }
    }

    private void startSpeaking() {
        String text = textArea.getText().trim();
        if (text.isEmpty()) {
            statusLabel.setText("No text to speak");
            return;
        }

        isSpeaking = true;
        playButton.setText("Pause");
        stopButton.setDisable(false);
        statusLabel.setText("Speaking...");
        progressBar.setProgress(-1);

        Task<Void> speakTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                speakText(text);
                return null;
            }

            @Override
            protected void succeeded() {
                resetControls();
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    statusLabel.setText("Error: " + getException().getMessage());
                    resetControls();
                });
            }
        };

        new Thread(speakTask).start();
    }

    private void speakText(String text) throws Exception {
        if (engineSelector.getValue().contains("Piper")) {
            speakWithPiper(text);
        } else {
            speakWithSystem(text);
        }
    }

    private void speakWithPiper(String text) throws Exception {
        String piperPath = piperPathField.getText();
        File piperExe = new File(piperPath);
        
        if (!piperExe.exists()) {
            throw new Exception("Piper not found. Please install Piper TTS.");
        }

        // Get selected voice model
        String selectedVoice = voiceSelector.getValue();
        String modelName = null;
        for (String[] v : PIPER_VOICES) {
            if (v[0].equals(selectedVoice)) {
                modelName = v[1];
                break;
            }
        }

        Path voicesDir = Path.of(System.getProperty("user.home"), "piper", "voices");
        Path modelPath = voicesDir.resolve(modelName + ".onnx");
        
        if (!Files.exists(modelPath)) {
            throw new Exception("Voice model not found. Click 'Download Voice' first.");
        }

        // Create temp file for audio output
        File tempWav = File.createTempFile("piper_", ".wav");
        tempWav.deleteOnExit();

        // Build piper command
        ProcessBuilder pb = new ProcessBuilder(
            piperPath,
            "--model", modelPath.toString(),
            "--output_file", tempWav.getAbsolutePath()
        );
        pb.redirectErrorStream(true);

        ttsProcess = pb.start();

        // Write text to piper's stdin
        try (OutputStream os = ttsProcess.getOutputStream()) {
            os.write(text.getBytes());
            os.flush();
        }

        ttsProcess.waitFor();

        // Play the generated WAV file
        playWavFile(tempWav);
    }

    private void playWavFile(File wavFile) throws Exception {
        String os = System.getProperty("os.name").toLowerCase();
        ProcessBuilder pb;

        if (os.contains("win")) {
            // Windows Media Player or PowerShell
            pb = new ProcessBuilder("powershell", "-Command",
                String.format("(New-Object Media.SoundPlayer '%s').PlaySync()", 
                    wavFile.getAbsolutePath()));
        } else if (os.contains("mac")) {
            pb = new ProcessBuilder("afplay", wavFile.getAbsolutePath());
        } else {
            // Linux - try aplay, paplay, or ffplay
            pb = new ProcessBuilder("aplay", wavFile.getAbsolutePath());
        }

        Process playProcess = pb.start();
        playProcess.waitFor();
    }

    private void speakWithSystem(String text) throws Exception {
        String os = System.getProperty("os.name").toLowerCase();
        ProcessBuilder pb;
        
        int rate = (int) ((speedSlider.getValue() - 1.0) * 10);
        int volume = (int) volumeSlider.getValue();

        if (os.contains("win")) {
            String script = String.format(
                "Add-Type -AssemblyName System.Speech; " +
                "$synth = New-Object System.Speech.Synthesis.SpeechSynthesizer; " +
                "$synth.Rate = %d; " +
                "$synth.Volume = %d; " +
                "$synth.Speak('%s')",
                rate, volume, text.replace("'", "''").replace("\n", " ")
            );
            pb = new ProcessBuilder("powershell", "-Command", script);
        } else if (os.contains("mac")) {
            int macRate = (int) (175 * speedSlider.getValue());
            String voice = voiceSelector.getValue().equals("Female") ? "Samantha" : "Alex";
            pb = new ProcessBuilder("say", "-v", voice, "-r", String.valueOf(macRate), text);
        } else {
            // Linux - try espeak-ng first (better than espeak)
            int espeakSpeed = (int) (160 * speedSlider.getValue());
            pb = new ProcessBuilder("espeak-ng", "-s", String.valueOf(espeakSpeed), 
                                   "-a", String.valueOf(volume * 2), text);
        }

        ttsProcess = pb.start();
        ttsProcess.waitFor();
    }

    private void stopSpeaking() {
        if (ttsProcess != null && ttsProcess.isAlive()) {
            ttsProcess.destroyForcibly();
        }
        resetControls();
    }

    private void resetControls() {
        Platform.runLater(() -> {
            isSpeaking = false;
            playButton.setText("Play");
            stopButton.setDisable(true);
            statusLabel.setText("Ready");
            progressBar.setProgress(0);
        });
    }

    private void openFile(Stage stage) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Open Text File");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files", "*.txt", "*.md"));
        File file = fc.showOpenDialog(stage);
        if (file != null) {
            try {
                String content = Files.readString(file.toPath());
                textArea.setText(content);
                statusLabel.setText("Loaded: " + file.getName());
            } catch (IOException e) {
                statusLabel.setText("Error loading file");
            }
        }
    }

    private void exportAudio(Stage stage) {
        String text = textArea.getText().trim();
        if (text.isEmpty()) {
            statusLabel.setText("No text to export");
            return;
        }

        FileChooser fc = new FileChooser();
        fc.setTitle("Export Audio");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("WAV Audio", "*.wav"));
        fc.setInitialFileName("narration.wav");
        File file = fc.showSaveDialog(stage);
        
        if (file != null) {
            statusLabel.setText("Exporting...");
            progressBar.setProgress(-1);

            Task<Void> exportTask = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    exportToWav(text, file);
                    return null;
                }

                @Override
                protected void succeeded() {
                    Platform.runLater(() -> {
                        statusLabel.setText("Exported: " + file.getName());
                        progressBar.setProgress(1);
                    });
                }

                @Override
                protected void failed() {
                    Platform.runLater(() -> {
                        statusLabel.setText("Export failed: " + getException().getMessage());
                        progressBar.setProgress(0);
                    });
                }
            };

            new Thread(exportTask).start();
        }
    }

    private void exportToWav(String text, File outputFile) throws Exception {
        if (engineSelector.getValue().contains("Piper")) {
            exportWithPiper(text, outputFile);
        } else {
            exportWithSystem(text, outputFile);
        }
    }

    private void exportWithPiper(String text, File outputFile) throws Exception {
        String piperPath = piperPathField.getText();
        File piperExe = new File(piperPath);
        
        if (!piperExe.exists()) {
            throw new Exception("Piper not found");
        }

        String selectedVoice = voiceSelector.getValue();
        String modelName = null;
        for (String[] v : PIPER_VOICES) {
            if (v[0].equals(selectedVoice)) {
                modelName = v[1];
                break;
            }
        }

        Path voicesDir = Path.of(System.getProperty("user.home"), "piper", "voices");
        Path modelPath = voicesDir.resolve(modelName + ".onnx");

        ProcessBuilder pb = new ProcessBuilder(
            piperPath,
            "--model", modelPath.toString(),
            "--output_file", outputFile.getAbsolutePath()
        );

        Process p = pb.start();
        try (OutputStream os = p.getOutputStream()) {
            os.write(text.getBytes());
            os.flush();
        }
        p.waitFor();
    }

    private void exportWithSystem(String text, File outputFile) throws Exception {
        String os = System.getProperty("os.name").toLowerCase();
        ProcessBuilder pb;

        if (os.contains("win")) {
            String script = String.format(
                "Add-Type -AssemblyName System.Speech; " +
                "$synth = New-Object System.Speech.Synthesis.SpeechSynthesizer; " +
                "$synth.SetOutputToWaveFile('%s'); " +
                "$synth.Speak('%s'); " +
                "$synth.Dispose()",
                outputFile.getAbsolutePath().replace("\\", "\\\\"),
                text.replace("'", "''").replace("\n", " ")
            );
            pb = new ProcessBuilder("powershell", "-Command", script);
        } else if (os.contains("mac")) {
            pb = new ProcessBuilder("say", "-o", outputFile.getAbsolutePath(), "--data-format=LEF32@22050", text);
        } else {
            pb = new ProcessBuilder("espeak-ng", "-w", outputFile.getAbsolutePath(), text);
        }

        Process p = pb.start();
        p.waitFor();
    }

    private void showPiperSetupDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Setup Piper TTS");
        alert.setHeaderText("How to Install Piper TTS");
        alert.setContentText(
            "Piper is a fast, local neural text-to-speech system.\n\n" +
            "1. Download Piper from:\n" +
            "   https://github.com/rhasspy/piper/releases\n\n" +
            "2. Extract to ~/piper/ (or any folder)\n\n" +
            "3. Set the path in the settings panel\n\n" +
            "4. Click 'Download Voice' to get a voice model\n\n" +
            "5. Select Piper engine and enjoy natural voices!"
        );
        alert.getDialogPane().setMinWidth(500);
        alert.showAndWait();
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Warning");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About JNarrator FX");
        alert.setHeaderText("JNarrator FX v2.0");
        alert.setContentText(
            "A Text-to-Speech narrator application.\n" +
            "Built with JavaFX.\n\n" +
            "Supports:\n" +
            "- Piper TTS (Neural voices)\n" +
            "- System TTS (Windows/macOS/Linux)\n\n" +
            "Author: Matrix Agent"
        );
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
