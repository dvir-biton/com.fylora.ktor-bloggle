package com.fylora.session

import com.fylora.auth.data.user.User
import com.fylora.auth.data.user.User.Companion.toAccount
import com.fylora.logging.Logging
import com.fylora.session.model.*
import com.fylora.session.notifications.Notification
import com.fylora.session.notifications.NotifyUser
import io.ktor.websocket.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Bloggle {

    private val posts = mutableListOf<Post>()
    private val accounts = mutableListOf<Account>()
    private val activeUsers = mutableListOf<ActiveUser>()
    private val notifiedUsers = mutableListOf<NotifyUser>()

    fun makeAccounts(users: List<User>) {
        users.forEach {
            accounts.add(it.toAccount())
        }
    }

    suspend fun follow(follower: ActiveUser, followUserId: String): Resource<String> {
        val isOwnId = follower.userId == followUserId
        if(isOwnId) {
            return logErrorSendAndReturn(
                message = "You can't follow yourself",
                logging = Logging.Fail(
                    "You can't follow yourself"
                ),
                session = follower.session,
                response = Response.ErrorResponse(
                    "You can't follow yourself"
                )
            )
        }

        val followAccount = accounts.find { it.userId == followUserId }
            ?: return logErrorSendAndReturn(
                message = "User not found",
                logging = Logging.Fail(
                    "follow account"
                ),
                session = follower.session,
                response = Response.ErrorResponse(
                    "User not found"
                )
            )
        val isUserFollowing =
            followAccount.followers.contains(follower.userId)

        if(isUserFollowing) {
            followAccount.followers.remove(follower.userId)
        } else {
            followAccount.followers.add(follower.userId)
            notify(
                notifierId = follower.userId,
                authorId = followUserId,
                notification = Notification.Following(
                    by = follower.username,
                    timestamp = System.currentTimeMillis()
                )
            )
        }

        sendResponse(
            session = follower.session,
            response = Response.ConfirmationResponse(
                "Followed Successfully"
            )
        )
        Logging.log(
            Logging.UserFollowed(
                follower = follower,
                followed = followAccount
            )
        )
        return Resource.Success("User added successfully")
    }

    suspend fun getPost(activeUser: ActiveUser, postId: String): Resource<String> {
        val post = posts.find { it.postId == postId }
            ?: return logErrorSendAndReturn(
                message = "Post not found",
                logging = Logging.Fail(
                    "get post"
                ),
                session = activeUser.session,
                response = Response.ErrorResponse(
                    "Post not found"
                )
            )

        sendResponse(
            session = activeUser.session,
            response = Response.PostResponse(post)
        )

        return Resource.Success("Comments sent")
    }

    suspend fun getAccount(activeUser: ActiveUser, userId: String): Resource<String> {
        val account = accounts.find { it.userId == userId }
            ?: return logErrorSendAndReturn(
                message = "Account not found",
                logging = Logging.Fail(
                    "get account"
                ),
                session = activeUser.session,
                response = Response.ErrorResponse(
                    "Account not found"
                )
            )

        sendResponse(
            session = activeUser.session,
            response = Response.AccountResponse(
                account
            )
        )
        return Resource.Success("Accounts sent")
    }

    suspend fun searchAccounts(
        activeUser: ActiveUser,
        query: String,
        amount: Int = 50
    ): Resource<String> {
        val accountsFound = accounts.filter { account ->
            account.username.contains(query, ignoreCase = true)
        }.sortedWith(
            compareBy(
                { it.username.equals(query, ignoreCase = true) },
                { it.username.startsWith(query, ignoreCase = true) },
                { it.username }
            )
        ).take(amount)

        if(accountsFound.isEmpty()) {
            return logErrorSendAndReturn(
                message = "Accounts not found",
                logging = Logging.Fail(
                    "accounts are empty"
                ),
                session = activeUser.session,
                response = Response.ErrorResponse(
                    "Did you hear that? No? That's the sound of silence, just like our results. Keep searching!"
                )
            )
        }

        sendResponse(
            session = activeUser.session,
            response = Response.AccountsResponse(
                accountsFound
            )
        )

        return Resource.Success("Accounts sent")
    }

    suspend fun getNotifications(activeUser: ActiveUser): Resource<String> {
        val notifyUser = notifiedUsers.find { it.userId == activeUser.userId }
            ?: return logErrorSendAndReturn(
                message = "No messages",
                logging = Logging.Fail(
                    "No messages"
                ),
                session = activeUser.session,
                response = Response.ErrorResponse(
                    "Did you hear that? No? That's the sound of silence, just like our results. Keep searching!"
                )
            )


        sendResponse(
            session = activeUser.session,
            response = Response.NotificationsResponse(
                notifyUser.notifications
            )
        )

        return Resource.Success("Notifications sent")
    }

    suspend fun likePost(activeUser: ActiveUser, postId: String): Resource<String> {
        val post = posts.find { it.postId == postId }
            ?: return logErrorSendAndReturn(
                message = "Post not found",
                logging = Logging.Fail(
                    "Post not found couldn't like the post"
                ),
                session = activeUser.session,
                response = Response.ErrorResponse(
                    "Post not found"
                )
            )
        val account = accounts.find { it.userId == post.authorId }
            ?: return logErrorSendAndReturn(
                message = "Account not found",
                logging = Logging.Fail(
                    "Account not found couldn't like the post"
                ),
                session = activeUser.session,
                response = Response.ErrorResponse(
                    "Account not found"
                )
            )
        val index = accounts.indexOf(account)
        if(index == -1) {
            return logErrorSendAndReturn(
                message = "Account not found",
                logging = Logging.Fail(
                    "Account not found couldn't like the post"
                ),
                session = activeUser.session,
                response = Response.ErrorResponse(
                    "Account not found"
                )
            )
        }

        notify(
            notifierId = activeUser.userId,
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
            sendResponse(
                session = activeUser.session,
                response = Response.ConfirmationResponse(
                    "Post successfully disliked"
                )
            )
            activeUsers.forEach {
                sendResponse(
                    session = it.session,
                    response = Response.PostsResponse(
                        posts
                    )
                )
            }

            val updatedAccount = account.copy(
                totalLikes = account.totalLikes - 1
            )
            accounts[index] = updatedAccount
            Resource.Success("Post successfully unliked")
        } else {
            post.userLiked.add(activeUser.userId)
            sendResponse(
                session = activeUser.session,
                response = Response.ConfirmationResponse(
                    "Post successfully liked"
                )
            )
            activeUsers.forEach {
                sendResponse(
                    session = it.session,
                    response = Response.PostsResponse(
                        posts
                    )
                )
            }

            val updatedAccount = account.copy(
                totalLikes = account.totalLikes + 1
            )
            accounts[index] = updatedAccount
            Resource.Success("Post successfully liked")
        }
    }

    suspend fun likeComment(activeUser: ActiveUser, commentId: String): Resource<String> {
        val post = posts.find { post ->
            post.comments.find { comment ->
                comment.commentId == commentId
            } != null
        } ?: return logErrorSendAndReturn(
            message = "Comment not found",
            logging = Logging.Fail(
                "Comment not found"
            ),
            session = activeUser.session,
            response = Response.ErrorResponse(
                "Comment not found"
            )
        )
        val comment = post.comments.find { it.commentId == commentId }
            ?: return logErrorSendAndReturn(
                message = "Comment not found",
                logging = Logging.Fail(
                    "Comment not found"
                ),
                session = activeUser.session,
                response = Response.ErrorResponse(
                    "Comment not found"
                )
            )
        val account = accounts.find { it.userId == post.authorId }
            ?: return logErrorSendAndReturn(
                message = "Account not found",
                logging = Logging.Fail(
                    "Account not found couldn't like the post"
                ),
                session = activeUser.session,
                response = Response.ErrorResponse(
                    "Account not found"
                )
            )
        val index = accounts.indexOf(account)
        if(index == -1) {
            return logErrorSendAndReturn(
                message = "Account not found",
                logging = Logging.Fail(
                    "Account not found couldn't like the post"
                ),
                session = activeUser.session,
                response = Response.ErrorResponse(
                    "Account not found"
                )
            )
        }

        notify(
            notifierId = activeUser.userId,
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
            sendResponse(
                session = activeUser.session,
                response = Response.ConfirmationResponse(
                    "Comment successfully disliked"
                )
            )

            val updatedAccount = account.copy(
                totalLikes = account.totalLikes - 1
            )
            accounts[index] = updatedAccount
            Resource.Success("Comment successfully disliked")
        } else {
            comment.userLiked.add(activeUser.userId)
            sendResponse(
                session = activeUser.session,
                response = Response.ConfirmationResponse(
                    "Comment successfully liked"
                )
            )
            val updatedAccount = account.copy(
                totalLikes = account.totalLikes + 1
            )
            accounts[index] = updatedAccount
            Resource.Success("Comment successfully liked")
        }
    }

    suspend fun getPosts(activeUser: ActiveUser) {
        sendResponse(
            session = activeUser.session,
            response = Response.PostsResponse(
                posts
            )
        )
    }

    suspend fun addUser(userId: String, username: String, session: WebSocketSession): Resource<ActiveUser> {
        val activeUser = ActiveUser(
            username = username,
            userId = userId,
            session = session
        )

        val account = Account(
            username = username,
            userId = userId,
        )
        if(
            !accounts.any { it.userId == userId }
        ) {
            accounts.add(
                account
            )
        }

        if(activeUsers.contains(activeUser)) {
            return logErrorSendAndReturn(
                message = "User already logged in",
                logging = Logging.Fail(
                    "User already logged in"
                ),
                session = activeUser.session,
                response = Response.ErrorResponse(
                    "The user is already logged in"
                )
            )
        }

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
            return logErrorSendAndReturn(
                message = "User isn't logged in",
                logging = Logging.Fail(
                    "User isn't logged in"
                ),
                session = activeUser.session,
                response = Response.ErrorResponse(
                    "The user isn't logged in"
                )
            )
        }

        activeUser.session.close()
        activeUsers.remove(activeUser)
        Logging.log(
            Logging.UserDisconnected(
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
            return logErrorSendAndReturn(
                message = "Empty post body",
                logging = Logging.Fail(
                    "Empty post body"
                ),
                session = activeUser.session,
                response = Response.ErrorResponse(
                    "The post body cannot be empty"
                )
            )
        }

        val account = accounts.find { it.userId == activeUser.userId }
            ?: return logErrorSendAndReturn(
                message = "Account not found",
                logging = Logging.Fail(
                    "Account not found couldn't add the post"
                ),
                session = activeUser.session,
                response = Response.ErrorResponse(
                    "Account not found"
                )
            )
        account.posts.add(0, post)
        account.followers.forEach {
            notify(
                notifierId = activeUser.userId,
                authorId = it,
                notification = Notification.NewPost(
                    by = activeUser.username,
                    timestamp = System.currentTimeMillis(),
                    postId = post.postId
                )
            )
        }

        posts.add(0, post)

        activeUsers.forEach {
            sendResponse(
                session = it.session,
                response = Response.PostsResponse(
                    posts
                )
            )
        }
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
            return logErrorSendAndReturn(
                message = "Empty comment body",
                logging = Logging.Fail(
                    "Empty comment body"
                ),
                session = activeUser.session,
                response = Response.ErrorResponse(
                    "The comment body cannot be empty"
                )
            )
        }

        val post = posts.find { it.postId == postId }
            ?: return logErrorSendAndReturn(
                message = "Post not found",
                logging = Logging.Fail(
                    "Post not found couldn't like the post"
                ),
                session = activeUser.session,
                response = Response.ErrorResponse(
                    "Post not found"
                )
            )

        post.comments.add(0, comment)

        activeUsers.forEach {
            sendResponse(
                session = it.session,
                response = Response.PostResponse(
                    post
                )
            )
        }

        notify(
            notifierId = activeUser.userId,
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

    private fun notify(
        notifierId: String,
        authorId: String,
        notification: Notification,
    ) {
        if(notifierId == authorId)
            return

        val notifiedUser = notifiedUsers.find { it.userId == authorId }

        if(notifiedUser != null) {
            notifiedUser.notifications.add(0, notification)
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

    private suspend fun sendResponse(
        session: WebSocketSession,
        response: Response
    ) {
        session.send(
            Frame.Text(
                Json.encodeToString(
                    response
                )
            )
        )
    }

    suspend fun <T> logErrorSendAndReturn(
        message: String,
        logging: Logging,
        session: WebSocketSession,
        response: Response
    ): Resource<T> {
        session.send(
            Frame.Text(
                Json.encodeToString(
                    response
                )
            )
        )

        Logging.log(logging)
        return Resource.Error(message)
    }
}