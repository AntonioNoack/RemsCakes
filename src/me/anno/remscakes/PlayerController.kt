package me.anno.remscakes

import me.anno.bullet.bodies.DynamicBody
import me.anno.ecs.Component
import me.anno.ecs.EntityQuery.getComponent
import me.anno.ecs.systems.OnPhysicsUpdate
import me.anno.engine.ui.render.RenderView
import me.anno.input.Input
import me.anno.input.Key
import me.anno.utils.types.Booleans.toInt
import org.joml.Quaternionf
import org.joml.Vector3f
import kotlin.math.abs

class PlayerController : Component(), OnPhysicsUpdate {

    var walkVelocity = 3f
    var runningVelocity = 5f
    var maxAcceleration = 20f

    override fun onPhysicsUpdate(dt: Double) {
        val body = getComponent(DynamicBody::class) ?: return

        val moveX =
            if (debugScene) Input.isKeyDown(Key.KEY_L) - Input.isKeyDown(Key.KEY_J)
            else Input.isKeyDown(Key.KEY_D) - Input.isKeyDown(Key.KEY_A)
        val moveZ =
            if (debugScene) Input.isKeyDown(Key.KEY_K) - Input.isKeyDown(Key.KEY_I)
            else Input.isKeyDown(Key.KEY_S) - Input.isKeyDown(Key.KEY_W)

        val dir = Vector3f(moveX.toFloat(), 0f, moveZ.toFloat())
            .safeNormalize(if (Input.isShiftDown) runningVelocity else walkVelocity)

        // rotate by camera forward
        val rv = RenderView.currentInstance
        if (rv != null) {
            val lp = rv.localPlayer
            val rotation = if (lp != null) {
                lp.cameraState.currentCamera!!.transform!!.getGlobalRotation(Quaternionf())
            } else {
                rv.orbitRotation
            }
            dir.rotateY(rotation.getEulerAngleYXZvY())
        }

        // scale force by fast we already are
        val speed = body.globalLinearVelocity
        dir.sub(speed).div(dt.toFloat())
        dir.y = 0f

        val acceleration = dir.length()
        if (acceleration > maxAcceleration) {
            dir.mul(maxAcceleration / acceleration)
        }

        dir.mul(body.mass)

        body.activate()
        body.applyForce(dir)

        // todo better on-ground check?
        if (Input.wasKeyPressed(Key.KEY_SPACE) && abs(speed.y) < 0.1) {
            body.applyImpulse(0f, 5f * body.mass, 0f)
        }
    }

    operator fun Boolean.minus(other: Boolean) = toInt() - other.toInt()
}