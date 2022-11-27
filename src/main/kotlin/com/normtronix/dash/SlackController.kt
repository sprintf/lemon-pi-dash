package com.normtronix.dash

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*
import java.text.ParseException
import java.util.regex.Pattern
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

data class SlackEvent(
    val type: String,
    val event_ts: Double,
    val user: String
)

data class SlackAuthorization(
    val enterprise_id: String,
    val team_id: String,
    val user_id: String,
    val is_bot: Boolean
)

data class SlackRequest(
    val token: String,
    val type: String,

    // just for challenge
    val challenge: String?,

    // other general fields
    val team_id: String?,
    val api_app_id: String?,
    val event: SlackEvent?,
    val authed_users: List<String>?,
    val authed_teams: List<String>?,
    val authorizations: List<SlackAuthorization>?,
    val event_context: String?,
    val event_id: String?,
    val event_time: Int?

)

data class SlackResponse(
    val challenge: String?
)

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

    @PostMapping("/slack")
    @ResponseBody
    suspend fun handleRequest(@RequestBody rq: SlackRequest, request: HttpServletRequest, response: HttpServletResponse, model: Model): SlackResponse {
        log.info("handling slack request $rq")
        if (rq.api_app_id != "A046XRKCGN7") {
            throw RuntimeException("not from Lemon-Pi")
        }
        // todo : check its for us maybe
        when (rq.type) {
            "url_verification" -> {
                return SlackResponse(rq.challenge)
            }
            "event_callback" -> {

            }
            "ssl_check" -> {
                return SlackResponse(null)
            }
            else -> { }
        }
        return SlackResponse(null)
    }

    @PostMapping("/slack-command")
    @ResponseBody
    suspend fun handleCommand(rq: SlackCommand): ResponseEntity<SlackCommandResponse> {
        log.info("handling command $rq")
        if (rq.api_app_id != "A046XRKCGN7") {
            throw RuntimeException("not from Lemon-Pi")
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
                meringue.resetFastLapTime("thil", "8")
                return ResponseEntity.ok(
                    SlackCommandResponse(
                        "in_channel",
                         "fast lap reset sent"
                    )
                )
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
                        meringue.setTargetTime("thil", "8", this)
                        return ResponseEntity.ok(
                            SlackCommandResponse(
                                "in_channel",
                                "target time sent"
                            )
                        )
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