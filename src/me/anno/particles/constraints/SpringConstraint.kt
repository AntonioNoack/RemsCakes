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
    var breakingDiff: Float,
) : ParticleConstraint {

    override fun solve(p: ParticleSet, dt: Float): Boolean {
        if (breakingDiff <= 0f) return true

        val dx = p.tx[j] - p.tx[i]
        val dy = p.ty[j] - p.ty[i]
        val dz = p.tz[j] - p.tz[i]

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

    fun breakManually() {
        breakingDiff = -1f
    }
}