package com.promethist.common

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

internal class ServiceUrlResolverTest {
    @Test
    fun `local core endpoint`() {
        val url = ServiceUrlResolver.getEndpointUrl("core", ServiceUrlResolver.RunMode.local, namespace = null)
        assertEquals("http://localhost:8080", url)
    }
}