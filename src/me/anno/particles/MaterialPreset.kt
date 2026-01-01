package me.anno.particles

enum class MaterialPreset(
    val staticFriction: Float,
    val dynamicFriction: Float,
    val cohesion: Float
) {
    DRY_SAND(0.6f, 0.4f, 0f),
    WET_SAND(0f, 0f, 0.02f),
    DOUGH(0.05f, 1f, 0.8f),
}