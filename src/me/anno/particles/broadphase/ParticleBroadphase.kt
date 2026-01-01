package me.anno.particles.broadphase

import me.anno.utils.callbacks.I2U

interface ParticleBroadphase {
    fun clear()
    fun insert(x: Float, y: Float, z: Float, index: Int)
    fun queryPairs(callback: I2U)
}
