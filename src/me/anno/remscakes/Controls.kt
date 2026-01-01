package me.anno.remscakes

import me.anno.ecs.Component
import me.anno.ecs.interfaces.InputListener
import me.anno.ecs.systems.OnUpdate
import me.anno.engine.ui.render.RenderView
import me.anno.input.Input
import me.anno.input.Key

class Controls : Component(), OnUpdate, InputListener {
    override fun onUpdate() {
        if (Input.wasKeyPressed(Key.BUTTON_LEFT)) {
            val rv = RenderView.currentInstance?.run { uiParent ?: this }
            if (rv != null && rv.isAnyChildInFocus) rv.lockMouse()
        }
        if (Input.wasKeyPressed(Key.KEY_ESCAPE)) {
            // todo show game/settings menu
            Input.unlockMouse()
        }
    }

    private operator fun Key.plus(i: Int) = Key.byId(id + i)
}