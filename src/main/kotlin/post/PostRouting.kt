package com.berlin.post

import com.berlin.user.UserService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.Database


fun Application.configurePostRoutes() {

    val database = Database.connect(
        url = "jdbc:postgresql://localhost:5432/ppx_db",
        user = "postgres",
        password = "123456",
    )

    val userService = UserService(database)

    val postService = PostService(userService, database)

    routing {
        // Create user
        post("/posts/{userId}") {
            val id = call.parameters["userId"]?.toInt() ?: throw IllegalArgumentException("Invalid ID")
            val user = userService.read(id)
            if (user != null) {
                val addPostItem = call.receive<AddPostItem>()
                postService.create(user = id, addPostItem)
                call.respond(HttpStatusCode.OK)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }

        }

        get("/posts") {
            val page = call.parameters["page"]?.toInt() ?: 0
            val count = call.parameters["limit"]?.toInt() ?: 10
            val postItems = postService.get(page, count)
            call.respond(HttpStatusCode.Created, postItems)
        }

//        // Read user
//        get("/users/{id}") {
//            val id = call.parameters["id"]?.toInt() ?: throw IllegalArgumentException("Invalid ID")
//            val user = userService.read(id)
//            if (user != null) {
//                call.respond(HttpStatusCode.OK, user)
//            } else {
//                call.respond(HttpStatusCode.NotFound)
//            }
//        }
//
//        // Update user
//        put("/users/{id}") {
//            val id = call.parameters["id"]?.toInt() ?: throw IllegalArgumentException("Invalid ID")
//            val user = call.receive<UpdateUser>()
//            userService.update(id, user)
//            call.respond(HttpStatusCode.OK)
//        }
//
//        // Delete user
//        delete("/users/{id}") {
//            val id = call.parameters["id"]?.toInt() ?: throw IllegalArgumentException("Invalid ID")
//            userService.delete(id)
//            call.respond(HttpStatusCode.OK)
//        }
//
//        post("/login") {
//            val userCredentials = call.receive<UserCredentials>()
//            // 验证用户凭据 (例如，从数据库中验证用户名和密码)
//            val loginUser = userService.login(userCredentials.email, userCredentials.password)
//            val result = addToken(loginUser)
//            if (result != null) {
//                call.respond(HttpStatusCode.OK, result)
//            } else {
//                call.respond(HttpStatusCode.Unauthorized, "Invalid credentials")
//            }
//        }


    }

}