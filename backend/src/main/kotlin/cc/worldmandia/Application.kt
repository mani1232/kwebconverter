package cc.worldmandia

import com.akuleshov7.ktoml.Toml
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import java.io.File

val toml = Toml()
val configurationFile = File("configuration.toml")

@Serializable
data class FileBasedConfiguration(
    val postgresConfig: PostgresConfig = PostgresConfig(),
) {
    @Serializable
    data class PostgresConfig(
        val postgresPassword: String = "password",
        val postgresUser: String = "username",
        val databaseName: String = "ktor_backend_db",
    )
}

fun main() {
    embeddedServer(CIO, port = 2565, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    configureFrameworks()
    configureHTTP()
    configureSerialization()
    configureRouting()
}

fun loadConfiguration(): FileBasedConfiguration = if (configurationFile.exists()) {
    toml.decodeFromString(configurationFile.readText())
} else {
    configurationFile.createNewFile()
    FileBasedConfiguration().also {
        configurationFile.writeText(toml.encodeToString(it))
    }
}
