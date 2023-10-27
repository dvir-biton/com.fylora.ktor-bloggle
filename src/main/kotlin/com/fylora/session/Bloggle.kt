package com.fylora.session

import com.fylora.auth.data.user.User
import com.fylora.logging.Logging
import com.fylora.session.model.*
import com.fylora.session.model.Account.Companion.toAccount
import com.fylora.session.notifications.Notification
import com.fylora.session.notifications.NotifyUser
import io.ktor.websocket.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.log

class Bloggle {

    private val posts = mutableListOf<Post>()
    private val accounts = mutableListOf<Account>()
    private val activeUsers = mutableListOf<ActiveUser>()
    private val notifiedUsers = mutableListOf<NotifyUser>()

    suspend fun follow(follower: ActiveUser, followUserId: String): Resource<String> {
        val followAccount = accounts.find { it.userId == followUserId }
            ?: return Resource.Error("User not found")
        val isFollowerInFollowAccountFollowers =
            followAccount.followers.contains(follower.userId)

        if(isFollowerInFollowAccountFollowers) {
            followAccount.followers.remove(follower.userId)
        } else {
            followAccount.followers.add(follower.userId)
            notify(
                authorId = followUserId,
                notification = Notification.Following(
                    by = follower.username,
                    timestamp = System.currentTimeMillis()
                )
            )
        }

        Logging.log(
            Logging.UserFollowed(
                follower = follower,
                followed = followAccount
            )
        )
        return Resource.Success("User added successfully")
    }

    suspend fun likePost(activeUser: ActiveUser, postId: String): Resource<String> {
        val post = posts.find { it.postId == postId }
            ?: return Resource.Error("Post not found")

        notify(
            authorId = post.authorId,
            notification = Notification.PostLiked(
                by = activeUser.username,
                timestamp = System.currentTimeMillis(),
                postId = post.postId
            )
        )

        Logging.log(
            Logging.PostLiked(
                post = post
            )
        )
        return if(post.userLiked.contains(activeUser.userId)) {
            post.userLiked.remove(activeUser.userId)
            Resource.Success("Post successfully unliked")
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
        } ?: return returnAndLogError(
            message = "Comment not found",
            logging = Logging.Fail("Couldn't like comment, Comment not found")
        )
        val comment = post.comments.find { it.commentId == commentId }
            ?: return returnAndLogError(
                message = "Comment not found",
                logging = Logging.Fail("Couldn't like comment, Comment not found")
            )

        notify(
            authorId = post.authorId,
            notification = Notification.CommentLiked(
                by = activeUser.username,
                timestamp = System.currentTimeMillis(),
                postId = post.postId
            )
        )

        Logging.log(
            Logging.CommentLiked(
                comment = comment
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
            return returnAndLogError(
                message = "The user is already logged in",
                logging = Logging.Fail("could not add user, already logged in")
            )
        }

        val account = user.toAccount()
        if(!accounts.contains(account)) {
            accounts.add(account)
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

        Logging.log(
            Logging.UserConnected(
                user = activeUser,
                allUsers = activeUsers
            )
        )
        return Resource.Success(activeUser)
    }

    suspend fun disconnectUser(activeUser: ActiveUser): Resource<String> {
        if(!activeUsers.contains(activeUser)) {
            return returnAndLogError(
                message = "The the user is not logged in",
                logging = Logging.Fail("couldn't disconnect user, not logged in")
            )
        }

        activeUser.session.close()
        activeUsers.remove(activeUser)
        Logging.log(
            Logging.UserConnected(
                user = activeUser,
                allUsers = activeUsers
            )
        )
        return Resource.Success("The user has been disconnected")
    }

    suspend fun post(activeUser: ActiveUser, body: String): Resource<String> {
        val post = Post(
            authorId = activeUser.userId,
            authorName = activeUser.username,
            body = body
        )

        if(post.body.isBlank()) {
            return returnAndLogError(
                message = "The body cannot be blank",
                logging = Logging.Fail("empty post body")
            )
        }

        val account = accounts.find { it.userId == activeUser.userId }
        account?.followers?.forEach {
            notify(
                authorId = it,
                notification = Notification.NewPost(
                    by = activeUser.username,
                    timestamp = System.currentTimeMillis(),
                    postId = post.postId
                )
            )
        }

        posts.add(post)
        Logging.log(
            Logging.NewPost(
                post = post
            )
        )
        return Resource.Success("The user posted successfully")
    }

    suspend fun comment(activeUser: ActiveUser, postId: String, body: String): Resource<String> {
        val comment = Comment(
            authorId = activeUser.userId,
            authorName = activeUser.username,
            body = body
        )

        if(comment.body.isBlank()) {
            return returnAndLogError(
                message = "The body cannot be blank",
                logging = Logging.Fail("empty comment body")
            )
        }

        val post = posts.find { it.postId == postId }
            ?: return Resource.Error("Post not found")

        post.comments.add(comment)

        notify(
            authorId = post.authorId,
            notification = Notification.Comment(
                timestamp = System.currentTimeMillis(),
                by = activeUser.username,
                postId = postId
            )
        )

        Logging.log(
            Logging.NewComment(
                comment = comment,
                post = post
            )
        )
        return Resource.Success("The user posted successfully")
    }

    private suspend fun notify(
        authorId: String,
        notification: Notification,
    ) {
        val author = activeUsers.find { it.userId == authorId }
        if(author != null) {
            Logging.log(
                Logging.SentNotification(
                    notification = notification,
                    sentToId = authorId,
                    sentToName = author.username,
                    type = "type: Live"
                )
            )
            author.session.send(Json.encodeToString(notification))
        } else {
            val notifiedUser = notifiedUsers.find { it.userId == authorId }
            if(notifiedUser != null) {
                notifiedUser.notifications.add(notification)
                Logging.log(
                    Logging.SentNotification(
                        notification = notification,
                        sentToId = notifiedUser.userId,
                        sentToName = notifiedUser.notifications.toString(),
                        type = "type: Notification, existing"
                    )
                )
            } else {
                val newUser = NotifyUser(
                    notifications = mutableListOf(notification),
                    userId = authorId
                )
                notifiedUsers.add(
                    newUser
                )
                Logging.log(
                    Logging.SentNotification(
                        notification = notification,
                        sentToId = newUser.userId,
                        sentToName = newUser.notifications.toString(),
                        type = "type: Notification, new"
                    )
                )
            }
        }
    }

    private fun <T> returnAndLogError(message: String, logging: Logging): Resource<T> {
        Logging.log(logging)
        return Resource.Error(message)
    }
}