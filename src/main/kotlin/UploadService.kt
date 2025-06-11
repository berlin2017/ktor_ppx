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
import java.text.SimpleDateFormat
import java.util.*

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
                        // Ensure the "uploads" directory exists
                        val uploadDir = File("uploads")
                        if (!uploadDir.exists()) {
                            uploadDir.mkdirs()
                        }
                        val timespan = SimpleDateFormat("yyyyMMddHHmmss").format(Date())
                        fileName = timespan + "-"+ generateUniqueFileNameRobust(part.originalFileName as String)
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


// More robust version for extensionless files:
fun generateUniqueFileNameRobust(originalFileName: String?): String {
    if (originalFileName.isNullOrBlank()) {
        return "${UUID.randomUUID()}.dat"
    }

    val extension: String

    val dotIndex = originalFileName.lastIndexOf('.')
    if (dotIndex > 0 && dotIndex < originalFileName.length - 1) {
        // Standard case: "filename.ext"
        extension = originalFileName.substring(dotIndex) // Includes the dot
    } else if (dotIndex == 0) {
        extension = originalFileName // e.g. ".bashrc"
    }
    else {
        // No dot, or dot is the last character. Treat as no extension.
        extension = ""
    }
    val uniqueId = UUID.randomUUID().toString()
    return "$uniqueId$extension" // If extension is empty, it's just the UUID.
    // If original was ".bashrc", extension is ".bashrc", result "uuid.bashrc"
}