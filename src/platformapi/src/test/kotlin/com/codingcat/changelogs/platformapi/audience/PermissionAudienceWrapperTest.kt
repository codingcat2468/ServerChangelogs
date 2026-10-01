package com.codingcat.changelogs.platformapi.audience

import net.kyori.adventure.audience.Audience
import net.kyori.adventure.permission.PermissionChecker
import net.kyori.adventure.util.TriState
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class PermissionAudienceWrapperTest {
    private class TestWrapper(
        private val permissions: Map<String, TriState>,
    ) : PermissionAudienceWrapper {
        private val audience = Audience.empty()
        private val checker = PermissionChecker { permission ->
            permissions[permission] ?: TriState.NOT_SET
        }

        override fun asAudience(): Audience = audience
        override fun asPermissionChecker(): PermissionChecker = checker
    }

    @Test
    fun testPermissionEvaluation() {
        val wrapper = TestWrapper(
            mapOf(
                "test.granted" to TriState.TRUE,
                "test.denied" to TriState.FALSE,
            ),
        )

        assertTrue(wrapper.hasPermission("test.granted"))
        assertFalse(wrapper.hasPermission("test.denied"))
        assertFalse(wrapper.hasPermission("test.unset"))

        assertTrue("test.granted" in wrapper)
        assertFalse("test.denied" in wrapper)
        assertFalse("test.unset" in wrapper)

        assertTrue(wrapper.hasPermission("test.unset", trueIfUnset = true))
        assertFalse(wrapper.hasPermission("test.unset", trueIfUnset = false))
        assertFalse(wrapper.hasPermission("test.denied", trueIfUnset = true))
        assertTrue(wrapper.hasPermission("test.granted", trueIfUnset = true))
    }

    @Test
    fun testAudienceAndCheckerAccessors() {
        val wrapper = TestWrapper(emptyMap())
        assertSame(wrapper.asAudience(), wrapper.asAudience())
        assertSame(wrapper.asPermissionChecker(), wrapper.asPermissionChecker())
    }
}
