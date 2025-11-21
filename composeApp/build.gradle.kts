import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.androidMultiplatform)
    //alias(libs.plugins.composePwa)
}

kotlin {
    //js {
    //    browser()
    //    binaries.executable()
    //}

    compilerOptions {
        freeCompilerArgs.addAll("-Xexpect-actual-classes")
    }

    androidLibrary {
        namespace = "cc.worldmandia.kwebconverter"
        compileSdk { version = preview("36.1") }
        androidResources.enable = true
        minSdk = 21
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.ui.tooling.preview)
            implementation(libs.compose.ui)
        }

        commonMain.dependencies {
            implementation(libs.bundles.compose.common)
            implementation(libs.compose.material.icons.extended)

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

        webMain.dependencies {
            implementation(kotlinWrappers.browser)
        }
    }
}

compose.resources {
    publicResClass = true
}