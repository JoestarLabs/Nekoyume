package com.bl4ckswordsman.nekoyume.core.audio

import androidx.media3.common.Player
import java.lang.reflect.Proxy

/**
 * Creates a dynamic proxy stub of [Player] with default no-op behavior for local unit testing.
 */
fun FakePlayerStub(): Player {
    return Proxy.newProxyInstance(
        Player::class.java.classLoader,
        arrayOf(Player::class.java)
    ) { _, method, _ ->
        when (method.returnType) {
            Boolean::class.javaPrimitiveType -> false
            Int::class.javaPrimitiveType -> 0
            Long::class.javaPrimitiveType -> 0L
            Float::class.javaPrimitiveType -> 1.0f
            Void.TYPE -> null
            else -> null
        }
    } as Player
}
