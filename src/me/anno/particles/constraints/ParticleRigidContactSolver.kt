package me.anno.particles.constraints

import me.anno.particles.BulletCollisionWorld
import me.anno.particles.ParticleSet
import me.anno.particles.RaycastHit
import me.anno.particles.constraints.ParticleConstraint.Companion.addT
import kotlin.math.sqrt

/**
 * Enforces distance from rigidbodies to particles
 * */
class ParticleRigidContactSolver(
    private val particles: ParticleSet,
    private val bullet: BulletCollisionWorld,
) {

    private val dst = RaycastHit(0f, 0f, 0f, 0f, 0f, 0f)

    fun solveContacts() {
        for (i in 0 until particles.size) {
            if (particles.invMass[i] == 0f) continue

            val px = particles.ppx[i]
            val py = particles.ppy[i]
            val pz = particles.ppz[i]

            val tx = particles.tx[i]
            val ty = particles.ty[i]
            val tz = particles.tz[i]

            var dx = tx - px
            var dy = ty - py
            var dz = tz - pz
            val distSq = dx * dx + dy * dy + dz * dz
            if (distSq < 1e-9f) continue

            val radius = particles.radius[i]
            val scale = radius / sqrt(distSq)
            dx *= scale
            dy *= scale
            dz *= scale

            // Sweep test (prevents tunneling)
            val hit = bullet.raycast(
                px, py, pz,
                tx + dx, ty + dy, tz + dz, dst
            ) ?: continue

            val nx = hit.normalX
            val ny = hit.normalY
            val nz = hit.normalZ

            // todo this should get a marker of what
            //  the relative velocity of the contact body is:
            //  friction must be applied with relative velocity!
            particles.inContact[i] = true
            particles.contactNx[i] = nx
            particles.contactNy[i] = ny
            particles.contactNz[i] = nz

            // Move particle out of penetration

            val penetration0 = (hit.hitX - tx) * nx +
                    (hit.hitY - ty) * ny +
                    (hit.hitZ - tz) * nz
            val penetration = -(penetration0 + radius)

            // println("Penetration[$px,$py,$pz -> $tx,$ty,$tz]: $penetration0 x $radius -> $penetration, Hit: $hit")
            if (penetration >= 0f) continue

            // println("Delta: ${-correction * ny}")

            particles.addT(i, nx, ny, nz, -penetration)

            // TODO: tangential friction against rigid body
            // TODO: rolling resistance
            // TODO: two-way impulse transfer to rigid body
        }
    }
}
