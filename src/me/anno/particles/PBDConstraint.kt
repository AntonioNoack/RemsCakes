package me.anno.particles

interface PBDConstraint {
    fun solve(p: ParticleSet)
}