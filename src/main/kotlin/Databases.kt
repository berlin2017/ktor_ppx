package com.berlin

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.Database

fun Application.configureDatabases() {

    val database = Database.connect(
        url = "jdbc:postgresql://localhost:5432/ppx_db",
        user = "postgres",
        password = "123456",
    )
    val userService = UserService(database)
    routing {
        // Create user
        post("/users") {
            val user = call.receive<RegisterUser>()
            val id = userService.create(user)
            call.respond(HttpStatusCode.Created, id)
        }

        get("/users") {
            val responseUsers = userService.get()
            call.respond(HttpStatusCode.Created, responseUsers)
        }

        // Read user
        get("/users/{id}") {
            val id = call.parameters["id"]?.toInt() ?: throw IllegalArgumentException("Invalid ID")
            val user = userService.read(id)
            if (user != null) {
                call.respond(HttpStatusCode.OK, user)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }

        // Update user
        put("/users/{id}") {
            val id = call.parameters["id"]?.toInt() ?: throw IllegalArgumentException("Invalid ID")
            val user = call.receive<UpdateUser>()
            userService.update(id, user)
            call.respond(HttpStatusCode.OK)
        }

        // Delete user
        delete("/users/{id}") {
            val id = call.parameters["id"]?.toInt() ?: throw IllegalArgumentException("Invalid ID")
            userService.delete(id)
            call.respond(HttpStatusCode.OK)
        }
    }
}
