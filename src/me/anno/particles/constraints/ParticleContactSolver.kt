package me.anno.particles.constraints

import me.anno.particles.CohesionBond
import me.anno.particles.ParticleSet
import me.anno.particles.broadphase.ParticleBroadphase
import kotlin.math.sqrt

/**
 * This is where sand-behavior emerges
 * */
class ParticleContactSolver(
    private val particles: ParticleSet,
    private val grid: ParticleBroadphase,
    private val stiffness: Float = 1.0f
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
        val correction = penetration * stiffness

        val cx = nx * correction * invW
        val cy = ny * correction * invW
        val cz = nz * correction * invW

        particles.tx[i] -= cx * w1
        particles.ty[i] -= cy * w1
        particles.tz[i] -= cz * w1

        particles.tx[j] += cx * w2
        particles.ty[j] += cy * w2
        particles.tz[j] += cz * w2

        if (particles.cohesion[i] > 0f || particles.cohesion[j] > 0f) {
            particles.cohesionBonds.add(CohesionBond(i, j))
        }

        // TODO: tangential friction (critical for angle of repose)
        // TODO: rolling resistance
    }
}
