package com.berlin.post

import com.berlin.user.UserService
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

class PostService(private val userService: UserService, database: Database) {
    object Posts : Table() {
        val id = integer("id").autoIncrement()
        val content = varchar("content", length = 1000)
        val images = varchar("images", length = 1000).nullable()
        val videoUrl = varchar("videoUrl", length = 1000).nullable()
        val likesCount = integer("likesCount").default(0)
        val unLikesCount = integer("unLikesCount").default(0)
        val commentsCount = integer("commentsCount").default(0)
        val postType = integer("postType").default(0)
        val userId = integer("userId")
        val timestamp = long("timestamp")

        override val primaryKey = PrimaryKey(id)
    }

    init {
        transaction(database) {
            SchemaUtils.create(Posts)
        }
    }

    suspend fun create(user: Int, postItem: AddPostItem) =
        dbQuery {
            val currentTime = System.currentTimeMillis()
            Posts.insert {
                it[content] = postItem.content ?: ""
                it[images] = Gson().toJson(postItem.images)
                it[videoUrl] = postItem.videoUrl
                it[postType] = postItem.postType
                it[userId] = user
                it[timestamp] = currentTime
            }[Posts.id]
        }

//    suspend fun read(id: Int): ResponseUser? {
//        return dbQuery {
//            Users.selectAll()
//                .where { Users.id eq id }
//                .map {
//                    ResponseUser(
//                        id = it[Users.id],
//                        name = it[Users.name],
//                        age = it[Users.age],
//                        email = it[Users.email],
//                        avatar = it[Users.avatar],
//                    )
//                }
//                .singleOrNull()
//        }
//    }
//
//    suspend fun read(email: String): ResponseUser? {
//        return dbQuery {
//            Users.selectAll()
//                .where { Users.email eq email }
//                .map {
//                    ResponseUser(
//                        id = it[Users.id],
//                        name = it[Users.name],
//                        age = it[Users.age],
//                        email = it[Users.email],
//                        avatar = it[Users.avatar],
//                    )
//                }
//                .singleOrNull()
//        }
//    }

//    suspend fun update(id: Int, user: UpdateUser) {
//        dbQuery {
//            Users.update({ Users.id eq id }) {
//                if (user.name?.isNotBlank() == true) it[name] = user.name
//                if (user.age != 0) it[age] = user.age
//                if (user.email?.isNotBlank() == true) it[email] = user.email
//                if (user.password?.isNotBlank() == true) it[password] = user.password
//                if (user.avatar?.isNotBlank() == true) it[avatar] = user.avatar
//            }
//        }
//    }
//
//    suspend fun delete(id: Int) {
//        dbQuery {
//            Users.deleteWhere { Users.id.eq(id) }
//        }
//    }

    suspend fun get(page: Int, limit: Int): List<PostItem> {
        return dbQuery {
            Posts.selectAll()
                .orderBy(Posts.timestamp, SortOrder.DESC)
                .limit(limit).offset(start = (page * limit).toLong())
                .map {
                    val userId = it[Posts.userId]
                    val userInfo = userService.read(id = userId)

                    val listStringType = object : TypeToken<List<String>>() {}.type
                    PostItem(
                        id = it[Posts.id],
                        content = it[Posts.content],
                        images = Gson().fromJson(it[Posts.images], listStringType),
                        likesCount = it[Posts.likesCount],
                        unLikesCount = it[Posts.unLikesCount],
                        commentsCount = it[Posts.commentsCount],
                        userInfo = userInfo,
                        timestamp = it[Posts.timestamp],
                        postType = it[Posts.postType],
                        videoUrl = it[Posts.videoUrl]
                    )
                }
        }
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}

