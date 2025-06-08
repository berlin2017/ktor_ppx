package com.berlin

import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

@Serializable
data class RegisterUser(val name: String, val age: Int?, val email: String, val password: String)

@Serializable
data class UpdateUser(val name: String?, val age: Int? = 0, val email: String? = "", val password: String? = "", var avatar: String? = "")

@Serializable
data class ResponseUser(val id: Int, val name: String, val age: Int?, val email: String?, var avatar: String?)

class UserService(database: Database) {
    object Users : Table() {
        val id = integer("id").autoIncrement()
        val name = varchar("name", length = 50)
        val age = integer("age").nullable().default(0)
        val email = varchar("email", length = 50).default("")
        val password = varchar("password", length = 50)
        val avatar = varchar("avatar", length = 50).nullable().default("")

        override val primaryKey = PrimaryKey(id)
    }

    init {
        transaction(database) {
            SchemaUtils.create(Users)
        }
    }

    suspend fun create(user: RegisterUser): Int = dbQuery {
        Users.insert {
            it[name] = user.name
            it[age] = user.age
            it[email] = user.email
            it[password] = user.password
        }[Users.id]
    }

    suspend fun read(id: Int): ResponseUser? {
        return dbQuery {
            Users.selectAll()
                .where { Users.id eq id }
                .map {
                    ResponseUser(
                        id = it[Users.id],
                        name = it[Users.name],
                        age = it[Users.age],
                        email = it[Users.email],
                        avatar = it[Users.avatar],
                    )
                }
                .singleOrNull()
        }
    }

    suspend fun update(id: Int, user: UpdateUser) {
        dbQuery {
            Users.update({ Users.id eq id }) {
                if (user.name?.isNotBlank() == true) it[name] = user.name
                if (user.age != 0) it[age] = user.age
                if (user.email?.isNotBlank() == true) it[email] = user.email
                if (user.password?.isNotBlank() == true) it[password] = user.password
                if (user.avatar?.isNotBlank() == true) it[avatar] = user.avatar
            }
        }
    }

    suspend fun delete(id: Int) {
        dbQuery {
            Users.deleteWhere { Users.id.eq(id) }
        }
    }

    suspend fun get(): List<ResponseUser> {
        return dbQuery {
            Users.selectAll().map {
                ResponseUser(
                    id = it[Users.id],
                    name = it[Users.name],
                    age = it[Users.age],
                    email = it[Users.email],
                    avatar = it[Users.avatar],
                )
            }
        }
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}

