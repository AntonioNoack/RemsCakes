package me.anno.particles

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MeshAttributes.color0
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.particles.ParticleSet.Companion.mergeParticles
import me.anno.particles.utils.ParticlePhysics
import me.anno.particles.utils.ParticleRenderer
import me.anno.utils.Color
import me.anno.utils.Color.withAlpha

fun main() {
    val helper = FluidSimulationTests()
    val light = helper.createFluidParticles(100, density = 1.0f)
    val medium = helper.createFluidParticles(100, density = 2.0f)
    val heavy = helper.createFluidParticles(100, density = 3.0f)

    val particles = mergeParticles(light, medium, heavy)
    val solver = helper.createFluidSolver(particles)

    val scene = Entity()
        .add(ParticleRenderer(particles).apply {
            mesh.color0 = IntArray(particles.size) {
                if (it < light.size) Color.white
                else if (it < light.size + medium.size) 0x777777.withAlpha(255)
                else 0x333333.withAlpha(255)
            }
        })
        .add(ParticlePhysics(solver, 1f / 60f))
    testSceneWithUI("SandPileTest", scene)
}