package me.anno.particles.utils

import me.anno.particles.BulletCollisionWorld
import me.anno.particles.RaycastHit
import org.joml.AABBf
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class BoundaryBullet(val bounds: AABBf) : BulletCollisionWorld {

    companion object {
        private val divisor = FloatArray(4) { 1f / sqrt(max(it, 1).toFloat()) }
    }

    override fun raycast(
        fromX: Float, fromY: Float, fromZ: Float,
        toX: Float, toY: Float, toZ: Float,
        dst: RaycastHit
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
        val count = (abs(normalX) + abs(normalY) + abs(normalZ)).toInt()
        if (count == 0) return null

        val normalizer = divisor[count]
        return dst.set(hitX, hitY, hitZ, normalX * normalizer, normalY * normalizer, normalZ * normalizer)
    }

    private fun AABBf.raycastFromInside(
        px: Float, py: Float, pz: Float,
        invDx: Float, invDy: Float, invDz: Float,
        margin: Float,
    ): Float {
        val sx0 = (minX - margin - px) * invDx
        val sx1 = (maxX + margin - px) * invDx

        val sy0 = (minY - margin - py) * invDy
        val sy1 = (maxY + margin - py) * invDy

        val sz0 = (minZ - margin - pz) * invDz
        val sz1 = (maxZ + margin - pz) * invDz

        val nearX = min(sx0, sx1)
        val nearY = min(sy0, sy1)
        val nearZ = min(sz0, sz1)

        val farX = max(sx0, sx1)
        val farY = max(sy0, sy1)
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
}