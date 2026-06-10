package me.anno.particles.world

import me.anno.particles.RaycastHit
import org.joml.AABBf
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class BoundsCollisions(val bounds: AABBf) : ParticleObstacle {

    companion object {
        private val divisor = FloatArray(4) { numHits -> 1f / sqrt(max(numHits, 1).toFloat()) }

        fun eq(v: Float, min: Float, max: Float): Float {
            val epsilon = 0.01f
            if (v <= min + epsilon) return +1f
            if (v >= max - epsilon) return -1f
            return 0f
        }
    }

    override fun raycast(
        fromX: Float, fromY: Float, fromZ: Float,
        toX: Float, toY: Float, toZ: Float,
        dst: RaycastHit
    ): RaycastHit? {

        val toInside = bounds.testPoint(toX, toY, toZ)
        if (toInside) return null

        val dirX = toX - fromX
        val dirY = toY - fromY
        val dirZ = toZ - fromZ

        val invDx = 1f / dirX
        val invDy = 1f / dirY
        val invDz = 1f / dirZ

        val tx0 = (bounds.minX - fromX) * invDx
        val tx1 = (bounds.maxX - fromX) * invDx

        val ty0 = (bounds.minY - fromY) * invDy
        val ty1 = (bounds.maxY - fromY) * invDy

        val tz0 = (bounds.minZ - fromZ) * invDz
        val tz1 = (bounds.maxZ - fromZ) * invDz

        val nearX = min(tx0, tx1)
        val nearY = min(ty0, ty1)
        val nearZ = min(tz0, tz1)

        val farX = max(tx0, tx1)
        val farY = max(ty0, ty1)
        val farZ = max(tz0, tz1)

        val near = max(nearX, max(nearY, nearZ))
        val far = min(farX, min(farY, farZ))

        if (far < near) return null

        // If starting inside, use exit point.
        // If starting outside, use entry point.
        val distance = if (bounds.testPoint(fromX, fromY, fromZ)) far else near
        if (distance < 0f || !distance.isFinite()) {
            return null
        }

        val hitX = fromX + dirX * distance
        val hitY = fromY + dirY * distance
        val hitZ = fromZ + dirZ * distance

        val normalX = eq(hitX, bounds.minX, bounds.maxX)
        val normalY = eq(hitY, bounds.minY, bounds.maxY)
        val normalZ = eq(hitZ, bounds.minZ, bounds.maxZ)

        val count = (abs(normalX) + abs(normalY) + abs(normalZ)).toInt()
        if (count == 0) return null

        val normalizer = divisor[count]

        return dst.set(
            hitX, hitY, hitZ,
            normalX * normalizer,
            normalY * normalizer,
            normalZ * normalizer
        )
    }
}