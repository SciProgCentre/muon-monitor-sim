package ru.mipt.npm.muon.sim;

import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.EventHandler;
import javafx.scene.*;
import javafx.scene.control.Button;
import javafx.scene.control.ToolBar;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Using example code from https://docs.oracle.com/javase/8/javafx/graphics-tutorial/sampleapp3d.htm
 */
public class MonitorFX extends Application {

    final Group root = new Group();
    final Xform axisGroup = new Xform();
    final Xform monitor = new Xform();
    final Xform world = new Xform();
    final PerspectiveCamera camera = new PerspectiveCamera(true);
    final Xform cameraXform = new Xform();
    final Xform cameraXform2 = new Xform();
    final Xform cameraXform3 = new Xform();
    private static final double CAMERA_INITIAL_DISTANCE = -4500;
    private static final double CAMERA_INITIAL_X_ANGLE = -50;
    private static final double CAMERA_INITIAL_Y_ANGLE = 0;
    private static final double CAMERA_INITIAL_Z_ANGLE = -210;
    private static final double CAMERA_NEAR_CLIP = 0.1;
    private static final double CAMERA_FAR_CLIP = 10000.0;
    private static final double AXIS_LENGTH = 2000.0;
    private static final double CONTROL_MULTIPLIER = 0.1;
    private static final double SHIFT_MULTIPLIER = 10.0;
    private static final double MOUSE_SPEED = 0.1;
    private static final double ROTATION_SPEED = 2.0;
    private static final double TRACK_SPEED = 6.0;
    private static final double RESIZE_SPEED = 50.0;
    private static final double LINE_WIDTH = 3;

    private static final PhongMaterial redMaterial = new PhongMaterial();

    private static final PhongMaterial whiteMaterial = new PhongMaterial();

    private static final PhongMaterial greyMaterial = new PhongMaterial();

    static {
        redMaterial.setDiffuseColor(Color.DARKRED);
        redMaterial.setSpecularColor(Color.RED);

        whiteMaterial.setDiffuseColor(Color.WHITE);
        whiteMaterial.setSpecularColor(Color.LIGHTBLUE);

        greyMaterial.setDiffuseColor(Color.DARKGREY);
        greyMaterial.setSpecularColor(Color.GREY);
    }

    private Map<Event, Xform> events = new HashMap<>();

    double mousePosX;
    double mousePosY;
    double mouseOldX;
    double mouseOldY;
    double mouseDeltaX;
    double mouseDeltaY;

    private Map<String, Box> pixelMap = new HashMap<>();

    private Simulation sim = new Simulation();


    private void buildCamera() {
        System.out.println("buildCamera()");
        root.getChildren().add(cameraXform);
        cameraXform.getChildren().add(cameraXform2);
        cameraXform2.getChildren().add(cameraXform3);
        cameraXform3.getChildren().add(camera);
        cameraXform3.setRotateZ(180.0);

        camera.setNearClip(CAMERA_NEAR_CLIP);
        camera.setFarClip(CAMERA_FAR_CLIP);
        camera.setTranslateZ(CAMERA_INITIAL_DISTANCE);
        cameraXform.ry.setAngle(CAMERA_INITIAL_Y_ANGLE);
        cameraXform.rx.setAngle(CAMERA_INITIAL_X_ANGLE);
        cameraXform.rz.setAngle(CAMERA_INITIAL_Z_ANGLE);
    }

    private void buildAxes() {
        System.out.println("buildAxes()");
        final PhongMaterial redMaterial = new PhongMaterial();
        redMaterial.setDiffuseColor(Color.DARKRED);
        redMaterial.setSpecularColor(Color.RED);

        final PhongMaterial greenMaterial = new PhongMaterial();
        greenMaterial.setDiffuseColor(Color.DARKGREEN);
        greenMaterial.setSpecularColor(Color.GREEN);

        final PhongMaterial blueMaterial = new PhongMaterial();
        blueMaterial.setDiffuseColor(Color.DARKBLUE);
        blueMaterial.setSpecularColor(Color.BLUE);

        final Box xAxis = new Box(AXIS_LENGTH, LINE_WIDTH, LINE_WIDTH);
        final Box yAxis = new Box(LINE_WIDTH, AXIS_LENGTH, LINE_WIDTH);
        final Box zAxis = new Box(LINE_WIDTH, LINE_WIDTH, AXIS_LENGTH);

        xAxis.setMaterial(redMaterial);
        yAxis.setMaterial(greenMaterial);
        zAxis.setMaterial(blueMaterial);

        axisGroup.getChildren().addAll(xAxis, yAxis, zAxis);
        axisGroup.setVisible(false);
        world.getChildren().addAll(axisGroup);
    }

    private Xform drawTrack(Track track) {
        Xform line = new Xform(Xform.RotateOrder.YZX);
        Box lineBox = new Box(AXIS_LENGTH, LINE_WIDTH, LINE_WIDTH);
        lineBox.setMaterial(redMaterial);
        line.getChildren().add(lineBox);
        line.setRy(-track.getTheta() * 180.0 / Math.PI);
        line.setRz(-track.getPhi() * 180.0 / Math.PI);
        line.setTranslate(-track.getX(), track.getY());
        return line;
    }

    /**
     * Display event
     *
     * @param event
     */
    public void displayEvent(Event event) {
        Xform line = drawTrack(event.getTrack());
        this.world.getChildren().add(line);
        for (Pixel p : event.getHits()) {
            this.setPixelActive(p.getName(), true);
        }
        this.events.put(event, line);
    }

    /**
     * clear all displayed events
     */
    public void clearEvents() {
        events.forEach((key, value) -> {
            this.world.getChildren().remove(value);
            key.getHits().forEach(h -> setPixelActive(h.getName(), false));
        });
    }

    private void handleMouse(SubScene scene, final Node root) {
        scene.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent me) {
                mousePosX = me.getSceneX();
                mousePosY = me.getSceneY();
                mouseOldX = me.getSceneX();
                mouseOldY = me.getSceneY();
            }
        });
        scene.setOnMouseDragged(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent me) {
                mouseOldX = mousePosX;
                mouseOldY = mousePosY;
                mousePosX = me.getSceneX();
                mousePosY = me.getSceneY();
                mouseDeltaX = (mousePosX - mouseOldX);
                mouseDeltaY = (mousePosY - mouseOldY);

                double modifier = 1.0;

                if (me.isControlDown()) {
                    modifier = CONTROL_MULTIPLIER;
                }
                if (me.isShiftDown()) {
                    modifier = SHIFT_MULTIPLIER;
                }
                if (me.isPrimaryButtonDown()) {
                    cameraXform.rz.setAngle(cameraXform.rz.getAngle() + mouseDeltaX * MOUSE_SPEED * modifier * ROTATION_SPEED);
                    cameraXform.rx.setAngle(cameraXform.rx.getAngle() + mouseDeltaY * MOUSE_SPEED * modifier * ROTATION_SPEED);
//                } else if (me.isSecondaryButtonDown()) {
//                    double z = camera.getTranslateZ();
//                    double newZ = z + mouseDeltaX * MOUSE_SPEED * modifier*100;
//                    camera.setTranslateZ(newZ);
                } else if (me.isSecondaryButtonDown()) {
                    cameraXform2.t.setX(cameraXform2.t.getX() + mouseDeltaX * MOUSE_SPEED * modifier * TRACK_SPEED);
                    cameraXform2.t.setY(cameraXform2.t.getY() + mouseDeltaY * MOUSE_SPEED * modifier * TRACK_SPEED);
                }
            }
        });
        scene.setOnScroll(new EventHandler<ScrollEvent>() {
            @Override
            public void handle(ScrollEvent event) {
                double z = camera.getTranslateZ();
                double newZ = z + MOUSE_SPEED * event.getDeltaY() * RESIZE_SPEED;
                camera.setTranslateZ(newZ);
            }
        });
    }

    private void handleKeyboard(Scene scene, final Node root) {
        scene.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                switch (event.getCode()) {
                    case Z:
                        cameraXform2.t.setX(0.0);
                        cameraXform2.t.setY(0.0);
                        camera.setTranslateZ(CAMERA_INITIAL_DISTANCE);
                        cameraXform.ry.setAngle(CAMERA_INITIAL_Y_ANGLE);
                        cameraXform.rx.setAngle(CAMERA_INITIAL_X_ANGLE);
                        break;
                    case X:
                        axisGroup.setVisible(!axisGroup.isVisible());
                        break;
                    case V:
                        monitor.setVisible(!monitor.isVisible());
                        break;
                    case S:
                        snapshot(scene);
                }
            }
        });
    }

    private Collection<Pixel> getPixels() {
        return MonitorKt.getPixels().values();
    }

    private void buildContent() {
        System.out.println("buildContent()");
        Xform monitorXform = new Xform();

        for (Pixel p : getPixels()) {
            //Slightly reducing pixel sizes to see gaps
            Box pixelBox = new Box(p.getXSize() - 0.1, p.getYSize() - 0.1, p.getZSize() - 0.1);
            pixelBox.setMaterial(whiteMaterial);
            pixelBox.setTranslateX(-p.getCenter().getX());
            pixelBox.setTranslateY(p.getCenter().getY());
            pixelBox.setTranslateZ(-p.getCenter().getZ());

            pixelMap.put(p.getName(), pixelBox);

            Text cap = new Text(p.getName());
            cap.setStyle("-fx-font-size:20;-fx-font-weight: bold");
//            cap.setFill(Color.WHITE);
            cap.setTranslateX(pixelBox.getTranslateX() - p.getXSize() / 4);
            cap.setTranslateY(pixelBox.getTranslateY());
            //Text is floating over surface for better rendering
            cap.setTranslateZ(pixelBox.getTranslateZ() - p.getZSize() / 2 - 2);

            monitorXform.getChildren().addAll(pixelBox, cap);
        }

        monitor.getChildren().add(monitorXform);

        world.getChildren().addAll(monitorXform);
    }

    private void setPixelActive(String pixelName, boolean active) {
        Box pixelBox = pixelMap.get(pixelName);
        if (pixelBox != null) {
            if (active) {
                pixelBox.setMaterial(redMaterial);
            } else {
                pixelBox.setMaterial(whiteMaterial);
            }
        }
    }

    private ToolBar buildToolbar() {

        Button simulateButton = new Button("Generate");
        simulateButton.setOnAction(event -> displayEvent(sim.simulateOne()));

        Button clearButton = new Button("Clear");
        clearButton.setOnAction(event -> clearEvents());


        return new ToolBar(simulateButton, clearButton);
    }

    private void snapshot(Scene scene) {
        scene.snapshot(res -> {
            FileChooser chooser = new FileChooser();
            chooser.setInitialFileName("snapshot_" + System.currentTimeMillis() + ".png");
            chooser.setTitle("Select output file");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG", "*.png"));
            File file = chooser.showSaveDialog(scene.getWindow());
            try {
                ImageIO.write(SwingFXUtils.fromFXImage(res.getImage(), null), "png", file);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }, null);
    }

    @Override
    public void start(Stage primaryStage) {

        // setUserAgentStylesheet(STYLESHEET_MODENA);
        System.out.println("start()");
        root.getChildren().add(world);
        root.setDepthTest(DepthTest.ENABLE);

        buildCamera();
        buildAxes();
        buildContent();

        this.monitor.setCache(true);
        this.monitor.setCacheHint(CacheHint.ROTATE);


        SubScene canvas = new SubScene(root, 1024, 768, true, SceneAntialiasing.BALANCED);
        canvas.setFill(Color.GREY);

        handleMouse(canvas, world);


        Scene scene = new Scene(new VBox(buildToolbar(), canvas), 1024, 768, false);

        handleKeyboard(scene, world);

        primaryStage.setTitle("Muon monitor demonstration");
        primaryStage.setScene(scene);
        primaryStage.show();

        canvas.setCamera(camera);
    }

    /**
     * The main() method is ignored in correctly deployed JavaFX application.
     * main() serves only as fallback in case the application can not be
     * launched through deployment artifacts, e.g., in IDEs with limited FX
     * support. NetBeans ignores main().
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

}
