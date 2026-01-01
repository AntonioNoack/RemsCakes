package me.anno.particles

import kotlin.math.sqrt

class PBDSolver(
    private val particles: ParticleSet,
    private val constraints: MutableList<PBDConstraint>,
    private val contactSolver: ParticleContactSolver,
    private val rigidContacts: ParticleRigidContactSolver,
    private val config: PBDSolverConfig
) {

    fun step(dt: Float) {
        val h = dt / config.substeps

        repeat(config.substeps) {
            applyExternalForces(h)
            predictPositions(h)
            solveConstraints()
            updateVelocities(h)
        }
    }

    private fun applyExternalForces(dt: Float) {
        for (i in 0 until particles.size) {
            if (particles.invMass[i] == 0f) continue

            particles.vx[i] += config.gravityX * dt
            particles.vy[i] += config.gravityY * dt
            particles.vz[i] += config.gravityZ * dt
        }
    }

    private fun predictPositions(dt: Float) {
        copy(particles.ppx, particles.px)
        copy(particles.ppy, particles.py)
        copy(particles.ppz, particles.pz)

        copy(particles.tx, particles.px)
        copy(particles.ty, particles.py)
        copy(particles.tz, particles.pz)

        fma(particles.tx, particles.vx, dt)
        fma(particles.ty, particles.vy, dt)
        fma(particles.tz, particles.vz, dt)
    }

    private fun copy(dst: FloatArray, src: FloatArray) {
        if (src === dst) return
        src.copyInto(dst, 0, 0, particles.size)
    }

    private fun fma(dst: FloatArray, src: FloatArray, factor: Float) {
        val size = particles.size
        check(src !== dst)
        check(src.size <= size)
        check(dst.size <= size)

        for (i in 0 until size) {
            dst[i] += src[i] * factor
        }
    }

    private fun solveConstraints() {
        repeat(config.solverIterations) {
            for (c in constraints) c.solve(particles)

            contactSolver.solveContacts()
            rigidContacts.solveContacts()
        }
    }

    private fun updateVelocities(dt: Float) {
        applyVelocityCorrections(dt)
        particles.cohesionBonds.removeIf { bond ->
            applyCohesion(bond.i, bond.j)
        }
        updateFinalPositions()
        clearTemporaryContactFlags()
    }

    private fun applyVelocityCorrections(dt: Float) {
        val invDt = 1f / dt
        for (i in 0 until particles.size) {
            var vx = (particles.tx[i] - particles.ppx[i]) * invDt
            var vy = (particles.ty[i] - particles.ppy[i]) * invDt
            var vz = (particles.tz[i] - particles.ppz[i]) * invDt

            if (particles.inContact[i]) {
                val nx = particles.contactNx[i]
                val ny = particles.contactNy[i]
                val nz = particles.contactNz[i]

                val vn = vx * nx + vy * ny + vz * nz

                // Remove inward normal velocity
                if (vn < 0f) {
                    vx -= nx * vn
                    vy -= ny * vn
                    vz -= nz * vn
                }

                // Tangential velocity
                val tx = vx - nx * vn
                val ty = vy - ny * vn
                val tz = vz - nz * vn
                val tangentialSpeed = sqrt(tx * tx + ty * ty + tz * tz)

                val muStatic = particles.staticFriction[i]
                val muDynamic = particles.dynamicFriction[i]

                // Static vs dynamic friction
                if (tangentialSpeed < muStatic * kotlin.math.abs(vn + 1e-5f)) {
                    // Static friction: cancel tangential motion
                    vx -= tx
                    vy -= ty
                    vz -= tz
                } else {
                    // Dynamic friction: damp tangential motion
                    val scale = maxOf(0f, 1f - muDynamic)
                    vx = nx * vn + tx * scale
                    vy = ny * vn + ty * scale
                    vz = nz * vn + tz * scale
                }
            }

            particles.vx[i] = vx
            particles.vy[i] = vy
            particles.vz[i] = vz
        }
    }

    private fun applyCohesion(i: Int, j: Int): Boolean {

        val vxRel = particles.vx[j] - particles.vx[i]
        val vyRel = particles.vy[j] - particles.vy[i]
        val vzRel = particles.vz[j] - particles.vz[i]

        val dx = particles.px[j] - particles.px[i]
        val dy = particles.py[j] - particles.py[i]
        val dz = particles.pz[j] - particles.pz[i]
        val dist = sqrt(dx * dx + dy * dy + dz * dz)
        if (dist == 0f) return false

        val nx = dx / dist
        val ny = dy / dist
        val nz = dz / dist

        // Relative normal velocity
        val vn = vxRel * nx + vyRel * ny + vzRel * nz

        // Relative tangential velocity
        val tx = vxRel - nx * vn
        val ty = vyRel - ny * vn
        val tz = vzRel - nz * vn
        val tangentialSpeed = sqrt(tx * tx + ty * ty + tz * tz)

        val cohesionFriction = minOf(particles.cohesion[i], particles.cohesion[j])
        val breakVelocity = 0.5f // tweak per material
        val shearLimit = 0.5f    // tweak per material

        // Damp relative tangential motion (preserves center-of-mass motion)
        if (tangentialSpeed > 1e-5f) {
            val scale = maxOf(0f, 1f - cohesionFriction)
            val dtx = tx * (1f - scale)
            val dty = ty * (1f - scale)
            val dtz = tz * (1f - scale)

            particles.vx[i] += dtx * 0.5f
            particles.vy[i] += dty * 0.5f
            particles.vz[i] += dtz * 0.5f

            particles.vx[j] -= dtx * 0.5f
            particles.vy[j] -= dty * 0.5f
            particles.vz[j] -= dtz * 0.5f
        }

        // Resist separation only (velocity along normal)
        if (vn > 0f && cohesionFriction > 0f) {
            val resist = minOf(vn, cohesionFriction)
            val impulse = resist * 0.5f
            particles.vx[i] += nx * impulse
            particles.vy[i] += ny * impulse
            particles.vz[i] += nz * impulse
            particles.vx[j] -= nx * impulse
            particles.vy[j] -= ny * impulse
            particles.vz[j] -= nz * impulse
        }

        // Break bond if exceeded thresholds
        return vn > breakVelocity || tangentialSpeed > shearLimit
    }

    private fun updateFinalPositions() {
        copy(particles.px, particles.tx)
        copy(particles.py, particles.ty)
        copy(particles.pz, particles.tz)
    }

    private fun clearTemporaryContactFlags() {
        particles.inContact.fill(false)
        particles.contactNx.fill(0f)
        particles.contactNy.fill(0f)
        particles.contactNz.fill(0f)
    }

}
