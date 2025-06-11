package com.berlin.post

import com.berlin.user.ResponseUser
import kotlinx.serialization.Serializable

@Serializable
data class PostItem(
    val id: Int, val userInfo: ResponseUser? = null, val content: String? = null, val images: List<String>? = arrayListOf(),
    val videoUrl: String? = null, val likesCount: Int = 0, val unLikesCount: Int = 0, val commentsCount: Int = 0,
    val postType: Int = 0, val isLiked: Boolean = false, val isUnliked: Boolean = false, val timestamp: Long = System.currentTimeMillis(),
)


@Serializable
data class AddPostItem(
    val content: String? = null,
    val images: List<String>? = arrayListOf(),
    val videoUrl: String? = null,
    val postType: Int = 0
)