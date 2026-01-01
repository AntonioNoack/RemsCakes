package me.anno.particles.utils

import me.anno.ecs.Component
import me.anno.ecs.systems.OnUpdate
import me.anno.particles.PBDSolver
import me.anno.utils.Threads
import kotlin.concurrent.thread

class ParticlePhysics(val solver: PBDSolver, val dt: Float) : Component(), OnUpdate {

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
