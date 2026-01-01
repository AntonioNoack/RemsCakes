package me.anno.remscakes

import me.anno.bullet.bodies.DynamicBody
import me.anno.bullet.bodies.GhostBody
import me.anno.ecs.Component
import me.anno.ecs.EntityQuery.getComponent
import me.anno.ecs.systems.OnPhysicsUpdate
import me.anno.input.Input
import me.anno.maths.Maths
import org.joml.Vector3d

class MagnetAttractor : Component(), OnPhysicsUpdate {

    private val otherPosition = Vector3d()
    private val position = Vector3d()

    var maxAcceleration = 20f

    override fun onPhysicsUpdate(dt: Double) {
        if (!Input.isLeftDown) return
        val self = getComponent(GhostBody::class) ?: return
        val transform = transform ?: return
        transform.getGlobalPosition(position)

        for (body in self.overlappingBodies) {
            if (body !is DynamicBody) continue
            if (body.mass > 1.0) continue // exclude the player

            val otherTransform = body.transform ?: continue
            otherTransform.getGlobalPosition(otherPosition)

            val dir = otherPosition.sub(position)
            val factor0 = body.mass * Maths.clamp(1.5 - 0.75 * dir.length())
            val multiplier = -maxAcceleration * factor0 / (dir.length() + 0.01f)

            val speed = body.globalLinearVelocity
            val speedX = -factor0 * 0.8f

            body.activate()
            body.applyForce(
                (dir.x * multiplier + speed.x * speedX).toFloat(),
                (dir.y * multiplier + speed.y * speedX).toFloat(),
                (dir.z * multiplier + speed.z * speedX).toFloat(),
            )
        }
    }
}