package com.normtronix.dash

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
class WebController {

    @GetMapping("/video_overlay")
    fun dash(model: Model): String {
        return "video_overlay"
    }

    @GetMapping("/config")
    fun config(model: Model): String {
        return "config"
    }

}


