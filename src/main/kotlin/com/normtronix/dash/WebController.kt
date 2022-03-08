package com.normtronix.dash

import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
class WebController {

    @GetMapping("/landing")
    fun landing(@RequestParam(name="track") track: String,
                @RequestParam(name="car") car: String,
                model: Model): String {
        //todo : call grpc server to get answer
        model.addAttribute("speed", "40")
        return "landing"
    }

    @GetMapping("/video_overlay")
    fun dash(model: Model): String {
        return "video_overlay"
    }

    @GetMapping("/config")
    fun config(model: Model): String {
        return "config"
    }

}