package com.normtronix.dash

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
class WebController {

    @Value("\${mapsApiKey}")
    lateinit var mapsApiKey:String

    @GetMapping("/live_track_map")
    fun trackMap(@RequestParam(name="track") track: String,
                 model: Model): String {
        model.addAttribute("trackCode", track)
        model.addAttribute("maps_api_key", mapsApiKey)
        return "live_track_map"
    }

}


