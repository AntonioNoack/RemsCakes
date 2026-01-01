package me.anno.particles.constraints

import me.anno.particles.CohesionBond
import me.anno.particles.ParticleSet
import me.anno.particles.broadphase.ParticleBroadphase
import me.anno.particles.constraints.ParticleConstraint.Companion.addT
import kotlin.math.sqrt

/**
 * Enforces distance between particles
 * */
class ParticleContactSolver(
    private val particles: ParticleSet,
    private val grid: ParticleBroadphase,
    val stiffness: Float = 0.2f,
) {

    var numClose = 0
    var numHit = 0

    fun solveContacts() {
        numClose = 0
        numHit = 0
        updateGrid()
        grid.queryPairs(::solveContact)
        // println("[${particles.size}] Close: $numClose, Hit: $numHit")
    }

    private fun updateGrid() {
        grid.clear()
        // Insert predicted positions
        for (i in 0 until particles.size) {
            grid.insert(
                particles.tx[i],
                particles.ty[i],
                particles.tz[i],
                i
            )
        }
    }

    private fun solveContact(i: Int, j: Int) {
        numClose++

        val dx = particles.tx[j] - particles.tx[i]
        val dy = particles.ty[j] - particles.ty[i]
        val dz = particles.tz[j] - particles.tz[i]

        val distSq = dx * dx + dy * dy + dz * dz
        val minDist = particles.radius[i] + particles.radius[j]

        if (distSq >= minDist * minDist || distSq == 0f) return

        val dist = sqrt(distSq)
        val penetration = minDist - dist

        val w1 = particles.invMass[i]
        val w2 = particles.invMass[j]
        val wSum = w1 + w2
        if (wSum == 0f) return

        numHit++

        // 0.2 works nicely for dough, but sand probably needs something harder...
        val correction = stiffness * penetration / (dist * wSum)
        particles.addT(i, dx, dy, dz, -w1 * correction)
        particles.addT(j, dx, dy, dz, +w2 * correction)

        if (particles.cohesion[i] > 0f || particles.cohesion[j] > 0f) {
            particles.cohesionBonds.add(CohesionBond(i, j))
        }

        // TODO: tangential friction (critical for angle of repose)
        // TODO: rolling resistance
    }
}
