import com.google.devtools.ksp.KspExperimental
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(custom.plugins.kotlinMultiplatform)
    alias(custom.plugins.composeMultiplatform)
    alias(custom.plugins.composeCompiler)
    alias(custom.plugins.kotlinSerialization)
    alias(custom.plugins.androidMultiplatform)
    alias(custom.plugins.ksp)
    alias(custom.plugins.composeHotReload)
    alias(custom.plugins.composePwa)
}

kotlin {
    //js {
    //    browser()
    //    binaries.executable()
    //}
    jvm("hotRunJvm") {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    compilerOptions {
        freeCompilerArgs.addAll("-Xexpect-actual-classes")
    }

    android {
        namespace = "cc.worldmandia.kwebutils"
        compileSdk { version = preview("36.1") }
        androidResources.enable = true
        minSdk = 21
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser {
            commonWebpackConfig {
                outputFileName = "config-editor.js"
            }
            testTask {
                useKarma {
                    useChrome()
                }
            }
        }
        binaries.executable()

    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        androidMain.dependencies {
            implementation(custom.compose.ui.tooling.preview)
            implementation(custom.compose.ui)
            implementation(custom.androidx.material3)
            implementation(custom.koin.android)
        }

        commonMain.dependencies {
            implementation(custom.bundles.compose.common)
            implementation(custom.compose.material.icons.extended)

            implementation(custom.androidx.lifecycle.viewmodel)
            implementation(custom.androidx.lifecycle.runtime)
            implementation(custom.androidx.material3)
            implementation(custom.androidx.material3.adaptive)
            implementation(custom.androidx.material3.adaptive.nav3)
            implementation(custom.androidx.lifecycle.viewmodel.nav3)
            implementation(custom.androidx.nav3.ui)

            implementation(project.dependencies.platform(custom.koin.bom))
            implementation(custom.koin.core)
            implementation(custom.koin.compose)
            implementation(custom.koin.compose.viewmodel)
            implementation(custom.koin.compose.viewmodel.navigation)
            implementation(custom.koin.compose.viewmodel.navigation3)

            implementation(custom.serialization.json5)
            implementation(custom.serialization.yaml)

            implementation(custom.compose.dnd)
            implementation(custom.compose.korender)
            implementation(custom.compose.haze)
            implementation(custom.compose.haze.materials)

            implementation(custom.multiplatformSettings)

            implementation(custom.filekit.core)
            implementation(custom.filekit.dialogs)
            implementation(custom.filekit.dialogs.compose)
        }
        commonTest.dependencies {
            implementation(custom.kotlin.test)
        }

        webMain.dependencies {
            implementation(kotlinWrappers.browser)
            implementation(kotlinWrappers.web)
        }
    }
}

compose {
    desktop {
        application {
            mainClass = "cc.worldmandia.kwebutils.MainKt"
        }
    }
    resources {
        publicResClass = true
    }
}

ksp {
    @OptIn(KspExperimental::class)
    useKsp2 = true
}