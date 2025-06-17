package com.berlin.videoparse

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit


fun Application.configVideoParer() {

    routing {
        post("/api/video-info") {
            try {
                val request = call.receive<VideoUrlRequest>()
                val videoUrl = request.url

                if (videoUrl.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, YtDlpInfo(null, null, "Video URL cannot be empty."))
                    return@post
                }

                // 调用 yt-dlp 来获取 JSON 格式的视频信息
                // -j, --dump-json: Dump single video information as JSON (alias for --print '%()j')
                // --no-warnings: Ignore warnings
                // --no-call-home: Do Not Call Home, Do Not Send Statistics
                val command = listOf(
                    "yt-dlp",
                    "-j", // 输出 JSON
                    "--no-warnings",
                    "--no-call-home",
                    "--no-check-certificate",
                    videoUrl
                )

                val processResult = executeCommand(command)

                if (processResult.exitCode == 0) {
                    // 尝试将 stdout 解析为 YtDlpInfo (yt-dlp -j 输出单个 JSON 对象)
                    // 注意：yt-dlp 的 JSON 输出可能直接是 Format 列表或者一个包含 title, formats 等的顶层对象
                    // 这里我们假设它是一个包含 title, formats 的对象
                    // 你可能需要根据实际的 yt-dlp JSON 输出来调整 YtDlpInfo 和 YtDlpFormat 数据类
                    try {
                        // 使用 kotlinx.serialization.json.Json 来解析
                        val jsonParser = kotlinx.serialization.json.Json { ignoreUnknownKeys = true; isLenient = true }
                        val info = jsonParser.decodeFromString<YtDlpInfoInternal>(processResult.stdout)

                        call.respond(YtDlpInfo(info.title, info.formats?.map { it.toExternal() }))
                    } catch (e: Exception) {
                        log.error("Failed to parse yt-dlp JSON output: ${e.message}")
                        // 如果 JSON 解析失败，仍然可以返回原始输出以供调试
                        call.respond(
                            io.ktor.http.HttpStatusCode.InternalServerError,
                            YtDlpInfo(
                                null,
                                null,
                                "Failed to parse yt-dlp JSON output: ${e.message}",
                                processResult.stdout
                            )
                        )
                    }
                } else {
                    log.error("yt-dlp execution failed. Exit code: ${processResult.exitCode}, Error: ${processResult.stderr}")
                    call.respond(
                        io.ktor.http.HttpStatusCode.InternalServerError,
                        YtDlpInfo(
                            null,
                            null,
                            "yt-dlp execution failed: ${processResult.stderr.ifBlank { "Unknown error" }}",
                            processResult.stdout
                        )
                    )
                }

            } catch (e: Exception) {
                log.error("Error processing video info request: ${e.message}", e)
                call.respond(HttpStatusCode.InternalServerError, YtDlpInfo(null, null, "Server error: ${e.message}"))
            }
        }
    }
}


// 辅助数据类，更接近 yt-dlp 的 JSON 输出结构
@Serializable
data class YtDlpInfoInternal(
    val title: String? = null,
    val formats: List<YtDlpFormatInternal>? = null,
    // 你可以添加更多你想从 yt-dlp JSON 中提取的字段
    val id: String? = null,
    val webpage_url: String? = null,
    val description: String? = null,
    val uploader: String? = null,
    val duration: Double? = null,
    val thumbnail: String? = null
)

@Serializable
data class YtDlpFormatInternal(
    val format_id: String? = null,
    val ext: String? = null,
    val resolution: String? = null, // "widthxheight"
    val fps: Float? = null,
    val filesize: Long? = null,
    val filesize_approx: Long? = null, // yt-dlp 2023.03.04+
    val tbr: Double? = null,
    val vcodec: String? = null,
    val acodec: String? = null,
    val url: String? = null,
    val format_note: String? = null,
    val height: Int? = null,
    val width: Int? = null,
    val protocol: String? = null // e.g. "https", "m3u8_native"
) {
    fun toExternal(): YtDlpFormat = YtDlpFormat(
        formatId = format_id,
        extension = ext,
        resolution = resolution,
        fps = fps,
        filesize = filesize,
        filesizeApprox = filesize_approx,
        tbr = tbr,
        vcodec = vcodec,
        acodec = acodec,
        url = url,
        note = format_note,
        height = height,
        width = width,
        protocol = protocol
    )
}

data class ProcessExecutionResult(val stdout: String, val stderr: String, val exitCode: Int)

fun executeCommand(command: List<String>, timeoutSeconds: Long = 60): ProcessExecutionResult {
    try {
        val processBuilder = ProcessBuilder(command)
        // 可选：设置工作目录
        // processBuilder.directory(File("/path/to/working/dir"))

        val process = processBuilder.start()

        val stdout = StringBuilder()
        val stderr = StringBuilder()

        // 注意：同步读取输出流可能会在某些情况下导致死锁
        // 如果输出非常大，或者 stderr 和 stdout 交错输出很多，考虑异步读取或使用 Process.inputStream.bufferedReader().readText() 等
        val stdOutReader = BufferedReader(InputStreamReader(process.inputStream))
        val stdErrReader = BufferedReader(InputStreamReader(process.errorStream))

        var line: String?
        while (stdOutReader.readLine().also { line = it } != null) {
            stdout.append(line).append(System.lineSeparator())
        }
        while (stdErrReader.readLine().also { line = it } != null) {
            stderr.append(line).append(System.lineSeparator())
        }

        // 等待进程结束，设置超时
        val exited = process.waitFor(timeoutSeconds, TimeUnit.SECONDS)

        if (!exited) {
            process.destroyForcibly()
            return ProcessExecutionResult(
                stdout.toString(),
                stderr.toString() + "\nProcess timed out after $timeoutSeconds seconds.",
                -1
            )
        }

        return ProcessExecutionResult(stdout.toString().trim(), stderr.toString().trim(), process.exitValue())

    } catch (e: Exception) {
        // Log the exception
//        log.error("Failed to execute command '$command': ${e.message}", e)
        return ProcessExecutionResult("", "Exception during command execution: ${e.message}", -1)
    }
}