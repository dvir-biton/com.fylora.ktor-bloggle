package com.fylora.session

import com.fylora.auth.data.user.User
import com.fylora.session.model.ActiveUser
import com.fylora.session.model.Comment
import com.fylora.session.model.Post
import com.fylora.session.model.Resource
import com.fylora.session.notifications.Notification
import com.fylora.session.notifications.NotifyUser
import io.ktor.websocket.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDateTime

class Bloggle {

    private val posts = mutableListOf<Post>()
    private val activeUsers = mutableListOf<ActiveUser>()
    private val notifiedUsers = mutableListOf<NotifyUser>()

    suspend fun likePost(activeUser: ActiveUser, postId: String): Resource<String> {
        val post = posts.find { it.postId == postId }
            ?: return Resource.Error("Post not found")

        notifyAuthor(
            authorId = post.authorId,
            notification = Notification.PostLiked(
                by = activeUser.username,
                timestamp = System.currentTimeMillis(),
                postId = post.postId
            )
        )

        return if(post.userLiked.contains(activeUser.userId)) {
            post.userLiked.remove(activeUser.userId)
            Resource.Success("Post successfully disliked")
        } else {
            post.userLiked.add(activeUser.userId)
            Resource.Success("Post successfully liked")
        }
    }

    suspend fun likeComment(activeUser: ActiveUser, commentId: String): Resource<String> {
        val post = posts.find { post ->
            post.comments.find { comment ->
                comment.commentId == commentId
            } != null
        } ?: return Resource.Error("Comment not found")
        val comment = post.comments.find { it.commentId == commentId }
            ?: return Resource.Error("Comment not found")

        notifyAuthor(
            authorId = post.authorId,
            notification = Notification.CommentLiked(
                by = activeUser.username,
                timestamp = System.currentTimeMillis(),
                postId = post.postId
            )
        )

        return if(comment.userLiked.contains(activeUser.userId)) {
            post.userLiked.remove(activeUser.userId)
            Resource.Success("Post successfully disliked")
        } else {
            comment.userLiked.add(activeUser.userId)
            Resource.Success("Post successfully liked")
        }
    }

    suspend fun addUser(user: User, session: WebSocketSession): Resource<ActiveUser> {
        val activeUser = ActiveUser(
            username = user.username,
            userId = user.id.toString(),
            session = session
        )

        if(activeUsers.contains(activeUser)) {
            return Resource.Error("The user is already logged in")
        }


        posts.forEach {
            activeUser.session.send(
                Frame.Text(
                    Json.encodeToString(it)
                )
            )
        }

        val notifyUser = notifiedUsers.find { it.userId == user.id.toString() }
        notifyUser?.notifications?.forEach {
            activeUser.session.send(
                Json.encodeToString(it)
            )
        }
        notifiedUsers.remove(notifyUser)

        activeUsers.add(activeUser)
        return Resource.Success(activeUser)
    }

    suspend fun disconnectUser(activeUser: ActiveUser): Resource<String> {
        if(!activeUsers.contains(activeUser)) {
            return Resource.Error("The the user is not logged in")
        }

        activeUser.session.close()
        activeUsers.remove(activeUser)
        return Resource.Success("The user has been disconnected")
    }

    suspend fun post(activeUser: ActiveUser, body: String): Resource<String> {
        val post = Post(
            authorId = activeUser.userId,
            authorName = activeUser.username,
            body = body
        )

        if(post.body.isBlank()) {
            return Resource.Error("The body cannot be blank")
        }

        activeUsers.forEach {
            it.session.send(
                Frame.Text(
                    Json.encodeToString(post)
                )
            )
        }

        posts.add(post)
        return Resource.Success("The user posted successfully")
    }

    suspend fun comment(activeUser: ActiveUser, postId: String, body: String): Resource<String> {
        val comment = Comment(
            authorId = activeUser.userId,
            authorName = activeUser.username,
            body = body
        )

        if(comment.body.isBlank()) {
            return Resource.Error("The body cannot be blank")
        }

        val post = posts.find { it.postId == postId }
            ?: return Resource.Error("Post not found")

        post.comments.add(comment)

        notifyAuthor(
            authorId = post.authorId,
            notification = Notification.Comment(
                timestamp = System.currentTimeMillis(),
                by = activeUser.username,
                postId = postId
            )
        )

        return Resource.Success("The user posted successfully")
    }

    private suspend fun notifyAuthor(
        authorId: String,
        notification: Notification,
    ) {
        val author = activeUsers.find { it.userId == authorId }
        if(author != null) {
            author.session.send(Json.encodeToString(notification))
        } else {
            val notifiedUser = notifiedUsers.find { it.userId == authorId }
            if(notifiedUser != null) {
                notifiedUser.notifications.add(notification)
            } else {
                notifiedUsers.add(
                    NotifyUser(
                        notifications = mutableListOf(notification),
                        userId = authorId
                    )
                )
            }
        }
    }
}