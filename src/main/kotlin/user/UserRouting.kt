package com.berlin.user

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.Database
import java.util.*

val jwtAudience = "jwt-audience"
val jwtDomain = "https://jwt-provider-domain/"
val jwtRealm = "ktor sample app"
val jwtSecret = "secret"

fun Application.configureUserRoutes() {

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
            val queryUser = userService.read(user.email)
            if (queryUser != null) {
                call.respond(HttpStatusCode.ExpectationFailed, "邮箱已被注册!")
                return@post
            }
            val loginUser = userService.create(user)
            val result = addToken(loginUser)
            if (result != null) {
                call.respond(HttpStatusCode.Created, result)
            } else {
                call.respond(HttpStatusCode.Unauthorized, "Invalid credentials")
            }
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

        post("/login") {
            val userCredentials = call.receive<UserCredentials>()
            // 验证用户凭据 (例如，从数据库中验证用户名和密码)
            val loginUser = userService.login(userCredentials.email, userCredentials.password)
            val result = addToken(loginUser)
            if (result != null) {
                call.respond(HttpStatusCode.OK, result)
            } else {
                call.respond(HttpStatusCode.Unauthorized, "Invalid credentials")
            }
        }


    }

}

fun addToken(loginUser: ResponseUser?): LoginUser? {
    if (loginUser != null) {
        val token = JWT.create().withAudience(jwtAudience).withIssuer(jwtDomain)
            .withClaim("userId", loginUser.id) // 添加自定义 claim，例如用户ID
            .withExpiresAt(Date(System.currentTimeMillis() + 60000 * 60)) // 设置过期时间 (例如，1小时)
            .sign(Algorithm.HMAC256(jwtSecret)) // 使用相同的秘密密钥和算法签名

        return LoginUser(loginUser.id, loginUser.name, loginUser.age, loginUser.email, loginUser.avatar, token)

    }
    return null
}