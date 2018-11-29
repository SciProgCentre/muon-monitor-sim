package ru.mipt.npm.muon.sim.app

import javafx.beans.property.SimpleBooleanProperty
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.embed.swing.SwingFXUtils
import javafx.event.EventHandler
import javafx.scene.*
import javafx.scene.control.Alert
import javafx.scene.control.TableView
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseEvent
import javafx.scene.input.ScrollEvent
import javafx.scene.paint.Color
import javafx.scene.paint.Material
import javafx.scene.paint.PhongMaterial
import javafx.scene.shape.Box
import javafx.scene.text.Text
import javafx.stage.FileChooser
import javafx.stage.Stage
import kotlinx.coroutines.*
import kotlinx.coroutines.javafx.JavaFx
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
    private val cameraRotation = Xform()
    private val cameraShift = Xform()

    private val events = FXCollections.observableArrayList<EventDisplay>()

    private var mousePosX: Double = 0.toDouble()
    private var mousePosY: Double = 0.toDouble()
    private var mouseOldX: Double = 0.toDouble()
    private var mouseOldY: Double = 0.toDouble()
    private var mouseDeltaX: Double = 0.toDouble()
    private var mouseDeltaY: Double = 0.toDouble()

    private val pixelMap = HashMap<Pixel, Box>()

    private val canvas = buildCanvas()

    private val highlightedPixels = FXCollections.observableArrayList<Pixel>()

    private val listShowingProperty = SimpleBooleanProperty().apply {
        onChange { newValue: Boolean ->
            if (newValue && newValue != eventListStage.isShowing) {
                eventListStage.show()
            } else {
                eventListStage.hide()
            }
        }
    }

    private val eventListStage: Stage by lazy {
        val stage = EventListView().openWindow(owner = currentWindow) ?: kotlin.error("Can't create event list window")
        stage.showingProperty().onChange { newValue: Boolean ->
            if (!newValue) {
                listShowingProperty.set(false)
            }
        }
        stage
    }


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
            togglebutton("List") {
                isSelected = false
                listShowingProperty.bindBidirectional(this.selectedProperty())
            }
            togglebutton("Preview") {
                isSelected = false
                this.selectedProperty().onChange {
                    if (it) startPreview() else stopPreview()
                }
            }
            separator { }
            label("Custom pixel highlight: ")
            textfield {
                id = "highlighter"
                prefWidth = 400.0
                onKeyPressed = EventHandler { event ->
                    if (event.code == KeyCode.ENTER) {
                        if (text.isEmpty()) {
                            highlightedPixels.clear()
                        } else {
                            try {
                                var truncate = text.trim()
                                if (truncate.startsWith("[")) {
                                    truncate = truncate.substring(1, truncate.length - 1)
                                }
                                highlightedPixels.setAll(truncate.split(",").map { findPixelByName(it.trim()) })
                            } catch (ex: Exception) {
                                alert(Alert.AlertType.ERROR, "Wrong syntax for pixel names", ex.message ?: "")
                            }
                        }
                    }
                }
            }
        }
        add(canvas)
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

        canvas.widthProperty().bind(root.widthProperty())

        this.monitor.isCache = true
        this.monitor.cacheHint = CacheHint.ROTATE

        highlightedPixels.addListener(ListChangeListener { change ->
            while (change.next()) {
                //reset previously highlighted pixels
                change.removed.forEach { resetPixelColor(it) }
                change.list.forEach { setPixelColor(it, Color.GREEN) }
            }
        })
    }

    private fun findPixelByName(name: String): Pixel {
        val fullName = if (name.startsWith("SC")) {
            name
        } else {
            "SC$name"
        }
        return pixels[fullName]!!
    }

    private fun buildCanvas(): SubScene {
        log.info("build canvas")
        val monitorScene = Group(world, cameraRotation)
        monitorScene.depthTest = DepthTest.ENABLE
        val canvas = SubScene(monitorScene, 1024.0, 768.0, true, SceneAntialiasing.BALANCED)
        canvas.fill = Color.GREY
        canvas.camera = camera
        canvas.id = "canvas"
        return canvas
    }

    private fun buildCamera() {
        log.info("buildCamera")
        cameraRotation.children.add(cameraShift)
        val cameraFlip = Xform()
        cameraShift.children.add(cameraFlip)
        cameraFlip.children.add(camera)
        cameraFlip.setRotateZ(180.0)

        camera.nearClip = CAMERA_NEAR_CLIP
        camera.farClip = CAMERA_FAR_CLIP
        camera.translateZ = CAMERA_INITIAL_DISTANCE
        cameraRotation.ry.angle = CAMERA_INITIAL_Y_ANGLE
        cameraRotation.rx.angle = CAMERA_INITIAL_X_ANGLE
        cameraRotation.rz.angle = CAMERA_INITIAL_Z_ANGLE
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

            val modifier = when {
                me.isControlDown -> CONTROL_MULTIPLIER
                me.isShiftDown -> SHIFT_MULTIPLIER
                else -> 1.0
            }

            if (me.isPrimaryButtonDown) {
                cameraRotation.rz.angle = cameraRotation.rz.angle + mouseDeltaX * MOUSE_SPEED * modifier * ROTATION_SPEED
                cameraRotation.rx.angle = cameraRotation.rx.angle + mouseDeltaY * MOUSE_SPEED * modifier * ROTATION_SPEED
                //                } else if (me.isSecondaryButtonDown()) {
                //                    double z = camera.getTranslateZ();
                //                    double newZ = z + mouseDeltaX * MOUSE_SPEED * modifier*100;
                //                    camera.setTranslateZ(newZ);
            } else if (me.isSecondaryButtonDown) {
                cameraShift.t.x = cameraShift.t.x + mouseDeltaX * MOUSE_SPEED * modifier * TRACK_SPEED
                cameraShift.t.y = cameraShift.t.y + mouseDeltaY * MOUSE_SPEED * modifier * TRACK_SPEED
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
            if (event.isControlDown) {
                when (event.code) {
                    KeyCode.Z -> {
                        cameraShift.t.x = 0.0
                        cameraShift.t.y = 0.0
                        camera.translateZ = CAMERA_INITIAL_DISTANCE
                        cameraRotation.ry.angle = CAMERA_INITIAL_Y_ANGLE
                        cameraRotation.rx.angle = CAMERA_INITIAL_X_ANGLE
                    }
                    KeyCode.X -> axisGroup.isVisible = !axisGroup.isVisible
                    KeyCode.S -> snapshot()
                    KeyCode.DIGIT1 -> pixelMap.filterKeys { it.getLayerNumber() == 1 }.values.forEach { toggleTransparency(it) }
                    KeyCode.DIGIT2 -> pixelMap.filterKeys { it.getLayerNumber() == 2 }.values.forEach { toggleTransparency(it) }
                    KeyCode.DIGIT3 -> pixelMap.filterKeys { it.getLayerNumber() == 3 }.values.forEach { toggleTransparency(it) }
                    else -> {
                    }//do nothing
                }
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

            pixelMap[p] = pixelBox

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
        if (active) {
            setPixelMaterial(pixel, redMaterial)
        } else {
            setPixelMaterial(pixel, whiteMaterial)
        }
    }

    private fun setPixelMaterial(pixel: Pixel, material: Material) {
        pixelMap[pixel]?.material = material
    }

    private fun setPixelColor(pixel: Pixel, color: Color) {
        setPixelMaterial(pixel, PhongMaterial(color))
    }

    private fun resetPixelColor(pixel: Pixel) {
        setPixelMaterial(pixel, whiteMaterial)
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
                log.info("Saving snapshot canceled")
            }
            null
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


    private var previewJob: Job? = null

    private fun startPreview() {
        previewJob = GlobalScope.launch(Dispatchers.JavaFx) {
            launch {
                while (true) {
                    delay(50)
                    cameraRotation.rz.angle += ROTATION_SPEED / 2
                }
            }
            launch {
                while(true){
                    delay(1000)
                    displayEvent(simulateOne(UniformTrackGenerator()))
                }
            }
        }
    }

    private fun stopPreview() {
        previewJob?.cancel()
        previewJob = null
    }

    inner class EventListView : View("List of events") {

        override val root = borderpane {
            center = tableview(events) {
                readonlyColumn("name", MonitorView.EventDisplay::name)
                readonlyColumn("theta", MonitorView.EventDisplay::theta)
                readonlyColumn("phi", MonitorView.EventDisplay::phi)
                columnResizePolicy = TableView.CONSTRAINED_RESIZE_POLICY
                onSelectionChange {
                    events.forEach { event ->
                        event.event.hits.forEach { pixel ->
                            setPixelMaterial(pixel, MonitorView.redMaterial)
                        }
                    }
                    it?.event?.hits?.forEach { pixel ->
                        setPixelMaterial(pixel, MonitorView.blueMaterial)
                    }
                }
            }

        }
    }

    companion object {
        private const val CAMERA_INITIAL_DISTANCE = -4500.0
        private const val CAMERA_INITIAL_X_ANGLE = -50.0
        private const val CAMERA_INITIAL_Y_ANGLE = 0.0
        private const val CAMERA_INITIAL_Z_ANGLE = -210.0
        private const val CAMERA_NEAR_CLIP = 0.1
        private const val CAMERA_FAR_CLIP = 10000.0
        private const val AXIS_LENGTH = 2000.0
        private const val CONTROL_MULTIPLIER = 0.1
        private const val SHIFT_MULTIPLIER = 10.0
        private const val MOUSE_SPEED = 0.1
        private const val ROTATION_SPEED = 2.0
        private const val TRACK_SPEED = 6.0
        private const val RESIZE_SPEED = 50.0
        private const val LINE_WIDTH = 3.0

        private val redMaterial = PhongMaterial().apply {
            diffuseColor = Color.DARKRED
            specularColor = Color.RED
        }

        private val whiteMaterial = PhongMaterial().apply {
            diffuseColor = Color.WHITE
            specularColor = Color.LIGHTBLUE
        }

        private val greyMaterial = PhongMaterial().apply {
            diffuseColor = Color.DARKGREY
            specularColor = Color.GREY
        }

        private val blueMaterial = PhongMaterial(Color.BLUE)

    }

}
