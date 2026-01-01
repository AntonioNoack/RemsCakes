package me.anno.particles.constraints

import me.anno.particles.CohesionBond
import me.anno.particles.ParticleSet
import me.anno.particles.broadphase.ParticleBroadphase
import me.anno.particles.constraints.ParticleConstraint.Companion.addT
import kotlin.math.sqrt

/**
 * This is where sand-behavior emerges
 * */
class ParticleContactSolver(
    private val particles: ParticleSet,
    private val grid: ParticleBroadphase,
    private val stiffness: Float = 1f
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

        val xi = particles.tx[i]
        val yi = particles.ty[i]
        val zi = particles.tz[i]
        val ri = particles.radius[i]

        val dx = particles.tx[j] - xi
        val dy = particles.ty[j] - yi
        val dz = particles.tz[j] - zi

        val distSq = dx * dx + dy * dy + dz * dz
        val minDist = ri + particles.radius[j]

        if (distSq >= minDist * minDist || distSq == 0f) return

        val dist = sqrt(distSq)
        val penetration = minDist - dist

        val invDist = 1f / dist
        val nx = dx * invDist
        val ny = dy * invDist
        val nz = dz * invDist

        val w1 = particles.invMass[i]
        val w2 = particles.invMass[j]
        val wSum = w1 + w2
        if (wSum == 0f) return
        val invW = 1f / wSum

        numHit++

        val correction = penetration * stiffness * invW
        particles.addT(i, nx, ny, nz, -w1 * correction)
        particles.addT(j, nx, ny, nz, +w2 * correction)

        if (particles.cohesion[i] > 0f || particles.cohesion[j] > 0f) {
            particles.cohesionBonds.add(CohesionBond(i, j))
        }

        // TODO: tangential friction (critical for angle of repose)
        // TODO: rolling resistance
    }
}
