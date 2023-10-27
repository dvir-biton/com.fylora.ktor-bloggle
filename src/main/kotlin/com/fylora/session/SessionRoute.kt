package com.fylora.session

import com.fylora.auth.data.user.User
import com.fylora.bloggle
import com.fylora.session.model.Request
import com.fylora.session.model.Resource
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.bson.types.ObjectId

fun Route.session() {
    webSocket("/connect") {
        val user = call.authentication.principal<User>()

        if(user == null) {
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Authentication required"))
            return@webSocket
        }

        val result = bloggle.addUser(user, this)
        if(result is Resource.Error) {
            close(
                CloseReason(
                    CloseReason.Codes.INTERNAL_ERROR,
                    message = result.message ?: "Unknown error"
                )
            )
            return@webSocket
        }
        val activeUser = result.data ?: return@webSocket

        try {
            incoming.consumeEach { frame ->
                if(frame is Frame.Text) {
                    try {
                        val requestBody = frame.readText()

                        when(val request = Request.fromString(requestBody)) {
                            is Request.MakeComment -> bloggle.comment(
                                activeUser = activeUser,
                                postId = request.postId,
                                body = request.body
                            )
                            is Request.MakePost -> bloggle.post(
                                activeUser = activeUser,
                                body = request.body
                            )
                            is Request.MakeLikeComment -> bloggle.likeComment(
                                activeUser = activeUser,
                                commentId = request.commentId
                            )
                            is Request.MakeLikePost -> bloggle.likePost(
                                activeUser = activeUser,
                                postId = request.postId
                            )
                            is Request.MakeFollow -> bloggle.follow(
                                follower = activeUser,
                                followUserId = request.userId
                            )
                        }
                    } catch (e: IllegalArgumentException) {
                        send(
                            Frame.Text(
                                Json.encodeToString(e.message ?: "Invalid request")
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            bloggle.disconnectUser(activeUser)
        }
    }
}

@Deprecated("will be removed!")
fun Route.test() {
    webSocket("/test") {
        val user = User(
            username = "bloggle",
            password = "f22a7f47c758d76c2f48ec577b8b0d09422b80a5ac27bc351c19a7b4f1d7dffa",
            salt = "635a14dc2a532dabf09b8acaa0e6d38b7f4ffbb14d4436589d9485bd54bae339",
            id = ObjectId("653bd4896e64794ea4c44a30")
        )

        val result = bloggle.addUser(user, this)
        if(result is Resource.Error) {
            close(
                CloseReason(
                    CloseReason.Codes.INTERNAL_ERROR,
                    message = result.message ?: "Unknown error"
                )
            )
            return@webSocket
        }
        val activeUser = result.data ?: return@webSocket

        try {
            incoming.consumeEach { frame ->
                if(frame is Frame.Text) {
                    try {
                        val requestBody = frame.readText()

                        when(val request = Request.fromString(requestBody)) {
                            is Request.MakeComment -> bloggle.comment(
                                activeUser = activeUser,
                                postId = request.postId,
                                body = request.body
                            )
                            is Request.MakePost -> bloggle.post(
                                activeUser = activeUser,
                                body = request.body
                            )
                            is Request.MakeLikeComment -> bloggle.likeComment(
                                activeUser = activeUser,
                                commentId = request.commentId
                            )
                            is Request.MakeLikePost -> bloggle.likePost(
                                activeUser = activeUser,
                                postId = request.postId
                            )
                            is Request.MakeFollow -> bloggle.follow(
                                follower = activeUser,
                                followUserId = request.userId
                            )
                        }
                    } catch (e: IllegalArgumentException) {
                        send(
                            Frame.Text(
                                Json.encodeToString(e.message ?: "Invalid request")
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            bloggle.disconnectUser(activeUser)
        }
    }
}