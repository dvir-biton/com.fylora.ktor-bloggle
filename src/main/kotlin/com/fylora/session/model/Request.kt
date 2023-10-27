package com.fylora.session.model

sealed class Request(val type: String) {
    data class MakePost(val body: String): Request("post")
    data class MakeComment(val body: String, val postId: String): Request("comment")
    data class MakeLikePost(val postId: String): Request("like_post")
    data class MakeLikeComment(val commentId: String): Request("like_comment")

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
                else -> throw IllegalArgumentException("Invalid action: $action")
            }
        }
    }
}
