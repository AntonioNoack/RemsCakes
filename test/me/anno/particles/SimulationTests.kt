package me.anno.particles

import me.anno.particles.broadphase.SparseParticleGrid
import me.anno.particles.constraints.ParticleContactSolver
import me.anno.particles.constraints.ParticleRigidContactSolver
import me.anno.particles.utils.BoundaryBullet
import org.joml.AABBf
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SimulationTests {

    @Test
    fun `particle falls under gravity`() {
        val particles = ParticleSet(1)
        particles.px[0] = 0f
        particles.py[0] = 1f
        particles.pz[0] = 0f
        particles.invMass[0] = 1f
        particles.radius[0] = 0.05f

        val bullet = BoundaryBullet(
            AABBf(-10f, -10f, -10f, 10f, 10f, 10f)
        )

        val solver = ParticleSolver(
            particles,
            ArrayList(),
            ParticleContactSolver(
                particles,
                SparseParticleGrid(0.2f)
            ),
            ParticleRigidContactSolver(
                particles,
                bullet
            ),
            ParticleSolverConfig()
        )

        solver.step(1f / 60f)

        assertTrue(
            particles.py[0] < 1f,
            "Particle should move down under gravity"
        )
    }

    @Test
    fun `particle does not fall through floor`() {
        val particles = ParticleSet(1)
        particles.px[0] = 0f
        particles.py[0] = 0.1f
        particles.pz[0] = 0f
        particles.invMass[0] = 1f
        particles.radius[0] = 0.05f

        val bounds = AABBf(-1f, 0f, -1f, 1f, 1f, 1f)
        val bullet = BoundaryBullet(bounds)

        val solver = ParticleSolver(
            particles,
            ArrayList(),
            ParticleContactSolver(particles, SparseParticleGrid(0.2f)),
            ParticleRigidContactSolver(particles, bullet),
            ParticleSolverConfig(solverIterations = 8)
        )

        repeat(120) {
            solver.step(1f / 60f)
        }

        assertTrue(
            particles.py[0] >= bounds.minY + particles.radius[0] - 1e-3f,
            "Particle should rest on the floor"
        )
    }

    @Test
    fun `particle remains inside boundary`() {
        val particles = ParticleSet(1)
        particles.px[0] = 0.9f
        particles.py[0] = 0.9f
        particles.pz[0] = 0.9f
        particles.vx[0] = 5f
        particles.vy[0] = 3f
        particles.vz[0] = 4f
        particles.invMass[0] = 1f
        particles.radius[0] = 0.05f

        val bounds = AABBf(-1f, -1f, -1f, 1f, 1f, 1f)
        val bullet = BoundaryBullet(bounds)

        val solver = ParticleSolver(
            particles,
            ArrayList(),
            ParticleContactSolver(particles, SparseParticleGrid(0.2f)),
            ParticleRigidContactSolver(particles, bullet),
            ParticleSolverConfig()
        )

        repeat(300) {
            solver.step(1f / 120f)
        }

        assertTrue(
            bounds.testPoint(
                particles.px[0],
                particles.py[0],
                particles.pz[0]
            ), "Particle should stay inside bounds"
        )

        assertFalse(
            particles.px[0].isNaN() ||
                    particles.py[0].isNaN() ||
                    particles.pz[0].isNaN(),
            "Particle position must remain finite"
        )
    }

    @Test
    fun `particles do not overlap`() {
        val particles = ParticleSet(2)

        particles.px[0] = 0f
        particles.py[0] = 0.5f
        particles.pz[0] = 0f

        particles.px[1] = 0f
        particles.py[1] = 0.6f
        particles.pz[1] = 0f

        particles.invMass[0] = 1f
        particles.invMass[1] = 1f
        particles.radius[0] = 0.05f
        particles.radius[1] = 0.05f

        val solver = ParticleSolver(
            particles,
            ArrayList(),
            ParticleContactSolver(
                particles,
                SparseParticleGrid(0.15f)
            ),
            ParticleRigidContactSolver(
                particles,
                BoundaryBullet(AABBf(-1f, -1f, -1f, 1f, 1f, 1f))
            ),
            ParticleSolverConfig(solverIterations = 5)
        )

        repeat(60) {
            solver.step(1f / 60f)
        }

        val dx = particles.px[1] - particles.px[0]
        val dy = particles.py[1] - particles.py[0]
        val dz = particles.pz[1] - particles.pz[0]
        val dist = kotlin.math.sqrt(dx * dx + dy * dy + dz * dz)

        assertTrue(
            dist >= particles.radius[0] + particles.radius[1] - 1e-3f,
            "Particles should not overlap"
        )
    }

    @Test
    fun `particle at rest remains stable`() {
        val particles = ParticleSet(1)
        particles.px[0] = 0f
        particles.py[0] = 0.05f
        particles.pz[0] = 0f
        particles.invMass[0] = 1f
        particles.radius[0] = 0.05f

        val bullet = BoundaryBullet(
            AABBf(-1f, 0f, -1f, 1f, 1f, 1f)
        )

        val solver = ParticleSolver(
            particles, ArrayList(),
            ParticleContactSolver(particles, SparseParticleGrid(0.2f)),
            ParticleRigidContactSolver(particles, bullet),
            ParticleSolverConfig()
        )

        // println("y[0]: ${particles.py[0]}")
        repeat(300) {
            solver.step(1f / 120f)
            // println("y[${it + 1}]: ${particles.py[0]}")
        }

        assertEquals(
            particles.radius[0],
            particles.py[0],
            1e-2f,
            "Resting particle should not sink or drift"
        )
    }
}
