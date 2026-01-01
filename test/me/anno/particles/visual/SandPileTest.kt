package me.anno.particles.visual

import me.anno.ecs.Entity
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.particles.MaterialPreset
import me.anno.particles.ParticleSolver
import me.anno.particles.ParticleSolverConfig
import me.anno.particles.broadphase.SparseParticleGrid
import me.anno.particles.constraints.ParticleContactSolver
import me.anno.particles.constraints.ParticleRigidContactSolver
import me.anno.particles.createParticleCloud
import me.anno.particles.utils.BoundaryBullet
import me.anno.particles.utils.ParticlePhysics
import me.anno.particles.utils.SphereParticleRenderer
import org.joml.AABBf

fun main() {

    val s = 0.2f
    val bounds = AABBf(-s, 0f, -s, s, 2f, s)

    val preset = MaterialPreset.DOUGH
    val particles = createParticleCloud(10_000, bounds)

    particles.staticFriction.fill(preset.staticFriction)
    particles.dynamicFriction.fill(preset.dynamicFriction)
    particles.cohesion.fill(preset.cohesion)

    val grid = SparseParticleGrid(cellSize = 0.03f)
    val contactSolver = ParticleContactSolver(particles, grid)
    val bullet = BoundaryBullet(bounds)
    val rigidSolver = ParticleRigidContactSolver(particles, bullet)

    val solver = ParticleSolver(
        particles,
        ArrayList(),
        contactSolver,
        rigidSolver,
        ParticleSolverConfig(solverIterations = 5)
    )

    // val renderer = PointParticleRenderer(particles)
    val renderer = SphereParticleRenderer(particles,emptyList())

    val scene = Entity()
        .add(renderer)
        .add(ParticlePhysics(solver, 1f / 60f))
    testSceneWithUI("SandPileTest", scene)

}