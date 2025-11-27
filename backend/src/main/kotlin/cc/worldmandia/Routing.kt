package cc.worldmandia

import io.ktor.http.*
import io.ktor.resources.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.resources.*
import io.ktor.server.resources.Resources
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

fun Application.configureRouting() {
    install(Resources)
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respondText(text = "500: $cause", status = HttpStatusCode.InternalServerError)
        }
        status(HttpStatusCode.NotFound) { call, status ->
            call.response.header("Refresh", "5; url=/")

            call.respondText(
                text = "404: Page Not Found. You will be redirected in 5 seconds.",
                status = status
            )
        }
    }
    routing {
        get("/hello") {
            call.respondText("Hello World!")
        }
        get<Articles> { article ->
            call.respond("List of articles sorted starting from ${article.sort}")
        }
        staticResources("/", "static") {
            preCompressed(CompressedFileType.BROTLI, CompressedFileType.GZIP)
        }
    }
}

@Serializable
@Resource("/articles")
class Articles(val sort: String? = "new")
