package me.anno.particles

import me.anno.maths.Maths.clamp
import me.anno.particles.utils.BoundaryBullet
import me.anno.utils.Clock
import org.joml.AABBf
import java.util.*

val bounds = AABBf(-1f, 0f, -1f, 1f, 2f, 1f)

fun createParticleCloud(size: Int, bounds: AABBf): ParticleSet {

    val particles = ParticleSet(size)
    val rnd = Random(51465)
    val spread = bounds.deltaY * 0.1f
    val radius = 0.01f

    val minX = bounds.minX + radius
    val minY = bounds.minY + radius
    val minZ = bounds.minZ + radius
    val maxX = bounds.maxX - radius
    val maxY = bounds.maxY - radius
    val maxZ = bounds.maxZ - radius

    for (i in 0 until particles.size) {
        particles.px[i] = clamp(rnd.nextGaussian().toFloat() * spread, minX, maxX)
        particles.py[i] = clamp(rnd.nextGaussian().toFloat() * spread + 0.5f, minY, maxY)
        particles.pz[i] = clamp(rnd.nextGaussian().toFloat() * spread, minZ, maxZ)
        particles.radius[i] = radius
        particles.invMass[i] = 1f
        particles.staticFriction[i] = 0.6f
        particles.dynamicFriction[i] = 0.4f
        particles.cohesion[i] = 0f
    }

    return particles
}

// todo test a simulation,
//  where particles are added one by one onto a large sand pile

fun main() {

    val clock = Clock("ParticleBenchmark")
    for (size in listOf(1250, 2500, 5000, 10_000, 20_000, 40_000)) {
        val particles = createParticleCloud(size, bounds)

        val grid = SpatialHashGrid(cellSize = 0.01f)
        val contactSolver = ParticleContactSolver(particles, grid)
        val bullet = BoundaryBullet(bounds)
        val rigidSolver = ParticleRigidContactSolver(particles, bullet)

        val solver = PBDSolver(
            particles,
            ArrayList(),
            contactSolver,
            rigidSolver,
            PBDSolverConfig(solverIterations = 8)
        )

        clock.benchmark(1, 10, size, "Solver[$size].step()") {
            solver.step(1f / 60f)
        }
    }

}