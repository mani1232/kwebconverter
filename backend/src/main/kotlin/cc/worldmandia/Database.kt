package cc.worldmandia

import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase

class Database(config: FileBasedConfiguration) {
    val db = R2dbcDatabase.connect(
        url = "r2dbc:postgresql://play.worldmandia.cc:5432/${config.postgresConfig.databaseName}",
        driver = "postgresql",
        user = config.postgresConfig.postgresUser,
        password = config.postgresConfig.postgresPassword
    )
}