package com.normtronix.dash

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.FirestoreOptions
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class FirestoreConfiguration {

    @Bean
    fun getFirestore() : Firestore {
        val firestoreOptions = FirestoreOptions.getDefaultInstance().toBuilder()
            .setProjectId("meringue")
            .setCredentials(GoogleCredentials.getApplicationDefault())
            .build()
        return firestoreOptions.getService()
    }

}