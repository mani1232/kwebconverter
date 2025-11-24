package cc.worldmandia.kwebconverter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import cc.worldmandia.kwebconverter.presentation.feature.dashboard.FileUploadCard

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            StartComposeApp()
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    FileUploadCard()
    //StartComposeApp()
}