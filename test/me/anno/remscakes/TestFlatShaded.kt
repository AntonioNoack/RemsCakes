package me.anno.remscakes

import me.anno.ecs.components.mesh.shapes.IcosahedronModel
import me.anno.ecs.components.mesh.utils.NormalCalculator.makeFlatShaded
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI

fun main() {
    // why is this not working???
    //  because the Mesh shared positions and normals, we one of the first steps is to clear normals to 0
    val gemMesh = IcosahedronModel.createIcosphere(0)
    println("old indices: ${gemMesh.indices?.size}, positions: ${gemMesh.positions?.size}, bounds: ${gemMesh.getBounds()}")
    println(gemMesh.positions?.toList())
    gemMesh.makeFlatShaded(true)
    println("new indices: ${gemMesh.indices?.size}, positions: ${gemMesh.positions?.size}, bounds: ${gemMesh.getBounds()}")
    println(gemMesh.positions?.toList())
    testSceneWithUI("FlatShaded", gemMesh)
}