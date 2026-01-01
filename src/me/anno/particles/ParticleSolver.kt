package me.anno.particles

import me.anno.maths.Maths.sq
import me.anno.particles.constraints.ParticleConstraint
import me.anno.particles.constraints.ParticleConstraint.Companion.addV
import me.anno.particles.constraints.ParticleContactSolver
import me.anno.particles.constraints.ParticleRigidContactSolver
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class ParticleSolver(
    private val particles: ParticleSet,
    private val constraints: MutableList<ParticleConstraint>,
    private val contactSolver: ParticleContactSolver,
    private val rigidContacts: ParticleRigidContactSolver,
    val config: ParticleSolverConfig
) {

    fun step(dt: Float) {
        val dtSubStep = dt / config.substeps

        repeat(config.substeps) {
            applyExternalForces(dtSubStep)
            predictPositions(dtSubStep)
            solveConstraints(dtSubStep)
            updateVelocities(dtSubStep)
        }
    }

    private fun applyExternalForces(dt: Float) {
        val gx = config.gravity.x * dt
        val gy = config.gravity.y * dt
        val gz = config.gravity.z * dt
        for (i in 0 until particles.size) {
            if (particles.invMass[i] == 0f) continue
            particles.addV(i, gx, gy, gz)
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

    private fun solveConstraints(dt: Float) {
        repeat(config.solverIterations) {
            constraints.removeIf { it.solve(particles, dt) }
            contactSolver.solveContacts()
            rigidContacts.solveContacts()
        }
    }

    private fun updateVelocities(dt: Float) {
        applyVelocityCorrections(dt)
        particles.cohesionBonds.removeIf { bond ->
            applyCohesion(bond.i, bond.j)
        }
        applyGlobalDamping()
        updateFinalPositions()
        clearTemporaryContactFlags()
    }

    private fun applyGlobalDamping() {
        val damping = config.damping
        for (i in 0 until particles.size) {
            particles.vx[i] *= damping
            particles.vy[i] *= damping
            particles.vz[i] *= damping
        }
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
                if (tangentialSpeed < muStatic * abs(vn + 1e-5f)) {
                    // Static friction: cancel tangential motion
                    vx -= tx
                    vy -= ty
                    vz -= tz
                } else {
                    // Dynamic friction: damp tangential motion
                    val scale = max(0f, 1f - muDynamic)
                    vx = nx * vn + tx * scale
                    vy = ny * vn + ty * scale
                    vz = nz * vn + tz * scale
                }
            }

            val ovx = particles.vx[i]
            val ovy = particles.vy[i]
            val ovz = particles.vz[i]

            val newEnergy = sq(vx, vy, vz)
            val oldEnergy = sq(ovx, ovy, ovz)
            if (newEnergy > oldEnergy) {
                val scale = sqrt(oldEnergy / newEnergy)
                vx *= scale
                vy *= scale
                vz *= scale
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
        val dist = sqrt(max(dx * dx + dy * dy + dz * dz, 0f))
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

        val cohesionFriction = min(particles.cohesion[i], particles.cohesion[j])
        // todo these should be per-material-pair
        val breakVelocity = 0.5f // tweak per material
        val shearLimit = 0.5f // tweak per material

        // Damp relative tangential motion (preserves center-of-mass motion)
        if (tangentialSpeed > 1e-5f) {
            val scale = max(0f, 1f - cohesionFriction)
            val factor = (1f - scale)
            push(i, j, tx, ty, tz, factor)
        }

        // Resist separation only (velocity along normal)
        if (vn > 0f && cohesionFriction > 0f) {
            val resist = min(vn, cohesionFriction)
            push(i, j, nx, ny, nz, resist)
        }

        // Break bond if exceeded thresholds
        return vn > breakVelocity || tangentialSpeed > shearLimit
    }

    private fun push(
        i: Int, j: Int,
        dtx: Float, dty: Float, dtz: Float,
        factor: Float
    ) {
        val m1 = particles.invMass[i]
        val m2 = particles.invMass[j]
        // is this correct, or should we use 1/invMass?
        // ratio stays the same, so both ways work
        val scale = factor / (m1 + m2)
        particles.addV(i, dtx, dty, dtz, +m1 * scale)
        particles.addV(i, dtx, dty, dtz, -m2 * scale)
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
