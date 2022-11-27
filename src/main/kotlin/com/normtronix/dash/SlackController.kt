package com.normtronix.dash

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*
import java.text.ParseException
import java.util.regex.Pattern

data class SlackCommand(
    val token: String,
    val team_id: String,
    val api_app_id: String,
    val command: String,
    val text: String,
    val response_url: String?,
    val trigger_id: String?,
    val user_id: String,
    val channel_id: String,
    val channel_name: String
    )

data class SlackCommandResponse(
    val response_type: String,
    val text: String
)

@Controller
class SlackController {

    @Autowired
    lateinit var meringue: MeringueConnector

    @PostMapping("/slack-command")
    @ResponseBody
    suspend fun handleCommand(rq: SlackCommand): ResponseEntity<SlackCommandResponse> {
        log.info("handling command $rq")
        if (rq.api_app_id != "A046XRKCGN7") {
            throw RuntimeException("not from Lemon-Pi Slack Integration")
        }
        when (rq.command) {
            "/reset-fast-lap" -> {
                if (rq.text == "help") {
                    return ResponseEntity.ok(
                        SlackCommandResponse(
                            "ephemeral",
                            "no arguments needed, this resets the fastest lap time shown to the driver."
                        )
                    )
                }
                return when (meringue.resetFastLapTime("thil", "8")) {
                    true -> {
                        ResponseEntity.ok(SlackCommandResponse(
                            "in_channel",
                            "fast lap reset sent (and probably delivered)"
                        ))
                    }
                    else -> {
                        ResponseEntity.ok(SlackCommandResponse(
                            "in_channel",
                            "Nopety-nope. Car not connected, or something went wonky"
                        ))
                    }
                }
            }
            "/set-target-time" -> {
                if (rq.text == "help") {
                    return ResponseEntity.ok(
                        SlackCommandResponse(
                            "ephemeral",
                            "send minutes and seconds as mm:ss e.g  2:20"
                        )
                    )
                }
                try {
                    getTimeInSecs(rq.text).apply {
                        return when(meringue.setTargetTime("thil", "8", this)) {
                            true -> {
                                ResponseEntity.ok(
                                    SlackCommandResponse(
                                        "in_channel",
                                        "target time sent (and probably delivered)"
                                    )
                                )
                            }
                            else -> {
                                ResponseEntity.ok(
                                    SlackCommandResponse(
                                        "in_channel",
                                        "Failed. Car not online or something else wonky. Might be worth a retry."
                                    )
                                )
                            }
                        }
                    }
                } catch (e: ParseException) {
                    return ResponseEntity.ok(
                        SlackCommandResponse(
                            "ephemeral",
                            "supply target time in minutes:seconds format e.g 2:10"
                        )
                    )
                }
            }
            "/send-driver-message" -> {
                if (rq.text == "help") {
                    return ResponseEntity.ok(
                        SlackCommandResponse(
                            "ephemeral",
                            "sends a text message to the driver. e.g. /send-driver-message pit in 3 laps"
                        )
                    )
                }
                return when(meringue.sendDriverMessage("thil", "8", rq.text)) {
                    true -> {
                        ResponseEntity.ok(
                            SlackCommandResponse(
                                "in_channel",
                                "Message sent to driver (and probably delivered, but not a chance they saw it)"
                            )
                        )
                    }
                    else -> {
                        ResponseEntity.ok(
                            SlackCommandResponse(
                                "in_channel",
                                "Yeah ... not so much. Don't think the car is online. You could try again in a few."
                            )
                        )
                    }
                }
            }
            else -> {
                return ResponseEntity.ok(
                    SlackCommandResponse(
                        "ephemeral",
                        "unrecognized command"
                    )
                )
            }
        }
    }

    internal fun getTimeInSecs(text: String): Int {
        val matcher = Pattern.compile("(\\d{1,2}):(\\d{1,2})").matcher(text.trim())
        if (!matcher.matches()) {
            throw ParseException("not mm:ss target time", 0)
        }
        return matcher.group(1).toInt() * 60 + matcher.group(2).toInt()
    }

    companion object {
        val log: Logger = LoggerFactory.getLogger(SlackController::class.java)
    }
}