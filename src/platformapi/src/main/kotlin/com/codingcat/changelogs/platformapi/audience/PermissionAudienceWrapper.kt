package com.codingcat.changelogs.platformapi.audience

import net.kyori.adventure.audience.Audience
import net.kyori.adventure.permission.PermissionChecker
import net.kyori.adventure.util.TriState

/**
 * Compound contract combining Adventure [Audience] message reception with [PermissionChecker] validation.
 */
interface PermissionAudienceWrapper {
    /**
     * Views this receiver as an Adventure [Audience].
     */
    fun asAudience(): Audience

    /**
     * Views this receiver as an Adventure [PermissionChecker].
     */
    fun asPermissionChecker(): PermissionChecker

    /**
     * Checks whether the receiver holds the specified permission node.
     */
    fun hasPermission(permission: String): Boolean {
        return asPermissionChecker().test(permission)
    }

    /**
     * Evaluates permission presence while treating unset ([TriState.NOT_SET]) status
     * as granted if [trueIfUnset] is true.
     */
    fun hasPermission(permission: String, trueIfUnset: Boolean): Boolean {
        val value = asPermissionChecker().value(permission)
        return value == TriState.TRUE || (trueIfUnset && value == TriState.NOT_SET)
    }

    /**
     * Operator overload allowing idiomatic Kotlin `'permission.node' in source` checks.
     */
    operator fun contains(permission: String): Boolean = hasPermission(permission)
}
