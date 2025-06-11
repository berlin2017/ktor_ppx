package com.berlin.post

import com.berlin.generateUniqueFileNameRobust
import com.berlin.user.UserService
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import org.jetbrains.exposed.sql.Database
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


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
        post("/posts") {
            val id = call.parameters["userId"]?.toInt() ?: throw IllegalArgumentException("Invalid ID")
            val user = userService.read(id)
            if (user != null) {
//                val addPostItem = call.receive<AddPostItem>()


                var textContent: String? = null
                val uploadedImageFiles = mutableListOf<String>()
                var uploadedVideoFile: String? = null
                var hasError = false
                var errorMessage = ""

                val multipartData = call.receiveMultipart(formFieldLimit = 1024 * 1024 * 1000)

                multipartData.forEachPart { part ->
                    when (part) {
                        is PartData.FormItem -> {
                            if (part.name == "textContent") {
                                textContent = part.value
                            }
                        }

                        is PartData.FileItem -> {
                            val originalFileName = part.originalFileName
                            if (originalFileName.isNullOrBlank()) {
                                part.dispose()
                                return@forEachPart // 跳过没有原始文件名的文件部分
                            }

                            // Ensure the "uploads" directory exists
                            val uploadDir = File("uploads")
                            if (!uploadDir.exists()) {
                                uploadDir.mkdirs()
                            }

                            // 根据 part.name 或者 contentType 来区分图片和视频
                            // 假设 Flutter 端发送图片时 part.name 为 "images[]" 或 "image1", "image2"
                            // 假设 Flutter 端发送视频时 part.name 为 "video"
                            // 或者更可靠的是检查 contentType
                            val contentType = part.contentType.toString().lowercase()

                            if (contentType.startsWith("image/")) {
                                if (uploadedImageFiles.size < 9) { // 最多9张图片
                                    val savedFile = saveFile(part)
                                    if (savedFile != null) {
                                        uploadedImageFiles.add(savedFile)
                                    } else {
                                        hasError = true
                                        errorMessage = "部分图片保存失败。"
                                        // 无需立即返回，继续处理其他部分，最后统一响应
                                    }
                                } else {
                                    log.warn("图片数量超出限制 (9张)，已忽略后续图片: $originalFileName")
                                    part.dispose() // 记得 dispose 未处理的 part
                                }
                            } else if (contentType.startsWith("video/")) {
                                if (uploadedVideoFile == null) { // 只处理第一个视频
                                    val savedFile = saveFile(part)
                                    if (savedFile != null) {
                                        uploadedVideoFile = savedFile
                                    } else {
                                        hasError = true
                                        errorMessage = "视频文件保存失败。"
                                    }
                                } else {
                                    log.warn("已选择一个视频，忽略后续视频文件: $originalFileName")
                                    part.dispose()
                                }
                            } else {
                                log.warn("不支持的文件类型: $originalFileName, ContentType: $contentType")
                                part.dispose()
                            }


                        }

                        else -> {}
                    }
                    part.dispose()
                }

                if (hasError && errorMessage.isNotBlank()) {
                    // 如果在文件处理过程中发生错误，但不是致命到中断请求的错误
                    // （例如部分文件保存失败，但请求本身是合法的）
                    // 可以选择返回一个包含错误信息的部分成功响应，或完全失败的响应
                    call.respond(HttpStatusCode.BadRequest, errorMessage)
                    return@post
                }

                if (textContent.isNullOrBlank() && uploadedImageFiles.isEmpty() && uploadedVideoFile == null) {
                    return@post call.respond(HttpStatusCode.BadRequest, "请输入内容或选择媒体文件")
                }

                val postType = if (!uploadedVideoFile.isNullOrBlank()) {
                    2
                } else if (uploadedImageFiles.isNotEmpty()) {
                    1
                } else {
                    0
                }
                val addPostItem = AddPostItem(
                    content = textContent,
                    images = uploadedImageFiles,
                    videoUrl = uploadedVideoFile,
                    postType = postType
                )
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

suspend fun saveFile(part: PartData.FileItem): String {
    val timespan = SimpleDateFormat("yyyyMMddHHmmss").format(Date())
    val fileName = timespan + "-" + generateUniqueFileNameRobust(part.originalFileName as String)
    val file = File("uploads/$fileName")
    part.provider().copyAndClose(file.writeChannel())
    return "uploads/$fileName"
}
