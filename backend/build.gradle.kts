@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import cc.worldmandia.Docker.IMAGE_NAME
import cc.worldmandia.FrontEnd.MENU_APP
import cc.worldmandia.FrontEnd.wasmApps
import io.ktor.plugin.features.DockerImageRegistry
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSerialization)
    application
}

application {
    mainClass = "cc.worldmandia.ApplicationKt"
}


ktor {
    fatJar {
        archiveFileName.set("backend.jar")
    }
    docker {
        jreVersion.set(JavaVersion.VERSION_25)
        localImageName.set(IMAGE_NAME)
        imageTag.set(providers.gradleProperty("docker-tag.version"))

        externalRegistry.set(DockerImageRegistry.externalRegistry(
            username = providers.environmentVariable("GITHUB_USERNAME"),
            password = providers.environmentVariable("GITHUB_PASSWORD"),
            project = provider { IMAGE_NAME },
            hostname = provider { "ghcr.io" },
            namespace = provider { "mani1232" }
        ))

        // For pterodactyl and pelican panels
        jib {
            container {
                workingDirectory = "/home/container"
                jvmFlags = listOf(
                    "-XX:+UnlockExperimentalVMOptions",
                    "-XX:+UnlockDiagnosticVMOptions",
                    "-XX:+AlwaysActAsServerClassMachine",
                    "-XX:+AlwaysPreTouch",
                    "-XX:+DisableExplicitGC",
                    "-XX:NmethodSweepActivity=1",
                    "-XX:ReservedCodeCacheSize=400M",
                    "-XX:NonNMethodCodeHeapSize=12M",
                    "-XX:ProfiledCodeHeapSize=194M",
                    "-XX:NonProfiledCodeHeapSize=194M",
                    "-XX:-DontCompileHugeMethods",
                    "-XX:MaxNodeLimit=240000",
                    "-XX:NodeLimitFudgeFactor=8000",
                    "-XX:+UseVectorCmov",
                    "-XX:+PerfDisableSharedMem",
                    "-XX:+UseFastUnorderedTimeStamps",
                    "-XX:+UseCriticalJavaThreadPriority",
                    "-XX:ThreadPriorityPolicy=1",
                    "-XX:+UseZGC",
                    "-XX:AllocatePrefetchStyle=1",
                    "-XX:-ZProactive",
                    "-Dterminal.jline=false",
                    "-Dterminal.ansi=true"
                )
            }
        }
    }
}

kotlin {
    sourceSets.main {
        dependencies {
            implementation(libs.bundles.backend)
        }
    }
}

tasks.processResources {
    project(":$MENU_APP").tasks.named("jsBrowserDistribution").also {
        dependsOn(it)

        from(it) {
            into("static")
        }
    }

    wasmApps.forEach { appName ->
        val appTask = project(":$appName").tasks.named("wasmJsBrowserDistribution")
        dependsOn(appTask)
        from(appTask) {
            into("static/$appName")
        }
    }
}
