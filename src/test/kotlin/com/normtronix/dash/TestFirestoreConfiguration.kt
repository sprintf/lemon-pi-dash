package com.normtronix.dash

import com.google.cloud.firestore.Firestore
import io.mockk.mockk
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

@TestConfiguration
class TestFirestoreConfiguration {

    @Bean
    fun getFirestore() : Firestore {
        return mockk()
    }

}