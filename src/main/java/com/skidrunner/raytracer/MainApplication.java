package com.skidrunner.raytracer;

/*
 *  Copyright (C) 2016 SkidRunner
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.nio.ByteBuffer;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.animation.TimelineBuilder;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Duration;

/**
 * @author SkidRunner
 */
public class MainApplication extends Application {

    private static final int labelWidth = 140;
    private static final int valueWidth = 45;

    class VectorInput {
        private HBox hb;

        private double defaultX;
        private double defaultY;
        private double defaultZ;

        private TextField tfX;
        private TextField tfY;
        private TextField tfZ;

        public VectorInput(String labelText, Vector3D v3f) {
            this.defaultX = v3f.getX();
            this.defaultY = v3f.getY();
            this.defaultZ = v3f.getZ();

            Label label = new Label(labelText);
            label.setPrefWidth(labelWidth);

            tfX = new TextField(Double.toString(defaultX));
            tfY = new TextField(Double.toString(defaultY));
            tfZ = new TextField(Double.toString(defaultZ));

            tfX.setPrefWidth(valueWidth);
            tfY.setPrefWidth(valueWidth);
            tfZ.setPrefWidth(valueWidth);

            hb = new HBox();
            hb.getChildren().add(label);
            hb.getChildren().add(tfX);
            hb.getChildren().add(tfY);
            hb.getChildren().add(tfZ);
        }

        public HBox getHBox() {
            return hb;
        }

        public Vector3D getVector3f() {
            Vector3D result = null;

            try {
                double x = Double.parseDouble(tfX.getText());
                double y = Double.parseDouble(tfY.getText());
                double z = Double.parseDouble(tfZ.getText());

                result = new Vector3D(x, y, z);
            } catch (NumberFormatException nfe) {
                result = new Vector3D(defaultX, defaultY, defaultZ);
            }

            return result;
        }
    }

    private RenderConfig config = new RenderConfig();

    private GraphicsContext gc;
    private Ray raytracer;
    private WritableImage image;

    private Button btnRayTrace;
    private TextArea taPattern;

    private TextField tfRays;
    private TextField tfThreads;
    private TextField tfRenderTime;

    private TextField tfImageWidth;
    private TextField tfImageHeight;

    private VectorInput viRayOrigin;
    private VectorInput viCamDirection;
    private VectorInput viOddColour;
    private VectorInput viEvenColour;
    private VectorInput viSkyColour;

    private TextField tfSphereReflectivity;
    private TextField tfBrightness;

    private Timeline timeline;

    private String[] pattern = new String[7];

    private int canvasWidth = 512;
    private int canvasHeight = 512;
    private Canvas canvas;
    private Stage stage;

    @Override
    public void start(final Stage stage) {
        this.stage = stage;

        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            public void handle(WindowEvent arg0) {
            }
        });

        int width = 840;
        final int controlsWidth = width - canvasWidth;

        int height = canvasHeight;

        canvas = new Canvas(canvasWidth, canvasHeight);
        gc = canvas.getGraphicsContext2D();
        gc.fillRect(0, 0, canvasWidth, canvasHeight);

        config.setImageWidth(canvasWidth);
        config.setImageHeight(canvasHeight);

        stage.widthProperty().addListener(new ChangeListener<Number>() {
            public void changed(ObservableValue<? extends Number> observableValue, Number oldSceneWidth, Number newSceneWidth) {
                config.setImageWidth(newSceneWidth.intValue() - controlsWidth);

                tfImageWidth.setText(Integer.toString(config.getImageWidth()));

                canvasWidth = config.getImageWidth();

                canvas.setWidth(canvasWidth);

                gc.fillRect(0, 0, canvasWidth, canvasHeight);

            }
        });
        stage.heightProperty().addListener(new ChangeListener<Number>() {
            public void changed(ObservableValue<? extends Number> observableValue, Number oldSceneHeight, Number newSceneHeight) {
                config.setImageHeight(newSceneHeight.intValue() - 22);

                tfImageHeight.setText(Integer.toString(config.getImageHeight()));

                canvasHeight = config.getImageHeight();

                canvas.setHeight(canvasHeight);

                gc.fillRect(0, 0, canvasWidth, canvasHeight);
            }
        });

        pattern[0] = "-****--******-";
        pattern[1] = "**--**-**---**";
        pattern[2] = "**-----**---**";
        pattern[3] = "*****--**--***";
        pattern[4] = "----**-*****--";
        pattern[5] = "**--**-**-***-";
        pattern[6] = "-****--**--***";

        taPattern = new TextArea();
        taPattern.setStyle("-fx-font-family:monospace;");

        StringBuilder builder = new StringBuilder();

        for (String line : pattern) {
            builder.append(line).append("\n");
        }

        taPattern.appendText(builder.toString().trim());

        taPattern.setPrefRowCount(2 + pattern.length);

        config.setLines(pattern);

        int cores = Runtime.getRuntime().availableProcessors();
        config.setThreads(cores);

        config.setBrightness(10);
        config.setCamDirection(new Vector3D(-2, -12, 0));
        config.setEvenColour(new Vector3D(3, 1, 1));
        config.setOddColour(new Vector3D(3, 3, 3));
        config.setRayOrigin(new Vector3D(8, 18, 8));
        config.setSkyColour(new Vector3D(.4f, .4f, 1f));
        config.setSphereReflectivity(0.5f);
        config.setRays(16);

        // ==============================
        // Image size
        // ==============================
        Label lblImageSize = new Label("Image size");
        lblImageSize.setPrefWidth(labelWidth);

        tfImageWidth = new TextField(Integer.toString(config.getImageWidth()));
        tfImageWidth.setPrefWidth(valueWidth * 3 / 2);

        tfImageHeight = new TextField(Integer.toString(config.getImageHeight()));
        tfImageHeight.setPrefWidth(valueWidth * 3 / 2);

        HBox hbImageSize = new HBox();
        hbImageSize.getChildren().add(lblImageSize);
        hbImageSize.getChildren().add(tfImageWidth);
        hbImageSize.getChildren().add(tfImageHeight);

        // ==============================
        // Rendering threads
        // ==============================
        Label lblThreads = new Label("Rendering threads");
        lblThreads.setPrefWidth(labelWidth);

        tfThreads = new TextField(Integer.toString(config.getThreads()));
        tfThreads.setPrefWidth(valueWidth);

        HBox hbThreads = new HBox();
        hbThreads.getChildren().add(lblThreads);
        hbThreads.getChildren().add(tfThreads);

        // ==============================
        // Rays per pixel
        // ==============================
        Label lblRays = new Label("Rays per pixel");
        lblRays.setPrefWidth(labelWidth);

        tfRays = new TextField(Integer.toString(config.getRays()));
        tfRays.setPrefWidth(valueWidth);

        HBox hbRays = new HBox();
        hbRays.getChildren().add(lblRays);
        hbRays.getChildren().add(tfRays);

        // ==============================
        // Vector3d inputs
        // ==============================
        viRayOrigin = new VectorInput("Ray origin", config.getRayOrigin());
        viCamDirection = new VectorInput("Camera direction", config.getCamDirection());
        viOddColour = new VectorInput("Odd Colour", config.getOddColour());
        viEvenColour = new VectorInput("Even Colour", config.getEvenColour());
        viSkyColour = new VectorInput("Sky Colour", config.getSkyColour());

        // ==============================
        // Sphere ray reflectivity
        // ==============================
        Label lblReflectivity = new Label("Sphere Reflectivity");
        lblReflectivity.setPrefWidth(labelWidth);

        tfSphereReflectivity = new TextField(Float.toString(config.getSphereReflectivity()));
        tfSphereReflectivity.setPrefWidth(valueWidth);

        HBox hbReflectivity = new HBox();
        hbReflectivity.getChildren().add(lblReflectivity);
        hbReflectivity.getChildren().add(tfSphereReflectivity);

        // ==============================
        // Brightness
        // ==============================
        Label lblBrightness = new Label("Brightness");
        lblBrightness.setPrefWidth(labelWidth);

        tfBrightness = new TextField(Float.toString(config.getBrightness()));
        tfBrightness.setPrefWidth(valueWidth);

        HBox hbBrightness = new HBox();
        hbBrightness.getChildren().add(lblBrightness);
        hbBrightness.getChildren().add(tfBrightness);

        // ==============================
        // Render time
        // ==============================
        Label lblRenderTime = new Label("Render time");
        lblRenderTime.setPrefWidth(labelWidth);

        tfRenderTime = new TextField("0");
        tfRenderTime.setPrefWidth(valueWidth * 3);

        HBox hbRenderTime = new HBox();
        hbRenderTime.getChildren().add(lblRenderTime);
        hbRenderTime.getChildren().add(tfRenderTime);

        // ==============================
        // Raytrac button
        // ==============================
        btnRayTrace = new Button("RayTrace!");
        btnRayTrace.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent e) {
                stage.setResizable(false);
                startRaytracing();
            }
        });

        VBox vBoxControls = new VBox();

        vBoxControls.getChildren().add(taPattern);
        vBoxControls.getChildren().add(hbImageSize);
        vBoxControls.getChildren().add(hbThreads);
        vBoxControls.getChildren().add(hbRays);
        vBoxControls.getChildren().add(viRayOrigin.getHBox());
        vBoxControls.getChildren().add(viCamDirection.getHBox());
        vBoxControls.getChildren().add(viOddColour.getHBox());
        vBoxControls.getChildren().add(viEvenColour.getHBox());
        vBoxControls.getChildren().add(viSkyColour.getHBox());
        vBoxControls.getChildren().add(hbReflectivity);
        vBoxControls.getChildren().add(hbBrightness);
        vBoxControls.getChildren().add(hbRenderTime);
        vBoxControls.getChildren().add(btnRayTrace);

        vBoxControls.setMinWidth(controlsWidth);

        HBox box = new HBox();
        box.getChildren().add(vBoxControls);
        box.getChildren().add(canvas);

        Scene scene = new Scene(box, width, height);

        stage.setTitle("JFXRay");
        stage.setScene(scene);
        stage.show();

        int refresh = 500; // ms

        final Duration oneFrameAmt = Duration.millis(refresh);

        final KeyFrame oneFrame = new KeyFrame(oneFrameAmt, new EventHandler<ActionEvent>() {
            public void handle(ActionEvent arg0) {
                if (raytracer != null) {
                    updateCanvas();
                }
            }
        });

        timeline = TimelineBuilder.create().cycleCount(Animation.INDEFINITE).keyFrames(oneFrame).build();
    }

    private void startRaytracing() {
        btnRayTrace.setDisable(true);

        raytracer = new Ray();

        Thread t = new Thread(new Runnable() {
            public void run() {
                String patternText = taPattern.getText();
                String[] lines = patternText.split("\n");

                int maxWidth = 0;

                for (String line : lines) {
                    if (line.length() > maxWidth) {
                        maxWidth = line.length();
                    }
                }

                for (int i = 0; i < lines.length; i++) {
                    String line = lines[i];

                    if (line.length() < maxWidth) {
                        lines[i] = padLine(line, maxWidth);
                    }
                }

                config.setLines(lines);

                try {
                    config.setThreads(Integer.parseInt(tfThreads.getText()));
                    config.setRays(Integer.parseInt(tfRays.getText()));
                    config.setImageWidth(Integer.parseInt(tfImageWidth.getText()));
                    config.setImageHeight(Integer.parseInt(tfImageHeight.getText()));
                    config.setSphereReflectivity(Float.parseFloat(tfSphereReflectivity.getText()));
                    config.setBrightness(Float.parseFloat(tfBrightness.getText()));

                    config.setRayOrigin(viRayOrigin.getVector3f());
                    config.setOddColour(viOddColour.getVector3f());
                    config.setEvenColour(viEvenColour.getVector3f());
                    config.setCamDirection(viCamDirection.getVector3f());
                    config.setSkyColour(viSkyColour.getVector3f());

                    Platform.runLater(new Runnable() {
                        public void run() {
                            canvasWidth = config.getImageWidth();
                            canvasHeight = config.getImageHeight();

                            canvas.setWidth(canvasWidth);
                            canvas.setHeight(canvasHeight);
                        }
                    });

                } catch (NumberFormatException nfe) {
                }

                timeline.play();

                image = new WritableImage(config.getImageWidth(), config.getImageHeight());

                raytracer.render(config);

                Platform.runLater(new Runnable() {
                    public void run() {
                        updateCanvas();

                        timeline.stop();

                        btnRayTrace.setDisable(false);
                        stage.setResizable(true);
                    }
                });
            }
        });

        t.start();
    }

    private void updateCanvas() {
        byte[] imgData = raytracer.getImageData();

        PixelWriter pixelWriter = image.getPixelWriter();

        PixelFormat<ByteBuffer> pixelFormat = PixelFormat.getByteRgbInstance();

        // write rgb data to WritableImage
        pixelWriter.setPixels(0, 0, config.getImageWidth(), config.getImageHeight(), pixelFormat, imgData, 0,
                config.getImageWidth() * 3);

        // scale WritableImage onto Canvas
        gc.drawImage(image, 0, 0, canvasWidth, canvasHeight);

        tfRenderTime.setText(raytracer.getRenderTime() + "ms");
    }

    private String padLine(String line, int width) {
        int pad = width - line.length();

        for (int i = 0; i < pad; i++) {
            line += " ";
        }

        return line;
    }

    public static void main(String[] arguments) {
        launch(arguments);
    }
}
