package me.anno.particles.world

import me.anno.particles.RaycastHit

interface ParticleObstacle {
    /**
     * Performs a collision query against rigid bodies.
     * Returns the closest hit, or null if no collision.
     */
    fun raycast(
        fromX: Float, fromY: Float, fromZ: Float,
        toX: Float, toY: Float, toZ: Float,
        dst: RaycastHit
    ): RaycastHit?
}
