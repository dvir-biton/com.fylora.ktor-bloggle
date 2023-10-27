package com.fylora.session.model

sealed class Request(val type: String) {
    data class MakePost(val body: String): Request("post")
    data class MakeComment(val body: String, val postId: String): Request("comment")
    data class MakeLikePost(val postId: String): Request("like_post")
    data class MakeLikeComment(val commentId: String): Request("like_comment")
}
