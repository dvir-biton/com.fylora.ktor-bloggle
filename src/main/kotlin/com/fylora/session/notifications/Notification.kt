package com.fylora.session.notifications

sealed class Notification(val message: String) {
    data class PostLiked(
        val by: String,
        val timestamp: Long,
        val postId: String,
    ): Notification("Liked you post!")
    data class CommentLiked(
        val by: String,
        val timestamp: Long,
        val postId: String,
    ): Notification("Liked you comment!")
    data class Following(
        val by: String,
        val timestamp: Long,
    ): Notification("Started following you!")
    data class Comment(
        val by: String,
        val timestamp: Long,
        val postId: String,
    ): Notification("Commented on you post!")
}
