package me.anno.particles.utils

import me.anno.ecs.Component
import me.anno.ecs.systems.OnUpdate
import me.anno.particles.ParticleSolver
import me.anno.utils.Threads
import org.joml.Vector3f

class ParticlePhysics(val solver: ParticleSolver, val dt: Float) : Component(), OnUpdate {

    var isCalculating = false

    var gravity: Vector3f
        get() = solver.config.gravity
        set(value) {
            solver.config.gravity.set(value)
        }

    override fun onUpdate() {
        if (isCalculating) return
        isCalculating = true
        Threads.runTaskThread("ParticlePhysics") {
            solver.step(dt)
            isCalculating = false
        }
    }
}
