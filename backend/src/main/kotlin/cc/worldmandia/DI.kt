package cc.worldmandia

import cc.worldmandia.database.user.UserRepository
import cc.worldmandia.database.user.UserRepositoryImpl
import io.ktor.server.application.*
import org.jetbrains.exposed.v1.r2dbc.transactions.TransactionManager
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun Application.configureFrameworks() {
    install(Koin) {
        slf4jLogger()
        modules(module {
            single { loadConfiguration() }
            single { Database(get()).also { TransactionManager.defaultDatabase = it.db } }
            singleOf(::UserRepositoryImpl) bind UserRepository::class
        })
    }
}
