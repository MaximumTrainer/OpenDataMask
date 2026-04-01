package com.opendatamask.security

import jakarta.servlet.FilterChain
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.User

@ExtendWith(MockitoExtension::class)
class JwtAuthenticationFilterTest {

    @Mock private lateinit var jwtTokenProvider: JwtTokenProvider
    @Mock private lateinit var userDetailsService: UserDetailsServiceImpl
    @Mock private lateinit var filterChain: FilterChain

    @InjectMocks
    private lateinit var filter: JwtAuthenticationFilter

    @BeforeEach
    fun clearSecurityContext() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `valid token sets authentication in security context`() {
        val request = MockHttpServletRequest()
        request.addHeader("Authorization", "Bearer valid.token.here")
        val response = MockHttpServletResponse()

        val userDetails = User("alice", "password", listOf(SimpleGrantedAuthority("ROLE_USER")))
        whenever(jwtTokenProvider.validateToken("valid.token.here")).thenReturn(true)
        whenever(jwtTokenProvider.getUsernameFromToken("valid.token.here")).thenReturn("alice")
        whenever(userDetailsService.loadUserByUsername("alice")).thenReturn(userDetails)

        filter.doFilter(request, response, filterChain)

        val auth = SecurityContextHolder.getContext().authentication
        assertNotNull(auth)
        assertTrue(auth.isAuthenticated)
        assertEquals("alice", (auth.principal as org.springframework.security.core.userdetails.UserDetails).username)
        verify(filterChain).doFilter(request, response)
    }

    @Test
    fun `invalid token does not set authentication`() {
        val request = MockHttpServletRequest()
        request.addHeader("Authorization", "Bearer invalid.token")
        val response = MockHttpServletResponse()

        whenever(jwtTokenProvider.validateToken("invalid.token")).thenReturn(false)

        filter.doFilter(request, response, filterChain)

        assertNull(SecurityContextHolder.getContext().authentication)
        verify(filterChain).doFilter(request, response)
        verify(jwtTokenProvider, never()).getUsernameFromToken(any())
    }

    @Test
    fun `missing Authorization header continues filter chain without authentication`() {
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, filterChain)

        assertNull(SecurityContextHolder.getContext().authentication)
        verify(filterChain).doFilter(request, response)
        verify(jwtTokenProvider, never()).validateToken(any())
    }

    @Test
    fun `non-Bearer Authorization header is ignored`() {
        val request = MockHttpServletRequest()
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz")
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, filterChain)

        assertNull(SecurityContextHolder.getContext().authentication)
        verify(filterChain).doFilter(request, response)
        verify(jwtTokenProvider, never()).validateToken(any())
    }
}
