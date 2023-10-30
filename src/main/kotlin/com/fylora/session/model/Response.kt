package com.fylora.session.model

import com.fylora.session.notifications.Notification
import kotlinx.serialization.Serializable

@Serializable
sealed interface Response {
    data class PostResponse(val post: Post): Response
    data class PostsResponse(val posts: List<Post>): Response
    data class NotificationsResponse(val notification: List<Notification>): Response
    data class AccountResponse(val account: Account): Response
    data class AccountsResponse(val account: List<Account>): Response
    data class ConfirmationResponse(val confirmation: String): Response
    data class ErrorResponse(val error: String): Response
}