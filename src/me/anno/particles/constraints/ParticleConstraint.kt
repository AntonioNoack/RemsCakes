package me.anno.particles.constraints

import me.anno.particles.ParticleSet

interface ParticleConstraint {
    fun solve(p: ParticleSet, dt: Float)
}