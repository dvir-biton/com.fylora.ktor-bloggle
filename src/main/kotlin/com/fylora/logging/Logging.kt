package com.fylora.logging

import com.fylora.auth.data.user.User
import com.fylora.session.model.Account
import com.fylora.session.model.ActiveUser
import com.fylora.session.model.Comment
import com.fylora.session.model.Post
import com.fylora.session.notifications.Notification

sealed class Logging(val message: String) {
    data class UserConnected(
        val user: ActiveUser,
        val allUsers: List<ActiveUser>
    ): Logging(
        "New user connected!\n" +
        "name: ${user.username}\n" +
        "id: ${user.userId}\n" +
        "totalUsers: $allUsers"
    )
    data class UserDisconnected(
        val user: ActiveUser,
        val allUsers: List<ActiveUser>
    ): Logging(
        "user disconnected!\n" +
        "name: ${user.username}\n" +
        "id: ${user.userId}\n" +
        "totalUsers: $allUsers"
    )
    data class NewPost(
        val post: Post
    ): Logging(
        "new post!\n" +
        "body: ${post.body}\n" +
        "id: ${post.postId}\n" +
        "author: ${post.authorName}\n" +
        "author: ${post.authorId}\n"
    )
    data class NewComment(
        val comment: Comment,
        val post: Post
    ): Logging(
        "new post!\n" +
        "body: ${comment.body}\n" +
        "id: ${comment.commentId}\n" +
        "author: ${comment.authorName}\n" +
        "author: ${comment.authorId}\n" +
        "post comment: ${post.comments}"
    )
    data class PostLiked(
        val post: Post
    ): Logging(
        "post Liked!\n" +
        "body: ${post.body}\n" +
        "id: ${post.postId}\n" +
        "author: ${post.authorName}\n" +
        "author: ${post.authorId}\n" +
        "likes: ${post.userLiked.size}" +
        "liked by: ${post.userLiked}"
    )
    data class CommentLiked(
        val comment: Comment
    ): Logging(
        "post Liked!\n" +
        "body: ${comment.body}\n" +
        "id: ${comment.commentId}\n" +
        "author: ${comment.authorName}\n" +
        "author: ${comment.authorId}\n" +
        "likes: ${comment.userLiked.size}" +
        "liked by: ${comment.userLiked}"
    )
    data class UserFollowed(
        val follower: ActiveUser,
        val followed: Account
    ): Logging(
        "user followed!\n" +
        "follower: ${follower.username}" +
        "followed: ${followed.username}" +
        "followers amount: ${followed.followers.size}" +
        "followers name: ${followed.followers}"
    )
    data class SentNotification(
        val notification: Notification,
        val sentToId: String,
        val sentToName: String,
        val type: String
    ): Logging(
        "notification: ${notification.message}" +
        "sent to: $sentToId" +
        "sent to name: $sentToName" +
        "type: $type"
    )
    data class Fail(val action: String): Logging(
        "failed to perform action: $action"
    )

    companion object {
        fun log(logging: Logging)  {
            println(logging.message)
        }
    }
}
