package me.anno.particles

import me.anno.particles.broadphase.SparseParticleGrid
import me.anno.particles.constraints.*
import me.anno.particles.utils.BoundaryBullet
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertTrue
import org.joml.AABBf
import org.junit.jupiter.api.Test
import kotlin.math.abs
import kotlin.math.sqrt

class NoodleSimulationTests {

    private val s = 50f
    private val bounds = AABBf(-s, 0f, -s, s, s, s)

    fun createNoodle(
        particleCount: Int,
        segmentLength: Float,
        stiffness: Float,
        fixedFirst: Boolean = true
    ): Pair<ParticleSet, List<SpringConstraint>> {

        val p = ParticleSet(particleCount)
        val constraints = ArrayList<SpringConstraint>()

        for (i in 0 until particleCount) {
            p.px[i] = i * segmentLength
            p.py[i] = 5f
            p.pz[i] = 0f

            p.tx[i] = p.px[i]
            p.ty[i] = p.py[i]
            p.tz[i] = p.pz[i]

            p.vx[i] = 0f
            p.vy[i] = 0f
            p.vz[i] = 0f

            p.radius[i] = 0.05f
            p.invMass[i] = if (fixedFirst && i == 0) 0f else 1f
        }

        for (i in 0 until particleCount - 1) {
            constraints += SpringConstraint(
                i, i + 1, segmentLength, stiffness,
                segmentLength
            )
        }

        return p to constraints
    }

    fun createSolver(particles: ParticleSet, constraints: List<ParticleConstraint>): ParticleSolver {
        return ParticleSolver(
            particles = particles,
            constraints = ArrayList(constraints),
            ParticleContactSolver(particles, SparseParticleGrid(0.07f)),
            ParticleRigidContactSolver(particles, BoundaryBullet(bounds)),
            config = ParticleSolverConfig(
                solverIterations = 10,
            )
        )
    }

    @Test
    fun `noodle preserves segment length`() {
        val (particles, springs) = createNoodle(
            particleCount = 10,
            segmentLength = 0.5f,
            stiffness = 1.0f * 60f
        )

        val solver = createSolver(particles, springs)
        repeat(200) { solver.step(1f / 60f) }

        for (i in 0 until particles.size - 1) {
            val dx = particles.px[i + 1] - particles.px[i]
            val dy = particles.py[i + 1] - particles.py[i]
            val dz = particles.pz[i + 1] - particles.pz[i]
            val dist = sqrt(dx * dx + dy * dy + dz * dz)

            assertEquals(
                0.5f, dist,
                0.02f,
                "Segment $i length drifted"
            )
        }
    }

    @Test
    fun `noodle bends under gravity`() {
        val (particles, springs) = createNoodle(
            particleCount = 12,
            segmentLength = 0.4f,
            stiffness = 0.8f
        )

        val solver = createSolver(particles, springs)
        repeat(300) { solver.step(1f / 60f) }

        val startY = particles.py[0]
        val endY = particles.py[particles.size - 1]

        assertTrue(
            endY < startY - 0.5f,
            "Noodle did not bend under gravity"
        )
    }

    @Test
    fun `noodle remains stable over time`() {
        val (particles, springs) = createNoodle(
            particleCount = 20,
            segmentLength = 0.3f,
            stiffness = 1.0f
        )

        val solver = createSolver(particles, springs)
        repeat(600) { solver.step(1f / 120f) }

        for (i in 0 until particles.size) {
            assertTrue(
                particles.py[i].isFinite(),
                "Particle $i became unstable"
            )
        }
    }

    @Test
    fun `stiffer noodle bends less`() {
        val (soft, softSprings) = createNoodle(10, 0.5f, stiffness = 0.3f)
        val (stiff, stiffSprings) = createNoodle(10, 0.5f, stiffness = 1.0f)

        val softSolver = createSolver(soft, softSprings)
        val stiffSolver = createSolver(stiff, stiffSprings)

        repeat(300) {
            softSolver.step(1f / 60f)
            stiffSolver.step(1f / 60f)
        }

        val softEndY = soft.py.last()
        val stiffEndY = stiff.py.last()

        println("Soft vs stiff: $softEndY vs $stiffEndY")

        assertTrue(
            softEndY < 0.9f * stiffEndY,
            "Stiffer noodle should bend less"
        )
    }

    @Test
    fun `uncooked spaghetti resists bending`() {
        val (particles, springs) = createNoodle(
            particleCount = 15,
            segmentLength = 0.3f,
            stiffness = 30.0f
        )

        val constraints = springs + List(particles.size - 2) { i ->
            BendingConstraint(
                i, i + 1, i + 2,
                stiffness = 0.9f * 60f, 1f
            )
        }

        val solver = createSolver(particles, constraints)

        repeat(300) { solver.step(1f / 60f) }

        val mid = particles.size / 2
        val sag = particles.py[particles.size - 1] + 2f * particles.py[mid] - particles.py[0]

        println("Sag: $sag")

        assertTrue(
            abs(sag) < 2f,
            "Uncooked spaghetti bent too much"
        )
    }
}