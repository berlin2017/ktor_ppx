package com.berlin

import com.berlin.post.configurePostRoutes
import com.berlin.user.configureUserRoutes
import com.berlin.videoparse.configVideoParer
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    install(ContentNegotiation) {
        json() // 配置 JSON 序列化器，默认使用 kotlinx.serialization
    }
    configureSerialization()

    configureUserRoutes()
    configurePostRoutes()

    configureFrameworks()
    configureSecurity()
    configureRouting()
    configureUpload()

    configVideoParer()
}
