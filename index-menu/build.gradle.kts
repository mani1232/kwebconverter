plugins {
    alias(custom.plugins.kotlinMultiplatform)
    alias(custom.plugins.kotlinSerialization)
}

kotlin {
    js {
        browser {
            commonWebpackConfig {
                outputFileName = "index-menu.js"
            }
        }
        binaries.executable()
    }

    sourceSets {
        jsMain.dependencies {
            implementation(kotlinWrappers.browser)
            implementation(kotlinWrappers.web)

            implementation(kotlinWrappers.react)
            implementation(kotlinWrappers.reactDom)
            implementation(kotlinWrappers.reactRouter)

            implementation(kotlinWrappers.emotion.styled)

            implementation(kotlinWrappers.mui.material)
            implementation(kotlinWrappers.mui.iconsMaterial)
            implementation(kotlinWrappers.mui.system)
        }
        webMain.dependencies {
            //implementation(custom.compose.runtime)
        }
        commonMain.dependencies {
            //implementation(custom.compose.html)
        }
    }
}