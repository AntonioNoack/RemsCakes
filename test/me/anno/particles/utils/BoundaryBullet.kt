package me.anno.particles.utils

import me.anno.particles.BulletCollisionWorld
import me.anno.particles.ContactHit
import me.anno.particles.RaycastHit
import org.joml.AABBf
import kotlin.math.max
import kotlin.math.min

class BoundaryBullet(val bounds: AABBf) : BulletCollisionWorld {

    override fun raycast(
        fromX: Float, fromY: Float, fromZ: Float,
        toX: Float, toY: Float, toZ: Float
    ): RaycastHit? {
        val fromInside = bounds.testPoint(fromX, fromY, fromZ)
        val toInside = bounds.testPoint(toX, toY, toZ)
        if (fromInside == toInside) return null

        val dirX = toX - fromX
        val dirY = toY - fromY
        val dirZ = toZ - fromZ
        val distance = bounds.raycastFromInside(
            fromX, fromY, fromZ,
            1f / dirX, 1f / dirY, 1f / dirZ,
            0f
        )
        if (!distance.isFinite()) return null
        val hitX = fromX + dirX * distance
        val hitY = fromY + dirY * distance
        val hitZ = fromZ + dirZ * distance
        val normalX = eq(hitX, bounds.minX, bounds.maxX)
        val normalY = eq(hitY, bounds.minY, bounds.maxY)
        val normalZ = eq(hitZ, bounds.minZ, bounds.maxZ)
        return RaycastHit(hitX, hitY, hitZ, normalX, normalY, normalZ, 0)
    }

    private fun AABBf.raycastFromInside(
        px: Float, py: Float, pz: Float,
        invDx: Float, invDy: Float, invDz: Float,
        margin: Float,
    ): Float {
        val sx0 = (minX - margin - px) * invDx
        val sy0 = (minY - margin - py) * invDy
        val sz0 = (minZ - margin - pz) * invDz
        val sx1 = (maxX + margin - px) * invDx
        val sy1 = (maxY + margin - py) * invDy
        val sz1 = (maxZ + margin - pz) * invDz
        val nearX = min(sx0, sx1)
        val farX = max(sx0, sx1)
        val nearY = min(sy0, sy1)
        val farY = max(sy0, sy1)
        val nearZ = min(sz0, sz1)
        val farZ = max(sz0, sz1)
        val far = min(farX, min(farY, farZ))
        val near = max(nearX, max(nearY, nearZ))
        return if (far >= near) far else Float.POSITIVE_INFINITY
    }

    private fun eq(v: Float, min: Float, max: Float): Float {
        val epsilon = 0.01f
        if (v <= min + epsilon) return +1f
        if (v >= max - epsilon) return -1f
        return 0f
    }

    override fun sphereCast(x: Float, y: Float, z: Float, radius: Float): List<ContactHit> {
        val result = ArrayList<ContactHit>()
        val minX = bounds.minX + radius
        val minY = bounds.minY + radius
        val minZ = bounds.minZ + radius
        val maxX = bounds.maxX - radius
        val maxY = bounds.maxY - radius
        val maxZ = bounds.maxZ - radius
        if (x < minX) result.add(ContactHit(minX, y, z, +1f, 0f, 0f, 0))
        if (y < minY) result.add(ContactHit(x, minY, z, 0f, +1f, 0f, 1))
        if (z < minZ) result.add(ContactHit(x, y, minZ, 0f, 0f, +1f, 2))
        if (x > maxX) result.add(ContactHit(maxX, y, z, -1f, 0f, 0f, 3))
        if (y > maxY) result.add(ContactHit(x, maxY, z, 0f, -1f, 0f, 4))
        if (z > maxZ) result.add(ContactHit(x, y, maxZ, 0f, 0f, -1f, 5))
        return result
    }

}