package me.anno.remscakes

import me.anno.config.DefaultConfig.style
import me.anno.ecs.prefab.PrefabInspector
import me.anno.engine.ui.ECSTreeView
import me.anno.engine.ui.EditorState
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.render.SceneView
import me.anno.engine.ui.scenetabs.ECSSceneTab
import me.anno.engine.ui.scenetabs.ECSSceneTabs
import me.anno.io.files.FileReference
import me.anno.ui.Panel
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.custom.CustomList
import me.anno.ui.editor.PropertyInspector


fun createSceneUI2(scene: FileReference, init: ((SceneView) -> Unit)? = null): Panel {
    val listY = PanelListY(style)
    listY.add(ECSSceneTabs)
    val playMode = PlayMode.PLAYING
    ECSSceneTabs.open(ECSSceneTab(scene, playMode), true)
    val sceneView = SceneView(playMode, style)
    PrefabInspector.currentInspector = PrefabInspector(scene)
    val list = CustomList(false, style)
    if (debugScene) list.add(ECSTreeView(style), 1f)
    list.add(sceneView, 3f)
    if (debugScene) list.add(PropertyInspector({ EditorState.selection }, style), 1f)
    if (init != null) init(sceneView)
    listY.add(list)
    list.weight = 1f
    listY.weight = 1f
    return listY
}
