package me.anno.particles.constraints

import me.anno.maths.Maths.sq
import me.anno.particles.ParticleSet
import me.anno.particles.constraints.ParticleConstraint.Companion.addT

class BendingConstraint(
    private val i0: Int,
    private val i1: Int,
    private val i2: Int,
    private val stiffness: Float,
    breakingDiff: Float
) : ParticleConstraint {

    private val breakingDiffSq = sq(breakingDiff)

    override fun solve(p: ParticleSet, dt: Float): Boolean {
        val x0 = p.tx[i0]
        val y0 = p.ty[i0]
        val z0 = p.tz[i0]

        val x1 = p.tx[i1]
        val y1 = p.ty[i1]
        val z1 = p.tz[i1]

        val x2 = p.tx[i2]
        val y2 = p.ty[i2]
        val z2 = p.tz[i2]

        // Target midpoint
        val mx = (x0 + x2) * 0.5f
        val my = (y0 + y2) * 0.5f
        val mz = (z0 + z2) * 0.5f

        val dx = x1 - mx
        val dy = y1 - my
        val dz = z1 - mz

        val w0 = p.invMass[i0]
        val w1 = p.invMass[i1]
        val w2 = p.invMass[i2]

        val wSum = w0 * 0.25f + w1 + w2 * 0.25f
        if (wSum == 0f) return false

        val corr = stiffness / wSum * dt
        if (sq(dx, dy, dz) >= breakingDiffSq) return true

        // Middle particle gets full correction
        p.addT(i1, dx, dy, dz, -corr * w1)

        // Neighbors get half correction
        p.addT(i0, dx, dy, dz, 0.5f * corr * w0)
        p.addT(i2, dx, dy, dz, 0.5f * corr * w2)
        return false
    }
}
