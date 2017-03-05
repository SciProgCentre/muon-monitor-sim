package ru.mipt.npm.muon.sim.app

import javafx.collections.FXCollections
import javafx.embed.swing.SwingFXUtils
import javafx.event.EventHandler
import javafx.geometry.Orientation
import javafx.scene.*
import javafx.scene.control.TableView
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseEvent
import javafx.scene.input.ScrollEvent
import javafx.scene.paint.Color
import javafx.scene.paint.PhongMaterial
import javafx.scene.shape.Box
import javafx.scene.text.Text
import javafx.stage.FileChooser
import ru.mipt.npm.muon.sim.*
import tornadofx.*
import java.io.IOException
import java.util.*
import javax.imageio.ImageIO

/**
 * Using example code from https://docs.oracle.com/javase/8/javafx/graphics-tutorial/sampleapp3d.htm
 */
class MonitorView : View() {

    private class EventDisplay(val event: Event, val xform: Xform) {
        val name: String get() = event.toString()
        val theta: Double get() = event.track.getTheta()
        val phi: Double get() = event.track.getPhi()
    }

    private val axisGroup = Xform()
    private val monitor = Xform()
    private val world = Xform()
    private val camera = PerspectiveCamera(true)
    private val cameraXform = Xform()
    private val cameraXform2 = Xform()
    private val cameraXform3 = Xform()

    private val events = FXCollections.observableArrayList<EventDisplay>()

    private var mousePosX: Double = 0.toDouble()
    private var mousePosY: Double = 0.toDouble()
    private var mouseOldX: Double = 0.toDouble()
    private var mouseOldY: Double = 0.toDouble()
    private var mouseDeltaX: Double = 0.toDouble()
    private var mouseDeltaY: Double = 0.toDouble()

    private val pixelMap = HashMap<Pixel, Box>()

    private val canvas = buildCanvas();

    override val root = vbox {
        title = "Muon monitor demonstration"

        toolbar {
            id = "toolbar"
            button("Generate") {
                id = "generateButton"
                onAction = EventHandler { displayEvent(simulateOne(UniformTrackGenerator())) }
            }
            button("Clear") {
                id = "clearButton"
                onAction = EventHandler { clearEvents() }
            }
//            text(labelTextProperty) {
//                textProperty().onChange {  }
//            }
        }
        splitpane {
            orientation = Orientation.HORIZONTAL
            add(canvas)
            tableview(events) {
                column("name", EventDisplay::name)
                column("theta", EventDisplay::theta)
                column("phi", EventDisplay::phi)
                columnResizePolicy = TableView.CONSTRAINED_RESIZE_POLICY
                onSelectionChange {
                    events.forEach {
                        it.event.hits.forEach {
                            pixelMap[it]?.material = redMaterial
                        }
                    }
                    it?.event?.hits?.forEach {
                        pixelMap[it]?.material = blueMaterial
                    }
                }
            }
        }

    }

    init {
        //initializing camera
        buildCamera()
        //initializing axes
        buildAxes()
        //initializing world
        buildContent()

        //hooking the input
        handleKeyboard()
        handleMouse(canvas)

        this.monitor.isCache = true
        this.monitor.cacheHint = CacheHint.ROTATE
    }

    private fun buildCanvas(): SubScene {
        log.info("build canvas")
        val monitorScene = Group(world, cameraXform)
        monitorScene.depthTest = DepthTest.ENABLE
        val canvas = SubScene(monitorScene, 1024.0, 768.0, true, SceneAntialiasing.BALANCED)
        canvas.fill = Color.GREY
        canvas.camera = camera
        canvas.id = "canvas"
        return canvas;
    }

    private fun buildCamera() {
        log.info("buildCamera")
        cameraXform.children.add(cameraXform2)
        cameraXform2.children.add(cameraXform3)
        cameraXform3.children.add(camera)
        cameraXform3.setRotateZ(180.0)

        camera.nearClip = CAMERA_NEAR_CLIP
        camera.farClip = CAMERA_FAR_CLIP
        camera.translateZ = CAMERA_INITIAL_DISTANCE
        cameraXform.ry.angle = CAMERA_INITIAL_Y_ANGLE
        cameraXform.rx.angle = CAMERA_INITIAL_X_ANGLE
        cameraXform.rz.angle = CAMERA_INITIAL_Z_ANGLE
    }

    private fun buildAxes() {
        log.info("buildAxes")
        val redMaterial = PhongMaterial()
        redMaterial.diffuseColor = Color.DARKRED
        redMaterial.specularColor = Color.RED

        val greenMaterial = PhongMaterial()
        greenMaterial.diffuseColor = Color.DARKGREEN
        greenMaterial.specularColor = Color.GREEN

        val blueMaterial = PhongMaterial()
        blueMaterial.diffuseColor = Color.DARKBLUE
        blueMaterial.specularColor = Color.BLUE

        val xAxis = Box(AXIS_LENGTH, LINE_WIDTH, LINE_WIDTH)
        val yAxis = Box(LINE_WIDTH, AXIS_LENGTH, LINE_WIDTH)
        val zAxis = Box(LINE_WIDTH, LINE_WIDTH, AXIS_LENGTH)

        xAxis.material = redMaterial
        yAxis.material = greenMaterial
        zAxis.material = blueMaterial

        axisGroup.children.addAll(xAxis, yAxis, zAxis)
        axisGroup.isVisible = false
        world.children.addAll(axisGroup)
    }

    private fun drawTrack(track: Track): Xform {
        val line = Xform(Xform.RotateOrder.YZX)
        val lineBox = Box(AXIS_LENGTH, LINE_WIDTH, LINE_WIDTH)
        lineBox.material = redMaterial
        line.children.add(lineBox)
        line.setRy(-track.getTheta() * 180.0 / Math.PI)
        line.setRz(-track.getPhi() * 180.0 / Math.PI)
        line.setTranslate(-track.getX(), track.getY())
        return line
    }

    /**
     * Display event

     * @param event
     */
    fun displayEvent(event: Event) {
        val line = drawTrack(event.track)
        this.world.children.add(line)
        for (p in event.hits) {
            this.setPixelActive(p, true)
        }
        this.events.add(EventDisplay(event, line))
    }

    /**
     * clear all displayed events
     */
    fun clearEvents() {
        events.forEach {
            this.world.children.remove(it.xform)
            it.event.hits.forEach { h -> setPixelActive(h, false) }
        }
        events.clear()
    }

    private fun handleMouse(scene: SubScene) {
        scene.onMousePressed = EventHandler<MouseEvent> { me ->
            mousePosX = me.sceneX
            mousePosY = me.sceneY
            mouseOldX = me.sceneX
            mouseOldY = me.sceneY
        }
        scene.onMouseDragged = EventHandler<MouseEvent> { me ->
            mouseOldX = mousePosX
            mouseOldY = mousePosY
            mousePosX = me.sceneX
            mousePosY = me.sceneY
            mouseDeltaX = mousePosX - mouseOldX
            mouseDeltaY = mousePosY - mouseOldY

            var modifier = 1.0

            if (me.isControlDown) {
                modifier = CONTROL_MULTIPLIER
            }
            if (me.isShiftDown) {
                modifier = SHIFT_MULTIPLIER
            }
            if (me.isPrimaryButtonDown) {
                cameraXform.rz.angle = cameraXform.rz.angle + mouseDeltaX * MOUSE_SPEED * modifier * ROTATION_SPEED
                cameraXform.rx.angle = cameraXform.rx.angle + mouseDeltaY * MOUSE_SPEED * modifier * ROTATION_SPEED
                //                } else if (me.isSecondaryButtonDown()) {
                //                    double z = camera.getTranslateZ();
                //                    double newZ = z + mouseDeltaX * MOUSE_SPEED * modifier*100;
                //                    camera.setTranslateZ(newZ);
            } else if (me.isSecondaryButtonDown) {
                cameraXform2.t.x = cameraXform2.t.x + mouseDeltaX * MOUSE_SPEED * modifier * TRACK_SPEED
                cameraXform2.t.y = cameraXform2.t.y + mouseDeltaY * MOUSE_SPEED * modifier * TRACK_SPEED
            }
        }
        scene.onScroll = EventHandler<ScrollEvent> { event ->
            val z = camera.translateZ
            val newZ = z + MOUSE_SPEED * event.deltaY * RESIZE_SPEED
            camera.translateZ = newZ
        }
    }

    private fun handleKeyboard() {
        root.onKeyPressed = EventHandler<KeyEvent> { event ->
            when (event.code) {
                KeyCode.Z -> {
                    cameraXform2.t.x = 0.0
                    cameraXform2.t.y = 0.0
                    camera.translateZ = CAMERA_INITIAL_DISTANCE
                    cameraXform.ry.angle = CAMERA_INITIAL_Y_ANGLE
                    cameraXform.rx.angle = CAMERA_INITIAL_X_ANGLE
                }
                KeyCode.X -> axisGroup.isVisible = !axisGroup.isVisible
                KeyCode.S -> snapshot()
                KeyCode.DIGIT1 -> pixelMap.filterKeys { it.getLayerNumber() == 1 }.values.forEach { toggleTransparency(it) }
                KeyCode.DIGIT2 -> pixelMap.filterKeys { it.getLayerNumber() == 2 }.values.forEach { toggleTransparency(it) }
                KeyCode.DIGIT3 -> pixelMap.filterKeys { it.getLayerNumber() == 3 }.values.forEach { toggleTransparency(it) }
                else ->{}//do nothing
            }
        }
    }


    private fun buildContent() {
        log.info("buildContent")
        val monitorXform = Xform()

        for (p in pixels.values) {
            //Slightly reducing pixel sizes to see gaps
            val pixelBox = Box(p.xSize - 0.1, p.ySize - 0.1, p.zSize - 0.1)
            pixelBox.material = whiteMaterial
            pixelBox.translateX = -p.center.x
            pixelBox.translateY = p.center.y
            pixelBox.translateZ = -p.center.z

            pixelMap.put(p, pixelBox)

            val cap = Text(p.name)
            cap.style = "-fx-font-size:20;-fx-font-weight: bold"
            //            cap.setFill(Color.WHITE);
            cap.translateX = pixelBox.translateX - p.xSize / 4
            cap.translateY = pixelBox.translateY
            //Text is floating over surface for better rendering
            cap.translateZ = pixelBox.translateZ - p.zSize / 2 - 2.0

            monitorXform.children.addAll(pixelBox, cap)
        }

        monitor.children.add(monitorXform)

        world.children.addAll(monitorXform)
    }

    private fun setPixelActive(pixel: Pixel, active: Boolean) {
        val pixelBox = pixelMap[pixel]
        if (pixelBox != null) {
            if (active) {
                pixelBox.material = redMaterial
            } else {
                pixelBox.material = whiteMaterial
            }
        }
    }

    private fun snapshot() {
        canvas.snapshot({ res ->
            val chooser = FileChooser()
            chooser.initialFileName = "snapshot_" + System.currentTimeMillis() + ".png"
            chooser.title = "Select output file"
            chooser.extensionFilters.add(FileChooser.ExtensionFilter("PNG", "*.png"))
            val file = chooser.showSaveDialog(primaryStage.scene.window)
            if (file != null) {
                try {
                    ImageIO.write(SwingFXUtils.fromFXImage(res.image, null), "png", file)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            } else {
                log.info("Saving snapshot canceled");
            }
            null;
        }, SnapshotParameters(), null)
    }

    private fun toggleTransparency(box: Box) {
        box.isVisible = !box.isVisible
//        if (box.material is PhongMaterial) {
//            val material = box.material as PhongMaterial;
//            val color = material.diffuseColor;
//            val scolor = material.specularColor;
//            if (color.opacity == 1.0) {
//                material.diffuseColor = Color(color.red, color.green, color.blue, 0.0);
//                material.specularColor = Color(scolor.red, scolor.green, scolor.blue, 0.0);
//            } else {
//                material.diffuseColor = Color(color.red, color.green, color.blue, 1.0);
//                material.specularColor = Color(scolor.red, scolor.green, scolor.blue, 1.0);
//            }
//        }
    }

    companion object {
        private val CAMERA_INITIAL_DISTANCE = -4500.0
        private val CAMERA_INITIAL_X_ANGLE = -50.0
        private val CAMERA_INITIAL_Y_ANGLE = 0.0
        private val CAMERA_INITIAL_Z_ANGLE = -210.0
        private val CAMERA_NEAR_CLIP = 0.1
        private val CAMERA_FAR_CLIP = 10000.0
        private val AXIS_LENGTH = 2000.0
        private val CONTROL_MULTIPLIER = 0.1
        private val SHIFT_MULTIPLIER = 10.0
        private val MOUSE_SPEED = 0.1
        private val ROTATION_SPEED = 2.0
        private val TRACK_SPEED = 6.0
        private val RESIZE_SPEED = 50.0
        private val LINE_WIDTH = 3.0

        private val redMaterial = PhongMaterial()

        private val whiteMaterial = PhongMaterial()

        private val greyMaterial = PhongMaterial()

        private val blueMaterial = PhongMaterial(Color.BLUE)

        init {
            redMaterial.diffuseColor = Color.DARKRED
            redMaterial.specularColor = Color.RED

            whiteMaterial.diffuseColor = Color.WHITE
            whiteMaterial.specularColor = Color.LIGHTBLUE

            greyMaterial.diffuseColor = Color.DARKGREY
            greyMaterial.specularColor = Color.GREY
        }

    }

}
