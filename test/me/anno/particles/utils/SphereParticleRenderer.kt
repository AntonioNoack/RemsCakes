package me.anno.particles.utils

import me.anno.ecs.Transform
import me.anno.ecs.components.mesh.IMesh
import me.anno.ecs.components.mesh.MeshSpawner
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.mesh.shapes.IcosahedronModel
import me.anno.gpu.pipeline.Pipeline
import me.anno.particles.ParticleSet
import me.anno.utils.structures.arrays.FloatArrayList
import me.anno.utils.structures.arrays.FloatArrayListUtils.add
import org.joml.AABBd
import org.joml.Matrix4x3

class SphereParticleRenderer(
    val particles: ParticleSet,
    val materialRanges: List<MaterialRange>,
) : MeshSpawner() {

    private val mesh = IcosahedronModel.createIcosphere(0)
    private val material = Material().apply { translucency = 1f }

    override fun fillSpace(globalTransform: Matrix4x3, dstUnion: AABBd) {
        fillAllSpace(dstUnion)
    }

    override fun forEachMesh(pipeline: Pipeline?, callback: (IMesh, Material?, Transform) -> Boolean) {
        throw NotImplementedError()
    }

    override fun forEachMeshGroupTRS(pipeline: Pipeline, callback: (IMesh, Material?) -> FloatArrayList): Boolean {
        var i = 0
        for (range in materialRanges) {
            val dst = callback(mesh, range.material)
            push(dst, i, range.until)
            i = range.until
        }
        if (i < particles.size) {
            val dst = callback(mesh, material)
            push(dst, i, particles.size)
        }
        return true
    }

    private fun push(dst: FloatArrayList, i0: Int, i1: Int) {
        for (i in i0 until i1) {
            // todo we could interpolate position and previous position for a smoother simulation
            dst.add(particles.px[i], particles.py[i], particles.pz[i], particles.radius[i])
            dst.add(0f, 0f, 0f, 1f)

            // println(listOf(particles.px[i], particles.py[i], particles.pz[i]))
        }
    }

}
