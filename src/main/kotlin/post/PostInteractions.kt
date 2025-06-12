package com.berlin.post

import kotlinx.serialization.Serializable

@Serializable
data class PostInteractions(val id: Int, val userId: Int, val postId: Int, val type: PostInteraction, val timestamp: Long)

enum class PostInteraction(val value:Int) {
    NONE(0),
    LIKE(1),
    DISLIKE(2),
}