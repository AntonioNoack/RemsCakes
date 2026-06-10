package me.anno.particles

import org.joml.Vector3f

class RaycastHit {

    val position = Vector3f()
    val normal = Vector3f()

    fun set(hitX: Float, hitY: Float, hitZ: Float, normalX: Float, normalY: Float, normalZ: Float): RaycastHit {
        position.set(hitX, hitY, hitZ)
        normal.set(normalX, normalY, normalZ)
        return this
    }

    fun set(src: RaycastHit): RaycastHit {
        position.set(src.position)
        normal.set(src.normal)
        return this
    }
}
