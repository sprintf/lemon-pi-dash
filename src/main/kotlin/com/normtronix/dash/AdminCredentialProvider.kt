package com.normtronix.dash

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class AdminCredentialProvider {

    @Value("\${adminUsername}")
    lateinit var adminUsername:String

    @Value("\${adminPassword}")
    lateinit var adminPassword:String

}