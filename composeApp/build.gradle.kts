import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    //alias(libs.plugins.composePwa)
}

kotlin {
    //js {
    //    browser()
    //    binaries.executable()
    //}

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation("org.jetbrains.compose.runtime:runtime:1.10.10-alpha01+dev3301")
            implementation("org.jetbrains.compose.foundation:foundation:1.10.10-alpha01+dev3301")
            implementation("org.jetbrains.compose.ui:ui:1.10.10-alpha01+dev3301")
            implementation("org.jetbrains.compose.material:material-icons-extended:1.7.3")
            implementation("org.jetbrains.compose.components:components-resources:1.10.10-alpha01+dev3301")
            implementation("org.jetbrains.compose.ui:ui-tooling-preview:1.10.10-alpha01+dev3301")

            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime)
            implementation(libs.androidx.material3)
            implementation(libs.androidx.material3.adaptive)
            implementation(libs.androidx.material3.adaptive.nav3)
            implementation(libs.androidx.lifecycle.viewmodel.nav3)
            implementation(libs.androidx.nav3.ui)

            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.koin.compose.viewmodel.navigation)
            implementation(libs.koin.compose.viewmodel.navigation3)

            implementation(kotlinWrappers.browser)

            implementation(libs.serialization.json5)
            implementation(libs.serialization.yaml)

            implementation(libs.compose.dnd)
            implementation(libs.compose.korender)
            implementation(libs.compose.haze)
            implementation(libs.compose.haze.materials)

            implementation(libs.filekit.core)
            implementation(libs.filekit.dialogs)
            implementation(libs.filekit.dialogs.compose)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}


