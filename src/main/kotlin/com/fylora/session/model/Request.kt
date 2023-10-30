package com.fylora.session.model

import kotlinx.serialization.Serializable

@Serializable
sealed class Request(val type: String) {
    data object GetNotifications: Request("notifications")
    data object GetPosts: Request("posts")
    data class MakePost(val body: String): Request("post")
    data class MakeComment(val body: String, val postId: String): Request("comment")
    data class MakeLikePost(val postId: String): Request("like_post")
    data class MakeLikeComment(val commentId: String): Request("like_comment")
    data class MakeFollow(val userId: String): Request("follow")
    data class GetPost(val postId: String): Request("get_post")
    data class SearchAccounts(val query: String): Request("search_accounts")
    data class GetAccount(val userId: String): Request("get_account")
}
