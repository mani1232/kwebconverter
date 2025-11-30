package cc.worldmandia.database.user

import org.jetbrains.exposed.v1.core.ResultRow

interface UserRepository {
    suspend fun getAllUsersByName(userName: String): Set<ResultRow>
    suspend fun getAllUsersBySortType(sortType: SortType): Set<ResultRow>
}