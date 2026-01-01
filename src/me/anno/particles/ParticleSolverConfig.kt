package me.anno.particles

data class ParticleSolverConfig(
    val substeps: Int = 1,
    val solverIterations: Int = 5,
    val gravityX: Float = 0f,
    val gravityY: Float = -9.81f,
    val gravityZ: Float = 0f,
    val damping: Float = 0.999f,
)