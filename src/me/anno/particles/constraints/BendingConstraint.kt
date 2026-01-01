package me.anno.particles.constraints

import me.anno.maths.Maths.clamp
import me.anno.particles.ParticleSet

class BendingConstraint(
    private val i0: Int,
    private val i1: Int,
    private val i2: Int,
    private val stiffness: Float
) : ParticleConstraint {

    override fun solve(p: ParticleSet, dt: Float) {
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
        if (wSum == 0f) return

        val corr = clamp(stiffness / wSum * dt, -1f, 1f)

        // Middle particle gets full correction
        p.tx[i1] -= dx * corr * w1
        p.ty[i1] -= dy * corr * w1
        p.tz[i1] -= dz * corr * w1

        // Neighbors get half correction
        val hx = dx * 0.5f * corr
        val hy = dy * 0.5f * corr
        val hz = dz * 0.5f * corr

        p.tx[i0] += hx * w0
        p.ty[i0] += hy * w0
        p.tz[i0] += hz * w0

        p.tx[i2] += hx * w2
        p.ty[i2] += hy * w2
        p.tz[i2] += hz * w2
    }
}
