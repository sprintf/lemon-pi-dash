package com.normtronix.dash

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

data class TimeResponse(val hour: Int, val minute: Int, val second: Int)

@RestController
class RestController {

    @GetMapping("/time")
    fun time():TimeResponse {
        val now = LocalDateTime.now()
        return TimeResponse(now.hour, now.minute,now.second)
    }
}