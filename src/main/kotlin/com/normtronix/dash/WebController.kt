package com.normtronix.dash

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.ModelMap
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.ModelAndView
import javax.servlet.http.Cookie
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
    fun auth(model: Model): String {
        return "auth"
    }

    @PostMapping("/doLogin")
    suspend fun login(@ModelAttribute request: LoginData, response: HttpServletResponse, model: Model): ModelAndView {
        log.info("login request")
        if (request.username == adminCreds.adminUsername &&
                request.password == adminCreds.adminPassword) {
            response.addCookie(Cookie("sessionId", authService.createTokenForUser(request.username)))
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

    companion object {
        val log: Logger = LoggerFactory.getLogger(WebController::class.java)
    }

}


