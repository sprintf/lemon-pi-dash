package com.normtronix.dash

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import java.util.*
import javax.servlet.Filter
import javax.servlet.FilterChain
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
@Order(1)
class RequestFilter : Filter {

    @Autowired
    lateinit var authService: AuthService

    override fun doFilter(request: ServletRequest?, response: ServletResponse?, chain: FilterChain?) {
        val httpRequest = request as HttpServletRequest
        val httpResponse = response as HttpServletResponse
        if (httpRequest.servletPath.startsWith("/admin")) {
            val sessionCookie = Arrays.stream(httpRequest.cookies).
                filter { it.name == "sessionId"}.findFirst()
            if (!sessionCookie.isPresent || !authService.isTokenValid(sessionCookie.get().value)) {
                httpResponse.sendRedirect("/auth")
            }
        }
        chain?.doFilter(request, response)
    }
}