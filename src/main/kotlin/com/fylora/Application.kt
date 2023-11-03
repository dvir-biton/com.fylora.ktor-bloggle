package com.fylora

import com.fylora.auth.data.user.MongoUserDataSource
import com.fylora.auth.security.hashing.SHA256HashingService
import com.fylora.auth.security.token.JwtTokenService
import com.fylora.auth.security.token.TokenConfig
import com.fylora.plugins.configureMonitoring
import com.fylora.plugins.configureRouting
import com.fylora.plugins.configureSecurity
import com.fylora.plugins.configureSerialization
import com.fylora.session.Bloggle
import io.ktor.server.application.*
import io.ktor.server.websocket.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo

val bloggle = Bloggle()

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

@OptIn(DelicateCoroutinesApi::class)
@Suppress("Unused")
fun Application.module() {
    val mongoPassword = System.getenv("MONGO_PASSWORD")
    val dbName = "ktor-login"
    val db = KMongo.createClient(
        connectionString = "mongodb+srv://fylora:$mongoPassword@users.q9zqet0.mongodb.net/$dbName?retryWrites=true&w=majority"
    ).coroutine
        .getDatabase(dbName)
    val userDataSource = MongoUserDataSource(db)

    GlobalScope.launch {
        bloggle.makeAccounts(userDataSource.getAllUsers())
    }

    val tokenService = JwtTokenService()
    val tokenConfig = TokenConfig(
        issuer = environment.config.property("jwt.issuer").getString(),
        audience = environment.config.property("jwt.audience").getString(),
        expiresIn = 2629746000L, // 1 month
        secret = System.getenv("JWT_SECRET")
    )
    val hashingService = SHA256HashingService()

    install(WebSockets)
    configureMonitoring()
    configureSerialization()
    configureSecurity(tokenConfig)
    configureRouting(userDataSource, hashingService, tokenService, tokenConfig)
}
