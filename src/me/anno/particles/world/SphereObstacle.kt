package me.anno.particles.world

import me.anno.maths.Maths.length
import me.anno.maths.Maths.sq
import me.anno.particles.RaycastHit
import org.joml.Vector3f
import kotlin.math.sqrt

class SphereObstacle(val position: Vector3f, var radius: Float) : ParticleObstacle {

    override fun raycast(
        fromX: Float, fromY: Float, fromZ: Float,
        toX: Float, toY: Float, toZ: Float,
        dst: RaycastHit
    ): RaycastHit? {

        val relX = fromX - position.x
        val relY = fromY - position.y
        val relZ = fromZ - position.z

        val dirX = toX - fromX
        val dirY = toY - fromY
        val dirZ = toZ - fromZ

        val dot = relX * dirX + relY * dirY + relZ * dirZ
        if (dot <= 0f) return null // moving away -> don't care

        val relX2 = toX - position.x
        val relY2 = toY - position.y
        val relZ2 = toZ - position.z

        val distSq = sq(relX2, relY2, relZ2) // gonna be distance
        if (distSq < 1e-9f) return null // direction unknown (tunneling)
        val distance = sqrt(distSq)

        val particleRadius = length(dirX, dirY, dirZ) // particle radius
        if (distance >= radius + particleRadius) return null // too far away

        // now we have a hit
        val norm = 1f / distance
        val nx = norm * relX2
        val ny = norm * relY2
        val nz = norm * relZ2
        dst.normal.set(nx, ny, nz)
        dst.position.set(position)
            .add(radius * nx, radius * ny, radius * nz)
        return dst
    }
}