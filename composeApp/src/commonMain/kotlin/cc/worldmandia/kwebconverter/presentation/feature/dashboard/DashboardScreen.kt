package cc.worldmandia.kwebconverter.presentation.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cc.worldmandia.kwebconverter.core.OrbitCamera
import cc.worldmandia.kwebconverter.domain.model.ProjectFile
import cc.worldmandia.kwebconverter.presentation.common.MainFont
import com.zakgof.korender.Korender
import com.zakgof.korender.math.ColorRGB.Companion.white
import com.zakgof.korender.math.ColorRGBA
import com.zakgof.korender.math.FloatMath.PIdiv2
import com.zakgof.korender.math.Transform.Companion.scale
import com.zakgof.korender.math.Vec3
import com.zakgof.korender.math.y
import com.zakgof.korender.math.z
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.CupertinoMaterials
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.rememberHazeState
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitPickerState
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import kwebconverter.composeapp.generated.resources.Res

@OptIn(
    ExperimentalMaterial3ExpressiveApi::class, ExperimentalComposeUiApi::class,
    ExperimentalHazeMaterialsApi::class
)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onFileOpen: (ProjectFile) -> Unit
) {
    val files by viewModel.files.collectAsStateWithLifecycle()

    var isBlurEnabled by remember { mutableStateOf(false) }
    val hazeState = rememberHazeState(isBlurEnabled)

    val launcher = rememberFilePickerLauncher(
        mode = FileKitMode.MultipleWithState(maxItems = 5),
        type = FileKitType.File(extensions = listOf("yml", "yaml", "json", "json5")),
        title = "Open config files"
    ) { state ->
        if (state is FileKitPickerState.Completed) {
            viewModel.onFilesSelected(state.result)
        }
    }

    val callback = remember {
        object : DragAndDropTarget {
            override fun onDrop(event: DragAndDropEvent): Boolean {
                return onDropDragAndDropEvent(viewModel).invoke(event)
            }

            override fun onStarted(event: DragAndDropEvent) {
                isBlurEnabled = true
                hazeState.blurEnabled = isBlurEnabled
            }

            override fun onEnded(event: DragAndDropEvent) {
                isBlurEnabled = false
                hazeState.blurEnabled = isBlurEnabled
            }


            override fun onExited(event: DragAndDropEvent) {
                isBlurEnabled = false
                hazeState.blurEnabled = isBlurEnabled
            }
        }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { launcher.launch() },
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Open File", fontFamily = MainFont) },
            )
        },
        modifier = Modifier.fillMaxSize().dragAndDropTarget(
            shouldStartDragAndDrop = onDragAndDropEvent(),
            target = callback
        )
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp).fillMaxSize().hazeSource(hazeState)) {
            Text("Projects", style = MaterialTheme.typography.headlineMedium, fontFamily = MainFont)
            Spacer(Modifier.height(16.dp))

            if (files.isEmpty()) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text("No open files.\nClick + to start.", fontFamily = MainFont)
                    //Column(modifier = Modifier.width(400.dp).height(400.dp).align(Alignment.CenterStart)) {
                    //    TestChangeColor(MaterialTheme.colorScheme.surface)
                    //    GltfExample(MaterialTheme.colorScheme.surface)
                    //}
                    //Column(modifier = Modifier.width(400.dp).height(400.dp).align(Alignment.CenterEnd)) {
                    //    ObjFileExample(MaterialTheme.colorScheme.surface)
                    //}
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(files, key = { it.id }) { file ->
                        FileCard(file, onClick = { onFileOpen(file) })
                    }
                }
            }
        }

        if (isBlurEnabled) {
            Column(
                modifier = Modifier.fillMaxSize().background(
                    color = Color.Transparent,
                ).padding(64.dp).clip(RoundedCornerShape(16.dp))
                    .hazeEffect(state = hazeState, style = CupertinoMaterials.ultraThin())
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    FileUploadCard()
                }
            }
        }
    }
}

@Composable
@Preview
fun TestPreview() {
    FileUploadCard()
}

@Composable
fun FileUploadCard(modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(Modifier.padding(16.dp)) {
            Icon(
                Icons.Default.UploadFile,
                "Add new file",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.CenterHorizontally).size(32.dp)
            )
            Spacer(Modifier.width(16.dp))
            Text("Drop file here", modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}

@Composable
fun FileCard(file: ProjectFile, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Description, null, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(file.name, style = MaterialTheme.typography.titleMedium, fontFamily = MainFont)
                Text(file.format.name, style = MaterialTheme.typography.bodySmall)
            }
            FilledTonalButton(onClick = onClick) {
                Icon(Icons.Default.Edit, null, Modifier.size(16.dp))
                Text("Edit", Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
fun GltfExample(surface: Color) = Korender(appResourceLoader = { Res.readBytes(it) }) {
    val orbitCamera = OrbitCamera(20.z, 3.y)
    OnTouch { orbitCamera.touch(it) }
    Frame {
        base(ColorRGBA.Green)
        background = with(surface) {
            ColorRGBA(red, green, blue, 1f)
        }
        camera = orbitCamera.run { camera() }
        DirectionalLight(Vec3(1.0f, -1.0f, -1.0f), white(3f))
        AmbientLight(white(0.6f))
        Gltf(
            resource = "models/dress.glb",
            transform = scale(0.03f)//.rotate(1.y, frameInfo.time)
        )
    }
}

@Composable
fun TestChangeColor(surface: Color) {
    Korender(appResourceLoader = { Res.readBytes(it) }) {
        val orbitCamera = OrbitCamera(20.z, 3.y)
        OnTouch { orbitCamera.touch(it) }
        Frame {
            camera = orbitCamera.run { camera() }
            background = with(surface) {
                ColorRGBA(red, green, blue, 1f)
            }
            DirectionalLight(Vec3(1.0f, -1.0f, -1.0f), white(3f))
            Gltf(
                resource = "models/dress.glb",
                materialOverrides = mapOf(
                    "FABRIC_1_FRONT_1309" to base(color = ColorRGBA.Green, metallicFactor = 0.0f, roughnessFactor = 0.9f),
                ),
                transform = scale(0.0025f).rotate(Vec3(1f, 0f, 0f), -1.57f).rotate(Vec3(0f, 1f, 0f), frameInfo.time),
            )
        }
    }
}

private fun Color.toRGBA(a: Float = 1f) = ColorRGBA(red, green, blue, a)

@Composable
fun ObjFileExample(surface: Color) {
    Korender(appResourceLoader = { Res.readBytes(it) }) {
        val orbitCamera = OrbitCamera(20.z, 0.z)
        OnTouch { orbitCamera.touch(it) }
        Frame {
            background = with(surface) {
                ColorRGBA(red, green, blue, 1f)
            }
            DirectionalLight(Vec3(1.0f, -1.0f, -1.0f), white(3f))
            camera = orbitCamera.run { camera() }
            Renderable(
                base(colorTexture = texture("models/head.jpg"), metallicFactor = 0.3f, roughnessFactor = 0.5f),
                mesh = obj("models/head.obj"),
                transform = scale(7.0f).rotate(1.y, -PIdiv2),
            )
        }
    }
}

expect fun onDragAndDropEvent(): (DragAndDropEvent) -> Boolean
expect fun onDropDragAndDropEvent(viewModel: DashboardViewModel): (DragAndDropEvent) -> Boolean