package me.anno.remscakes

import me.anno.bullet.BulletDebugDraw
import me.anno.bullet.BulletPhysics
import me.anno.bullet.bodies.DynamicBody
import me.anno.bullet.bodies.GhostBody
import me.anno.bullet.bodies.StaticBody
import me.anno.config.DefaultConfig.style
import me.anno.ecs.Entity
import me.anno.ecs.components.camera.control.CameraController
import me.anno.ecs.components.camera.control.FirstPersonController
import me.anno.ecs.components.collider.CapsuleCollider
import me.anno.ecs.components.collider.InfinitePlaneCollider
import me.anno.ecs.components.collider.MeshCollider
import me.anno.ecs.components.collider.SphereCollider
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.TransformMesh.scale
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.mesh.shapes.IcosahedronModel
import me.anno.ecs.components.mesh.utils.NormalCalculator.makeFlatShaded
import me.anno.ecs.components.player.LocalPlayer
import me.anno.ecs.prefab.PrefabInspector
import me.anno.ecs.prefab.change.Path
import me.anno.ecs.systems.Systems
import me.anno.ecs.systems.Systems.registerSystem
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.ECSTreeView
import me.anno.engine.ui.EditorState
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.render.RenderView
import me.anno.engine.ui.render.SceneView
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.engine.ui.scenetabs.ECSSceneTab
import me.anno.engine.ui.scenetabs.ECSSceneTabs
import me.anno.gpu.GFX
import me.anno.gpu.pipeline.PipelineStage
import me.anno.graph.visual.render.effects.AutoExposureNode
import me.anno.io.files.FileReference
import me.anno.maths.Maths.TAU
import me.anno.ui.Panel
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.custom.CustomList
import me.anno.ui.debug.PureTestEngine.Companion.testPureUI
import me.anno.ui.editor.PropertyInspector
import me.anno.utils.OS.res
import org.apache.logging.log4j.LogManager
import org.joml.Vector3f
import kotlin.math.cos
import kotlin.math.sin

// todo physics-simulator chemistry engine:
//  there is components like water, flour, eggs, sugar, powdered sugar, fruits, fruit juice
//  we have a mixing degree
//  depending on mixing degree and components, there is different states:
//   - powder
//   - fluid
//   - dough
//   - solid
//  and they mix in 3D,
//  and the user works with them, mixes them, etc.
//  and there is containers for them
//  and we have a classifier for what something is
//  and ofc there is cooking recipes for the user to follow,
//  and baking/heating stations/places,
//  and we calculate calories, far percentage etc
//  and we combine it with Bullet physics

// todo we need recipe mixing...
//  upto four components per particle???
//  mix color/physics separate from components???

// done first-person player
// done player can grab stuff -> we kind of have a sample for that: GemMiner
// todo player can throw things, let them fall
// todo player is in a relatively simple environment

var debugScene = false

// this is from the mining game, adjust it to work for this cooking game
fun main() {

    LogManager.disableInfoLogs("Saveable")
    OfficialExtensions.initForTests()
    definePhysics()

    val scene = Entity()
    Systems.world = scene

    // todo create a kitchen room mesh
    // todo create physics for kitchen mesh

    spawnFloor(scene)
    spawnGems(scene)

    scene.add(Controls())

    AutoExposureNode.minBrightness = 0.01f
    AutoExposureNode.maxBrightness = 100f

    if (false) {
        testSceneWithUI("Rem's Gems", scene)
    } else {
        testPureUI("Rem's Gems") {
            GFX.someWindow.windowStack.firstOrNull()?.drawDirectly = false
            scene.prefabPath = Path.ROOT_PATH
            createSceneUI2(scene.ref) { sceneView ->
                // val ui = sceneView.playControls
                // createInventoryUI(ui)
                createPlayer(scene, sceneView.renderView)
            }.fill(1f)
        }
    }
}

fun createPlayer(scene: Entity, renderView: RenderView): LocalPlayer {

    val localPlayer = LocalPlayer()
    val controls = FirstPersonController().apply {
        needsClickToRotate = debugScene
        rotateRight = debugScene
        movementSpeed = 0.0
        radius = -0.15
        position.set(0.0, 0.7, 0.0)
    }

    val playerHeight = 1.8f

    val camBase = CameraController.setup(controls, renderView)
    Entity("Player", scene)
        .add(PlayerController())
        .add(DynamicBody().apply {
            mass = 2f
            angularFactor.set(0.0) // cannot rotate
        })
        .add(CapsuleCollider().apply {
            radius = playerHeight * 0.25f
            halfHeight = playerHeight * 0.25f
        })
        // .add(MeshComponent(CapsuleModel.createCapsule(20, 10, 0.5f, 0.5f)))
        .add(camBase)
        .setPosition(0.0, 1.0, 0.0)

    Entity("Magnet", camBase.children[0])
        .add(GhostBody())
        .add(SphereCollider().apply { radius = playerHeight })
        .add(MagnetAttractor())
        .setPosition(0.0, 0.0, -1.0)

    return localPlayer
}

fun definePhysics() {
    val physics = BulletPhysics().apply {
        updateInEditMode = true
        enableDebugRendering = debugScene
    }
    if (!debugScene) BulletDebugDraw.debugMode = 0
    registerSystem(physics)

    // todo spawn particle system with some fluid (in a glass?),
    //  some sand (flour) and some dough and salami

}

// todo let the player build the cupboards and such? :D

fun spawnFloor(scene: Entity) {
    Entity("Floor", scene)
        .add(StaticBody())
        .add(InfinitePlaneCollider())

    val roomMesh = res.getChild("models/Conditorei.glb")
    Entity("Room", scene)
        .add(MeshComponent(roomMesh))
        .add(StaticBody())
        .add(MeshCollider(roomMesh).apply { isConvex = false })
}

fun spawnGems(scene: Entity) {
    val gemMesh = IcosahedronModel.createIcosphere(0)
    gemMesh.makeFlatShaded(true)
    gemMesh.scale(Vector3f(0.2f))

    val material = Material().apply {
        diffuseBase.w = 0.1f
        pipelineStage = PipelineStage.GLASS
        metallicMinMax.set(1f)
        roughnessMinMax.set(0.01f)
        indexOfRefraction = 3f
    }

    for (i in 0 until 10) {
        val angle = (i + 0.5) * TAU / 10
        Entity("Gem", scene)
            .add(DynamicBody().apply {
                angularDamping = 0.2f
                mass = 0.1f
            })
            .add(SphereCollider().apply { radius = 0.2f })
            .add(MeshComponent(gemMesh, material))
            .setPosition(cos(angle), 1.0, sin(angle))
    }
}
