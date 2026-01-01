package me.anno.particles.constraints

import me.anno.particles.ParticleSet
import me.anno.particles.constraints.ParticleConstraint.Companion.addT
import kotlin.math.abs
import kotlin.math.sqrt

class SpringConstraint(
    private val i: Int,
    private val j: Int,
    private val restLength: Float,
    private val stiffness: Float,
    private val breakingDiff: Float,
) : ParticleConstraint {

    override fun solve(p: ParticleSet, dt: Float): Boolean {
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
        if (distSq < 1e-10f) return false

        val dist = sqrt(distSq)
        val diffLen = dist - restLength
        if (abs(diffLen) >= breakingDiff) return true

        val diffRel = diffLen / dist

        val w1 = p.invMass[i]
        val w2 = p.invMass[j]
        val wSum = w1 + w2
        if (wSum == 0f) return false

        val corr = stiffness * dt * diffRel / wSum
        p.addT(i, dx, dy, dz, +w1 * corr)
        p.addT(j, dx, dy, dz, -w2 * corr)
        return false
    }
}