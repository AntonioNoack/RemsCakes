package me.anno.particles.utils

import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.systems.OnUpdate
import me.anno.gpu.buffer.DrawMode
import me.anno.particles.ParticleSet

class ParticleRenderer(
    val particles: ParticleSet,
    val mesh: Mesh = Mesh()
) : MeshComponent(mesh), OnUpdate {

    init {
        mesh.drawMode = DrawMode.POINTS
        mesh.positions = FloatArray(particles.size * 3)
        updateMesh()
    }

    private fun updateMesh() {
        val dst = mesh.positions!!
        var k = 0
        for (i in 0 until particles.size) {
            dst[k++] = particles.px[i]
            dst[k++] = particles.py[i]
            dst[k++] = particles.pz[i]
        }
        mesh.invalidateGeometry()
        invalidateBounds()
    }

    override fun onUpdate() {
        // todo only if there was an update...
        updateMesh()
    }
}
