package me.anno.particles.visual

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.material.Material
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.particles.NoodleSimulationTests
import me.anno.particles.ParticleSet
import me.anno.particles.constraints.BendingConstraint
import me.anno.particles.utils.MaterialRange
import me.anno.particles.utils.ParticlePhysics
import me.anno.particles.utils.SphereParticleRenderer

fun main() {
    val helper = NoodleSimulationTests()
    val (particles, springs) = helper.createNoodle(20, 0.5f, 10f)

    val solver = helper.createSolver(particles, springs)
    val scene = Entity()
        .add(SphereParticleRenderer(particles, emptyList()))
        .add(ParticlePhysics(solver, 1f / 60f))
    testSceneWithUI("SandPileTest", scene)
}