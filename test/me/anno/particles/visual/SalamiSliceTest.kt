package me.anno.particles.visual

import me.anno.ecs.Entity
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.maths.Maths.TAUf
import me.anno.particles.ParticleSet
import me.anno.particles.ParticleSolver
import me.anno.particles.ParticleSolverConfig
import me.anno.particles.broadphase.SparseParticleGrid
import me.anno.particles.constraints.*
import me.anno.particles.utils.BoundaryBullet
import me.anno.particles.utils.ParticlePhysics
import me.anno.particles.utils.SphereParticleRenderer
import org.joml.AABBf
import kotlin.math.cos
import kotlin.math.sin

fun main() {

    // todo why is this soo extremely unstable???

    val bounds = AABBf(-2f, 0f, -2f, 2f, 2f, 2f)

    val numSlices = 3
    val particles = ParticleSet(7 * numSlices)
    val constraints = ArrayList<ParticleConstraint>()

    val sliceRadius = 0.3f
    val pointRadius = sliceRadius * 0.48f

    fun defineSalami(di: Int) {
        for (i in 0 until 6) {
            val angle = (di + i) * TAUf / 6
            particles.px[di + i] = cos(angle) * sliceRadius
            particles.py[di + i] = pointRadius * 3f + di * 0.1f
            particles.pz[di + i] = sin(angle) * sliceRadius
            particles.radius[di + i] = pointRadius
            particles.invMass[di + i] = 1f
        }

        particles.px[di + 6] = 0f
        particles.py[di + 6] = 2f
        particles.pz[di + 6] = 0f
        particles.radius[di + 6] = pointRadius
        particles.invMass[di + 6] = 1f

        fun line(i: Int, j: Int) {
            // add length constraint
            constraints.add(SpringConstraint(di + i, di + j, sliceRadius, 2f, 1f))
        }

        for (i in 0 until 6) {
            // add all triangle length constraints
            val j = (i + 1) % 6
            line(i, j)
            line(j, 6)
            line(i, 6)
        }

        for (i in di until di + 3) {
            // add bending constraints
            val i0 = i
            val i1 = di + 6
            val i2 = i + 3
            constraints.add(BendingConstraint(i0, i1, i2, 1f, 1f))
        }
    }

    for (i in 0 until numSlices) {
        defineSalami(i * 7)
    }

    val solver = ParticleSolver(
        particles,
        constraints,
        ParticleContactSolver(particles, SparseParticleGrid(pointRadius)),
        ParticleRigidContactSolver(particles, BoundaryBullet(bounds)),
        ParticleSolverConfig(10)
    )

    val scene = Entity()
        .add(SphereParticleRenderer(particles, emptyList()))
        .add(ParticlePhysics(solver, 1f / 60f))
    testSceneWithUI("SandPileTest", scene)
}