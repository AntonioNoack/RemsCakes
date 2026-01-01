package me.anno.particles.constraints

import me.anno.maths.Maths.sq
import me.anno.particles.ParticleSet
import me.anno.particles.constraints.ParticleConstraint.Companion.addT

class BendingConstraint(
    private val i0: Int,
    private val i1: Int,
    private val i2: Int,
    private val stiffness: Float,
    breakingDiff: Float,
    private val linked0: SpringConstraint? = null,
) : ParticleConstraint {

    private val breakingDiffSq = sq(breakingDiff)

    override fun solve(p: ParticleSet, dt: Float): Boolean {
        val dx = p.tx[i1] - (p.tx[i0] + p.tx[i2]) * 0.5f
        val dy = p.ty[i1] - (p.ty[i0] + p.ty[i2]) * 0.5f
        val dz = p.tz[i1] - (p.tz[i0] + p.tz[i2]) * 0.5f

        val w0 = p.invMass[i0]
        val w1 = p.invMass[i1]
        val w2 = p.invMass[i2]

        val wSum = (w0 + w2) * 0.25f + w1
        if (wSum == 0f) return false

        val corr = stiffness * dt / wSum
        if (sq(dx, dy, dz) >= breakingDiffSq) {
            linked0?.breakManually()
            return true
        }

        // Middle particle gets full correction
        p.addT(i1, dx, dy, dz, -corr * w1)

        // Neighbors get half correction
        p.addT(i0, dx, dy, dz, 0.5f * corr * w0)
        p.addT(i2, dx, dy, dz, 0.5f * corr * w2)
        return false
    }
}
