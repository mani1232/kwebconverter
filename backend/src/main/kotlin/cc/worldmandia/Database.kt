package cc.worldmandia

import cc.worldmandia.database.user.User
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.migration.r2dbc.MigrationUtils
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.SchemaUtils
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction

class Database(config: FileBasedConfiguration) {
    val db = R2dbcDatabase.connect(
        url = "r2dbc:postgresql://play.worldmandia.cc:5432/${config.postgresConfig.databaseName}",
        driver = "postgresql",
        user = config.postgresConfig.postgresUser,
        password = config.postgresConfig.postgresPassword
    )

    operator fun invoke() = runBlocking {
        suspendTransaction {
            listOf(User.Table).forEach { table ->
                SchemaUtils.create(table)
                MigrationUtils.statementsRequiredForDatabaseMigration(table, withLogs = true)
            }
        }
    }
}