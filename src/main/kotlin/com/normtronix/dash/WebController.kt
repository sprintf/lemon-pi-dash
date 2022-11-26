package com.normtronix.dash

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.env.Environment
import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.ModelMap
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.servlet.ModelAndView
import java.util.*
import javax.servlet.http.Cookie
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


data class LoginData(
    val username: String,
    val password: String,
)

@Controller
class WebController {

    @Autowired
    lateinit var trackLoader: TrackMetaDataLoader

    @Autowired
    lateinit var adminCreds: AdminCredentialProvider

    @Autowired
    lateinit var authService: AuthService

    @Autowired
    lateinit var meringue: MeringueConnector

    @Autowired
    var env: Environment? = null

    @Value("\${mapsApiKey}")
    lateinit var mapsApiKey:String

    @GetMapping("/live_track_map")
    fun trackMap(@RequestParam(name="track") track: String,
                 model: Model): String {
        trackLoader.validateTrackCode(track)
        val latLong = trackLoader.getLatLong(track)
        model.addAttribute("lat", latLong.first)
        model.addAttribute("long", latLong.second)
        model.addAttribute("trackCode", track)
        model.addAttribute("maps_api_key", mapsApiKey)
        return "live_track_map"
    }

    @GetMapping("/")
    fun redirectToAuth(model: ModelMap): ModelAndView {
        return ModelAndView("redirect:/auth", model)
    }

    @GetMapping("/auth")
    fun auth(request: HttpServletRequest, model: Model): String {

        val sessionCookie = Arrays.stream(request.cookies ?: emptyArray())
                .filter { it.name == "sessionId"}.findFirst()
        if (sessionCookie.isPresent && authService.isTokenValid(sessionCookie.get().value)) {
            return "redirect:/admin/track_list"
        }
        return "auth"
    }

    @PostMapping("/doLogin")
    suspend fun login(@ModelAttribute rq: LoginData, request: HttpServletRequest, response: HttpServletResponse, model: Model): ModelAndView {
        log.info("login request")
        if (rq.username == adminCreds.adminUsername &&
                rq.password == adminCreds.adminPassword) {
            response.addCookie(Cookie("sessionId", authService.createTokenForUser(rq.username)).apply {
                val isDev = env?.activeProfiles?.contains("dev")?:false
                this.secure = !isDev
                this.maxAge = 3600 * 48 // 2 days
            })
            return ModelAndView("redirect:/admin/track_list", ModelMap())
        }
        return ModelAndView("redirect:/auth", ModelMap())
    }

    @GetMapping("/admin/track_list")
    suspend fun showTrackList(model: Model): String {
        model.addAttribute("tracks", meringue.listTracksAndRaces())
        return "admin/track_list"
    }

    @GetMapping("/admin/choose_track")
    suspend fun chooseTrack(@RequestParam(name="trackCode") trackCode: String,
                    model: Model): String {
        if (trackLoader.isValidTrackCode(trackCode)) {
            val trackName = trackLoader.name(trackCode)
            model.addAttribute("trackName", trackName)
            model.addAttribute("trackCode", trackCode)
            model.addAttribute("races", meringue.getLiveRaces(trackName!!).racesList)
            return "admin/active_races"
        }
        return "error"
    }

    @GetMapping("/admin/connect")
    suspend fun chooseTrack(@RequestParam(name="trackCode") trackCode: String,
                            @RequestParam(name="provider") provider: String,
                            @RequestParam(name="providerId") providerId: String,
                            model: Model): ModelAndView {
        log.info("called connect with $trackCode $provider $providerId")
        if (trackLoader.isValidTrackCode(trackCode)) {
            meringue.connectToRace(trackCode, provider, providerId)
            return ModelAndView("redirect:/admin/track_list", ModelMap())
        }
        return ModelAndView("error")
    }

    @GetMapping("/admin/disconnect")
    suspend fun disconnectRace(@RequestParam(name="handle") handle:String,
                               model: Model): ModelAndView {
        log.info("called disconnect with $handle")
        meringue.disconnectRace(handle)
        return ModelAndView("redirect:/admin/track_list", ModelMap())
    }

    @GetMapping("/admin/car_message")
    suspend fun carMessage(@RequestParam(name="track") trackCode: String,
                            model: Model) : ModelAndView {
        if (trackLoader.isValidTrackCode(trackCode)) {
            return ModelAndView("/admin/car_message", buildCarMessageContext(trackCode))
        }
        return ModelAndView("error")
    }

    private suspend fun buildCarMessageContext(trackCode: String): MutableMap<String, Any> {
        val model = mutableMapOf<String, Any>()
        model["trackName"] = trackLoader.codeToName(trackCode)
        model["trackCode"] = trackCode
        val listConnectedCars = meringue.listConnectedCars(trackCode)
        model["cars"] = listConnectedCars
        model["disabled"] = listConnectedCars.isEmpty()
        return model
    }

    data class CarMessageFormData(
        val trackCode: String,
        val carNumber: String,
        var message: String
    )

    @PostMapping("/admin/car_message", consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE])
    suspend fun carMessageHandler(formData: CarMessageFormData) : ModelAndView {
        if (trackLoader.isValidTrackCode(formData.trackCode)) {
            if (formData.carNumber.isEmpty()) {
                return ModelAndView("error")
            }
            if (formData.message.isEmpty()) {
                return ModelAndView("error")
            }
            meringue.sendDriverMessage(formData.trackCode, formData.carNumber, formData.message)
            val context = buildCarMessageContext(formData.trackCode)
            context["message"] = "message sent"
            return ModelAndView("/admin/car_message", context)
        }
        return ModelAndView("error")
    }

    companion object {
        val log: Logger = LoggerFactory.getLogger(WebController::class.java)
    }

}


