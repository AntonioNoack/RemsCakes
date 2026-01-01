package me.anno.particles

data class RaycastHit(
    var hitX: Float,
    var hitY: Float,
    var hitZ: Float,
    var normalX: Float,
    var normalY: Float,
    var normalZ: Float
) {
    fun set(hitX: Float, hitY: Float, hitZ: Float, normalX: Float, normalY: Float, normalZ: Float): RaycastHit {
        this.hitX = hitX
        this.hitY = hitY
        this.hitZ = hitZ
        this.normalX = normalX
        this.normalY = normalY
        this.normalZ = normalZ
        return this
    }
}
