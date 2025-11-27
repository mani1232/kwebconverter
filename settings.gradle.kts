rootProject.name = "KWebUtils"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        maven("https://central.sonatype.com/repository/maven-snapshots/")
        maven("https://packages.jetbrains.team/maven/p/firework/dev")
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositories {
        maven("https://repo.worldmandia.cc/snapshots")
        mavenLocal()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        maven("https://central.sonatype.com/repository/maven-snapshots/")
        maven("https://packages.jetbrains.team/maven/p/firework/dev")
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }

    versionCatalogs {
        create("kotlinWrappers") {
            val wrappersVersion = "2025.11.12"
            from("org.jetbrains.kotlin-wrappers:kotlin-wrappers-catalog:$wrappersVersion")
        }
        create("custom") {
            from(files("gradle/custom.versions.toml"))

            val devVersion = settings.providers.gradleProperty("devVersion").getOrElse("+dev3326")

            version("androidx-lifecycle", "2.10.0-alpha07$devVersion")
            version("androidx-nav3", "1.0.0-alpha07$devVersion")
            version("androidx-adaptive", "1.3.0-alpha03$devVersion")
            version("androidx-material3", "1.10.0-alpha06$devVersion")
            version("composeMultiplatform", "1.10.10-alpha01$devVersion")
        }
    }
}

include(":config-editor")
//include(":compose-example") // Only for Android or maybe for non-web targets
include(":index-menu")
include(":backend")