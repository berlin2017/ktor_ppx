package com.berlin.videoparse

import kotlinx.serialization.Serializable

// 定义用于请求和响应的数据类
@Serializable
data class VideoUrlRequest(val url: String)

@Serializable
data class YtDlpInfo(
    val title: String?,
    val formats: List<YtDlpFormat>?,
    val error: String? = null,
    val rawOutput: String? = null // 可选：包含原始输出以供调试
)

@Serializable
data class YtDlpFormat(
    val formatId: String?,
    val extension: String?,
    val resolution: String?,
    val fps: Float?,
    val filesize: Long?,
    val filesizeApprox: Long?, // yt-dlp 2023.03.04+ uses filesize_approx
    val tbr: Double?, // average bitrate (kbps)
    val vcodec: String?,
    val acodec: String?,
    val url: String?, // Direct download URL (if available via -g or in format info)
    val note: String?,
    val height: Int?,
    val width: Int?,
    val protocol: String?
)


