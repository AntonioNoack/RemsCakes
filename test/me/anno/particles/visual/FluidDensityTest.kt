package me.anno.particles.visual

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.material.Material
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.particles.FluidSimulationTests
import me.anno.particles.ParticleSet.Companion.mergeParticles
import me.anno.particles.utils.MaterialRange
import me.anno.particles.utils.ParticlePhysics
import me.anno.particles.utils.SphereParticleRenderer

// todo for fluid rendering,
//  enter the fluid density into a grid,
//  blur the grid,
//  calculate marching cubes

// todo for finding connected puddles,
//  union-find algorithm,
//  AABB around each puddle,
//  and then marching cubes

fun main() {
    val helper = FluidSimulationTests()
    val light = helper.createFluidParticles(100, density = 1.0f)
    val medium = helper.createFluidParticles(100, density = 2.0f)
    val heavy = helper.createFluidParticles(100, density = 3.0f)

    val particles = mergeParticles(light, medium, heavy)
    val solver = helper.createFluidSolver(particles)

    fun create(color: Int): Material {
        return Material.diffuse(color)
    }

    val renderer = SphereParticleRenderer(
        particles, listOf(
            MaterialRange(light.size, create(0xffffff)),
            MaterialRange(light.size + medium.size, create(0x777777)),
            MaterialRange(particles.size, create(0x333333)),
        )
    )

    val scene = Entity()
        .add(renderer)
        .add(ParticlePhysics(solver, 1f / 60f))
    testSceneWithUI("SandPileTest", scene)
}