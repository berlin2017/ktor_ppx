package com.berlin

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import java.io.File

fun Application.configureUpload() {

    routing {
        post("/upload") {
            var fileDescription = ""
            var fileName = ""
            val multipartData = call.receiveMultipart(formFieldLimit = 1024 * 1024 * 100)

            multipartData.forEachPart { part ->
                when (part) {
                    is PartData.FormItem -> {
                        fileDescription = part.value
                    }

                    is PartData.FileItem -> {
                        fileName = part.originalFileName as String
                        val file = File("uploads/$fileName")
                        part.provider().copyAndClose(file.writeChannel())
                    }

                    else -> {}
                }
                part.dispose()
            }

            call.respondText("$fileDescription is uploaded to 'uploads/$fileName'")
        }

        get("/uploads/{id}") {
            val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)

            val file = File("uploads/$id")

            call.respondFile(file)
        }
    }
}