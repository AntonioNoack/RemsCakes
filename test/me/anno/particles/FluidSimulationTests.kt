package me.anno.particles

import me.anno.maths.Maths.mix
import me.anno.particles.ParticleSet.Companion.mergeParticles
import me.anno.particles.broadphase.DenseParticleGrid
import me.anno.particles.constraints.ParticleContactSolver
import me.anno.particles.constraints.ParticleRigidContactSolver
import me.anno.particles.utils.BoundaryBullet
import org.joml.AABBf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.random.Random

class FluidSimulationTests {

    val radius = 0.3f
    val s = 3f
    val random = Random(1324)
    val bounds = AABBf(-s, 0f, -s, s, s * 3f, s)

    // --- Helper functions ---
    fun createFluidParticles(size: Int, density: Float = 1f): ParticleSet {
        val particles = ParticleSet(size)
        for (i in 0 until size) {
            particles.px[i] = mix(bounds.minX + radius, bounds.maxX - radius, random.nextFloat())
            particles.py[i] = mix(bounds.minY + radius, bounds.maxY - radius, random.nextFloat())
            particles.pz[i] = mix(bounds.minZ + radius, bounds.maxZ - radius, random.nextFloat())
            particles.invMass[i] = 1f / density
            particles.radius[i] = radius
        }
        return particles
    }

    private fun isInsideDomain(x: Float, y: Float, z: Float): Boolean {
        return bounds.testPoint(x, y, z)
    }

    // --- Stub: create solver for fluid particles ---
    fun createFluidSolver(particles: ParticleSet): ParticleSolver {
        return ParticleSolver(
            particles,
            ArrayList(),
            ParticleContactSolver(particles, DenseParticleGrid(radius * 1.4f, bounds)),
            ParticleRigidContactSolver(particles, BoundaryBullet(bounds)),
            ParticleSolverConfig(solverIterations = 5)
        )
    }

    // -----------------------
    // --- UNIT TESTS -------
    // -----------------------

    @Test
    fun `fluid particles remain in domain`() {
        val particles = createFluidParticles(100)
        val solver = createFluidSolver(particles)

        repeat(60) { solver.step(1f / 60f) }

        for (i in 0 until particles.size) {
            assertTrue(
                isInsideDomain(particles.px[i], particles.py[i], particles.pz[i]),
                "Particle $i is outside the simulation domain"
            )
        }
    }

    @Test
    fun `fluid falls under gravity`() {
        val particles = createFluidParticles(50)
        val solver = createFluidSolver(particles)

        val initialY = particles.py.average()
        repeat(60) { solver.step(1f / 60f) }
        val finalY = particles.py.average()

        assertTrue(finalY < initialY, "Fluid should move down under gravity")
    }

    // todo fixing the velocity-explosion issue somehow prevents our fluids from sorting...
    //  because it's finally stable???

    @Test
    fun `fluids separate by density`() {
        val light = createFluidParticles(100, density = 1.0f)
        val medium = createFluidParticles(100, density = 2.0f)
        val heavy = createFluidParticles(100, density = 3.0f)

        val particles = mergeParticles(light, medium, heavy)
        val solver = createFluidSolver(particles)

        repeat(600) { solver.step(1f / 120f) }

        val avgLight = (0 until light.size).map { particles.py[it] }.average()
        val avgMedium = (light.size until light.size + medium.size).map { particles.py[it] }.average()
        val avgHeavy = (light.size + medium.size until particles.size).map { particles.py[it] }.average()

        assertTrue(avgLight > avgMedium + 1e-3f, "Light fluid should float above medium")
        assertTrue(avgMedium > avgHeavy + 1e-3f, "Medium fluid should float above heavy")
    }

    @Test
    fun `mixed fluids separate correctly`() {
        val light = createFluidParticles(100, density = 1.0f)
        val medium = createFluidParticles(100, density = 2.0f)
        val heavy = createFluidParticles(100, density = 3.0f)

        val particles = mergeParticles(light, medium, heavy)

        val solver = createFluidSolver(particles)
        repeat(600) { solver.step(1f / 120f) }

        // Check ordering by average Y
        val lightY = (0 until light.size).map { particles.py[it] }.average()
        val mediumY = (light.size until light.size + medium.size).map { particles.py[it] }.average()
        val heavyY = (light.size + medium.size until particles.size).map { particles.py[it] }.average()

        assertTrue(lightY > mediumY + 1e-3f, "Light fluid should end up on top")
        assertTrue(mediumY > heavyY + 1e-3f, "Medium fluid should end up in the middle")
    }
}
