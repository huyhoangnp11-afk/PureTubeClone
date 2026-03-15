package com.example.puretube.model

data class VideoStatus(
    val title: String,
    val videoId: String,
    val formatStreams: List<FormatStream>
)

data class FormatStream(
    val url: String,
    val itag: String,
    val type: String,
    val quality: String,
    val fps: Int,
    val container: String,
    val encoding: String,
    val resolution: String,
    val size: String
)
