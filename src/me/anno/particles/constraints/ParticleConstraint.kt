package me.anno.particles.constraints

import me.anno.particles.ParticleSet

interface ParticleConstraint {
    /**
     * return whether the constraint was broken;
     * todo in the future, we will have to split meshes, which constraints break
     * */
    fun solve(p: ParticleSet, dt: Float): Boolean

    companion object {
        fun ParticleSet.addT(k: Int, vx: Float, vy: Float, vz: Float, scale: Float) {
            tx[k] += vx * scale
            ty[k] += vy * scale
            tz[k] += vz * scale
        }
    }
}