package com.codingcat.changelogs.platformapi.event.util

import com.codingcat.changelogs.platformapi.event.PlatformEventManager
import com.codingcat.changelogs.platformapi.event.impl.PlatformEvent
import java.lang.reflect.InvocationTargetException

/**
 * Interface enabling reflective discovery and binding of methods annotated
 * with [ListenerMethod] to a [PlatformEventManager].
 */
interface MethodDispatchingListener {
    /**
     * Inspects declared methods on this instance and registers those annotated with [ListenerMethod] to [eventManager].
     *
     * @throws IllegalArgumentException if an annotated method does not take exactly one [PlatformEvent] parameter.
     */
    fun setupAll(eventManager: PlatformEventManager) {
        javaClass.declaredMethods.forEach { method ->
            if (!method.isAnnotationPresent(ListenerMethod::class.java)) return@forEach

            require(method.parameterCount == 1) {
                "Listener method ${method} has an invalid amount of parameters"
            }

            val paramType = method.parameterTypes[0]
            require(paramType != PlatformEvent::class.java && PlatformEvent::class.java.isAssignableFrom(paramType)) {
                buildString {
                    append("Listener method ${method} parameter has to be a valid implementation ")
                    append("of PlatformEvent, got ${paramType} instead")
                }
            }

            method.isAccessible = true

            val handler = object : IdentifiableEventListener<PlatformEvent> {
                override val origin: MethodDispatchingListener = this@MethodDispatchingListener

                override fun invoke(event: PlatformEvent) {
                    try {
                        method.invoke(this@MethodDispatchingListener, event)
                    } catch (e: ReflectiveOperationException) {
                        val cause = if (e is InvocationTargetException) e.cause ?: e else e
                        throw RuntimeException(
                            "Failed to invoke method dispatching listener method ${method} for event ${event}",
                            cause,
                        )
                    }
                }
            }

            registerEventHelper<PlatformEvent>(eventManager, paramType, handler)
        }
    }

    /**
     * Unregisters all listener subscriptions registered by this instance from [eventManager].
     */
    fun unregisterAll(eventManager: PlatformEventManager) {
        eventManager.unregisterIf { listener ->
            listener is IdentifiableEventListener<*> && listener.origin === this
        }
    }

    /**
     * Marks a function within a [MethodDispatchingListener] as an event handler method.
     */
    @Target(AnnotationTarget.FUNCTION)
    @Retention(AnnotationRetention.RUNTIME)
    annotation class ListenerMethod

    /**
     * Listener token tracking the originating [MethodDispatchingListener] instance for teardown filtering.
     */
    interface IdentifiableEventListener<T : PlatformEvent> : (T) -> Unit {
        val origin: MethodDispatchingListener
    }
}

@Suppress("UNCHECKED_CAST")
private fun <T : PlatformEvent> registerEventHelper(
    eventManager: PlatformEventManager,
    eventType: Class<*>,
    handler: MethodDispatchingListener.IdentifiableEventListener<PlatformEvent>,
) {
    eventManager.registerListener(
        eventType as Class<T>,
        handler as (T) -> Unit,
    )
}
