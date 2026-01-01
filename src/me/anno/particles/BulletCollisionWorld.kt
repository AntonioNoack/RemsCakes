package me.anno.particles

interface BulletCollisionWorld {

    /**
     * Performs a collision query against rigid bodies.
     * Returns the closest hit, or null if no collision.
     */
    fun raycast(
        fromX: Float, fromY: Float, fromZ: Float,
        toX: Float, toY: Float, toZ: Float
    ): RaycastHit?

    /**
     * Optional: overlap test for sphere
     */
    fun sphereCast(
        x: Float, y: Float, z: Float,
        radius: Float
    ): List<ContactHit>
}
