package com.normtronix.dash

import com.google.cloud.firestore.Firestore
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

private const val DASH_WEBAUTH = "lemon_pi_dash_webauth"

@Service
class AuthService {

    @Autowired
    lateinit var db: Firestore

    fun createTokenForUser(username: String): String {
        val token = UUID.randomUUID().toString()
        db.collection(DASH_WEBAUTH).document(token).create(mapOf("ts" to Instant.now())).get()
        return token
    }

    fun isTokenValid(token: String?): Boolean {
        return when (token) {
            null -> throw RuntimeException()
            else -> db.collection(DASH_WEBAUTH).document(token).get().get().exists()
        }
    }
}