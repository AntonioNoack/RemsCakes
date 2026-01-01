package me.anno.particles

import org.joml.Vector3f

data class ParticleSolverConfig(
    val substeps: Int = 1,
    val solverIterations: Int = 5,
    val gravity: Vector3f = Vector3f(0f, -9.81f, 0f),
    val damping: Float = 0.999f,
)