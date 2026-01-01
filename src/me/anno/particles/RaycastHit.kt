package me.anno.particles

data class RaycastHit(
    val hitX: Float,
    val hitY: Float,
    val hitZ: Float,
    val normalX: Float,
    val normalY: Float,
    val normalZ: Float,
    val bodyId: Int
)
