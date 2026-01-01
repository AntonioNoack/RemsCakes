package me.anno.particles.constraints

import me.anno.particles.ParticleSet

interface ParticleConstraint {
    /**
     * return whether the constraint was broken;
     * todo in the future, we will have to split meshes, which constraints break
     * */
    fun solve(p: ParticleSet, dt: Float): Boolean

    companion object {
        fun ParticleSet.addT(k: Int, nx: Float, ny: Float, nz: Float, scale: Float) {
            tx[k] += nx * scale
            ty[k] += ny * scale
            tz[k] += nz * scale
        }

        fun ParticleSet.addV(k: Int, nx: Float, ny: Float, nz: Float, scale: Float) {
            addV(k, nx * scale, ny * scale, nz * scale)
        }

        fun ParticleSet.addV(k: Int, nx: Float, ny: Float, nz: Float) {
            vx[k] += nx
            vy[k] += ny
            vz[k] += nz
        }
    }
}