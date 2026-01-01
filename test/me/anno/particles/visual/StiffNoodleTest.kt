package me.anno.particles.visual

import me.anno.ecs.Entity
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.particles.NoodleSimulationTests
import me.anno.particles.ParticleSet
import me.anno.particles.constraints.BendingConstraint
import me.anno.particles.utils.ParticlePhysics
import me.anno.particles.utils.SphereParticleRenderer

fun createBendingConstraints(particles: ParticleSet): List<BendingConstraint> {
    return List(particles.size - 2) { i0 ->
        BendingConstraint(i0, i0 + 1, i0 + 2, 60f, 0.05f)
    }
}

fun main() {
    val helper = NoodleSimulationTests()
    val (particles, springs) = helper.createNoodle(20, 0.5f, 60f)

    val solver = helper.createSolver(particles, springs + createBendingConstraints(particles))
    val scene = Entity()
        .add(SphereParticleRenderer(particles, emptyList()))
        .add(ParticlePhysics(solver, 1f / 60f))
    testSceneWithUI("SandPileTest", scene)
}