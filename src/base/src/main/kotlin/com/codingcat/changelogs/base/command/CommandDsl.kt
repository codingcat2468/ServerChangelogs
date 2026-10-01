package com.codingcat.changelogs.base.command

/**
 * DSL marker for Brigadier command building blocks, preventing scope leakage in nested builders.
 *
 * @author GuavaDealer
 * @since Kotlin Migration
 */
@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class CommandDsl
