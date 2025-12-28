package com.narrator;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.nio.file.Files;

public class App extends Application {

    private TextArea textArea;
    private ComboBox<String> voiceSelector;
    private ComboBox<String> fontFamilySelector;
    private Slider fontSizeSlider;
    private Slider speedSlider;
    private Slider volumeSlider;
    private Button playButton;
    private Button stopButton;
    private Label statusLabel;
    private ProgressBar progressBar;
    
    private Process ttsProcess;
    private volatile boolean isSpeaking = false;

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
        root.setRight(createSettingsPanel());

        // Bottom - Controls
        root.setBottom(createControlBar());

        Scene scene = new Scene(root, 900, 600);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(700);
        primaryStage.setMinHeight(500);
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
        MenuItem aboutItem = new MenuItem("About");
        aboutItem.setOnAction(e -> showAbout());
        helpMenu.getItems().add(aboutItem);

        menuBar.getMenus().addAll(fileMenu, editMenu, helpMenu);
        return menuBar;
    }

    private VBox createSettingsPanel() {
        VBox settingsPanel = new VBox(15);
        settingsPanel.setPadding(new Insets(15));
        settingsPanel.setPrefWidth(240);
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

        // Voice Selection
        Label voiceLabel = new Label("Voice:");
        voiceSelector = new ComboBox<>();
        voiceSelector.getItems().addAll("Default", "Male", "Female");
        voiceSelector.setValue("Default");
        voiceSelector.setMaxWidth(Double.MAX_VALUE);

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
            voiceLabel, voiceSelector,
            speedLabel, speedSlider,
            volumeLabel, volumeSlider
        );

        return settingsPanel;
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
        progressBar.setProgress(-1); // Indeterminate

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
        String os = System.getProperty("os.name").toLowerCase();
        ProcessBuilder pb;
        
        int rate = (int) ((speedSlider.getValue() - 1.0) * 10); // Convert to espeak/say rate
        int volume = (int) volumeSlider.getValue();

        if (os.contains("win")) {
            // Windows - Use PowerShell with SAPI
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
            // macOS - Use 'say' command
            int macRate = (int) (150 * speedSlider.getValue());
            pb = new ProcessBuilder("say", "-r", String.valueOf(macRate), text);
        } else {
            // Linux - Use espeak
            int espeakSpeed = (int) (150 * speedSlider.getValue());
            pb = new ProcessBuilder("espeak", "-s", String.valueOf(espeakSpeed), 
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
                        statusLabel.setText("Export failed");
                        progressBar.setProgress(0);
                    });
                }
            };

            new Thread(exportTask).start();
        }
    }

    private void exportToWav(String text, File outputFile) throws Exception {
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
            pb = new ProcessBuilder("espeak", "-w", outputFile.getAbsolutePath(), text);
        }

        Process p = pb.start();
        p.waitFor();
    }

    private void showAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About JNarrator FX");
        alert.setHeaderText("JNarrator FX v1.0");
        alert.setContentText("A Text-to-Speech narrator application.\nBuilt with JavaFX.\n\nAuthor: Matrix Agent");
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
