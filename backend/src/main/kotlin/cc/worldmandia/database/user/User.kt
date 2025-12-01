package cc.worldmandia.database.user

import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.datetime

object User {
    const val MAX_VARCHAR_LENGTH = 50

    object Table : IntIdTable("Users") {
        val name = varchar("name", MAX_VARCHAR_LENGTH)
        val bio = text("bio")
        val age = short("age")
        val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    }
}