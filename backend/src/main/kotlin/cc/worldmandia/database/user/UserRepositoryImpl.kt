package cc.worldmandia.database.user

import kotlinx.coroutines.flow.toSet
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction

class UserRepositoryImpl: UserRepository {
    override suspend fun getAllUsersByName(userName: String) = suspendTransaction {
        User.Table.selectAll().where { User.Table.name eq userName }.toSet()
    }

    override suspend fun getAllUsersBySortType(sortType: SortType): Set<ResultRow> = suspendTransaction {
        when (sortType) {
            SortType.NEW -> User.Table.selectAll().orderBy(User.Table.createdAt, SortOrder.DESC).toSet()
        }
    }
}