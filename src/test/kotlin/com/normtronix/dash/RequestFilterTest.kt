package com.normtronix.dash

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

import javax.servlet.http.Cookie
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

internal class RequestFilterTest {

    @Test
    fun testDoFilterNoCookies() {
        val filter = RequestFilter()
        val request = mockk< HttpServletRequest>()
        val response = mockk<HttpServletResponse>()
        every { request.servletPath } returns "/admin/foo"
        every { request.cookies } returns null
        every { response.sendRedirect("/auth")} returns Unit
        filter.doFilter(request, response, null)
        verify { response.sendRedirect("/auth") }
    }

    @Test
    fun testDoFilterInvalidCookie() {
        val filter = RequestFilter()
        val request = mockk< HttpServletRequest>()
        val response = mockk<HttpServletResponse>()
        every { request.servletPath } returns "/admin/foo"
        every { request.cookies } returns arrayOf(Cookie("name", "value"))
        every { response.sendRedirect("/auth")} returns Unit
        filter.doFilter(request, response, null)
        verify { response.sendRedirect("/auth") }
    }

    @Test
    fun testDoFilterValidCookie() {
        val filter = RequestFilter()
        filter.authService = mockk()
        val request = mockk< HttpServletRequest>()
        val response = mockk<HttpServletResponse>()
        every { request.servletPath } returns "/admin/foo"
        every { request.cookies } returns arrayOf(Cookie("sessionId", "value"))
        every { filter.authService.isTokenValid(any())} returns true
        filter.doFilter(request, response, null)
        // nothing to verify here
    }
}