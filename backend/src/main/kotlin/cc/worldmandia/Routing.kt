package cc.worldmandia

import cc.worldmandia.database.user.SortType
import cc.worldmandia.database.user.User
import cc.worldmandia.database.user.UserRepository
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
import org.koin.ktor.ext.inject

fun Application.configureRouting() {
    install(Resources)
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respondText(text = "500: $cause", status = HttpStatusCode.InternalServerError)
        }
        status(HttpStatusCode.NotFound) { call, status ->
            call.response.header("Refresh", "5; url=/")

            call.respondText(
                text = "404: Page Not Found. You will be redirected in 5 seconds.", status = status
            )
        }
    }
    val userRepository by inject<UserRepository>()

    routing {
        preCompressed {
            get<UsersApi> { userApi ->
                userApi.sort?.let { sortType ->
                    call.respond(
                        StringBuilder().append("List of articles sorted starting from ${sortType.name}")
                            .append(userRepository.getAllUsersBySortType(sortType).map {
                                StringBuilder().append(it[User.Table.id]).append(it[User.Table.name])
                                    .append(it[User.Table.age]).append(it[User.Table.bio]).append("--------------")
                            })
                    )
                } ?: call.respond(HttpStatusCode.BadRequest)
            }
            singlePageApplication {
                useResources = true
                filesPath = "static"
                defaultPage = "index.html"
                ignoreFiles { it.endsWith(".txt") }
            }
        }

    }
}

@Serializable
@Resource("/v1/api/users")
class UsersApi(val sort: SortType? = SortType.NEW)