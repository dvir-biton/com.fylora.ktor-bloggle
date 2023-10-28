package com.fylora.session.model

sealed class Request(val type: String) {
    data object GetNotifications: Request("notifications")
    data class MakePost(val body: String): Request("post")
    data class MakeComment(val body: String, val postId: String): Request("comment")
    data class MakeLikePost(val postId: String): Request("like_post")
    data class MakeLikeComment(val commentId: String): Request("like_comment")
    data class MakeFollow(val userId: String): Request("follow")
    data class GetPost(val postId: String): Request("get_post")
    data class SearchAccounts(val query: String): Request("search_accounts")
    data class GetAccount(val userId: String): Request("get_account")

    companion object {
        /*
            requests from the client looks like this:
            <action>&value;value

            for example:
            post&hey!
            or:
            comment&what's up;id
        */
        fun fromString(s: String): Request {
            val action = s.substringBefore("&").trim()
            val parameters = s.substringAfter("&").split(';')

            return when (action) {
                "post" -> {
                    if (parameters.size == 1) {
                        MakePost(parameters[0])
                    } else {
                        throw IllegalArgumentException("Invalid parameters for 'post' action")
                    }
                }
                "comment" -> {
                    if (parameters.size == 2) {
                        MakeComment(parameters[0], parameters[1])
                    } else {
                        throw IllegalArgumentException("Invalid parameters for 'comment' action")
                    }
                }
                "like_post" -> {
                    if (parameters.size == 1) {
                        MakeLikePost(parameters[0])
                    } else {
                        throw IllegalArgumentException("Invalid parameters for 'like_post' action")
                    }
                }
                "like_comment" -> {
                    if (parameters.size == 1) {
                        MakeLikeComment(parameters[0])
                    } else {
                        throw IllegalArgumentException("Invalid parameters for 'like_comment' action")
                    }
                }
                "follow" -> {
                    if (parameters.size == 1) {
                        MakeFollow(parameters[0])
                    } else {
                        throw IllegalArgumentException("Invalid parameters for 'like_comment' action")
                    }
                }
                "get_post" -> {
                    if (parameters.size == 1) {
                        GetPost(parameters[0])
                    } else {
                        throw IllegalArgumentException("Invalid parameters for 'get_post' action")
                    }
                }
                "search_accounts" -> {
                    if (parameters.size == 1) {
                        SearchAccounts(parameters[0])
                    } else {
                        throw IllegalArgumentException("Invalid parameters for 'search_accounts' action")
                    }
                }
                "get_account" -> {
                    if (parameters.size == 1) {
                        GetAccount(parameters[0])
                    } else {
                        throw IllegalArgumentException("Invalid parameters for 'get_account' action")
                    }
                }
                "notifications" -> {
                    if (parameters.isEmpty()) {
                        GetNotifications
                    } else {
                        throw IllegalArgumentException("Invalid parameters for 'notifications' action")
                    }
                }
                else -> throw IllegalArgumentException("Invalid action: $action")
            }
        }
    }
}
