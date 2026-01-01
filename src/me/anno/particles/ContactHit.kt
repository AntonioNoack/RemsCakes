package me.anno.particles

data class ContactHit(
    val pointX: Float,
    val pointY: Float,
    val pointZ: Float,
    val normalX: Float,
    val normalY: Float,
    val normalZ: Float,
    val bodyId: Int
)
