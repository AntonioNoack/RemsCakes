package me.anno.particles.utils

import me.anno.ecs.Component
import me.anno.ecs.systems.OnUpdate
import me.anno.input.Input
import me.anno.particles.ParticleSolver
import me.anno.utils.Threads

class ParticlePhysics(val solver: ParticleSolver, val dt: Float) : Component(), OnUpdate {

    var isCalculating = false

    override fun onUpdate() {
        if (isCalculating) return
        isCalculating = true
        Threads.runTaskThread("ParticlePhysics") {
            solver.step(dt)
            isCalculating = false
        }
    }
}
