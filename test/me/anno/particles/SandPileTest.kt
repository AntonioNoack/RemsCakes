package me.anno.particles

import me.anno.ecs.Entity
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.particles.utils.BoundaryBullet
import me.anno.particles.utils.ParticlePhysics
import me.anno.particles.utils.ParticleRenderer

fun main() {

    val preset = MaterialPreset.DOUGH
    val particles = createParticleCloud(10_000, bounds)

    particles.staticFriction.fill(preset.staticFriction)
    particles.dynamicFriction.fill(preset.dynamicFriction)
    particles.cohesion.fill(preset.cohesion)

    val grid = SpatialHashGrid(cellSize = 0.03f)
    val contactSolver = ParticleContactSolver(particles, grid)
    val bullet = BoundaryBullet(bounds)
    val rigidSolver = ParticleRigidContactSolver(particles, bullet)

    val solver = PBDSolver(
        particles,
        ArrayList(),
        contactSolver,
        rigidSolver,
        PBDSolverConfig(solverIterations = 20)
    )

    val scene = Entity()
        .add(ParticleRenderer(particles))
        .add(ParticlePhysics(solver, 1f / 60f))
    testSceneWithUI("SandPileTest", scene)

}