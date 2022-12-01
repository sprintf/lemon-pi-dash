package com.normtronix.dash

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling

class DashApplication

fun main(args: Array<String>) {
	runApplication<DashApplication>(*args)
}
