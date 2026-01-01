package me.anno.particles.constraints

import me.anno.maths.Maths.clamp
import me.anno.particles.ParticleSet
import kotlin.math.sqrt

class SpringConstraint(
    private val i: Int,
    private val j: Int,
    private val restLength: Float,
    private val stiffness: Float = 1.0f
) : ParticleConstraint {

    override fun solve(p: ParticleSet, dt: Float) {
        val ix = p.tx[i]
        val iy = p.ty[i]
        val iz = p.tz[i]

        val jx = p.tx[j]
        val jy = p.ty[j]
        val jz = p.tz[j]

        val dx = jx - ix
        val dy = jy - iy
        val dz = jz - iz

        val distSq = dx * dx + dy * dy + dz * dz
        if (distSq < 1e-10f) return

        val dist = sqrt(distSq)
        val diff = (dist - restLength) / dist

        var w1 = p.invMass[i]
        var w2 = p.invMass[j]
        val wSum = w1 + w2
        if (wSum == 0f) return

        val corr = clamp(stiffness * dt * diff, -0.25f, 0.25f)

        val cx = dx * corr
        val cy = dy * corr
        val cz = dz * corr

        w1 /= wSum
        w2 /= wSum

        p.tx[i] += cx * w1
        p.ty[i] += cy * w1
        p.tz[i] += cz * w1

        p.tx[j] -= cx * w2
        p.ty[j] -= cy * w2
        p.tz[j] -= cz * w2
    }
}