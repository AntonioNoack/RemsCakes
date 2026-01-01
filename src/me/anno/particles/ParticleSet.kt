package me.anno.particles

import kotlin.math.min

class ParticleSet(
    var size: Int,
    original: ParticleSet? = null
) {

    val cohesionBonds = HashSet<CohesionBond>()

    val capacity: Int
        get() = px.size

    // Positions
    val px: FloatArray = original?.px ?: FloatArray(size)
    val py: FloatArray = original?.py ?: FloatArray(size)
    val pz: FloatArray = original?.pz ?: FloatArray(size)

    // Previous positions (for velocity update)
    val ppx: FloatArray = original?.ppx ?: FloatArray(size)
    val ppy: FloatArray = original?.ppy ?: FloatArray(size)
    val ppz: FloatArray = original?.ppz ?: FloatArray(size)

    // Velocities
    val vx: FloatArray = original?.vx ?: FloatArray(size)
    val vy: FloatArray = original?.vy ?: FloatArray(size)
    val vz: FloatArray = original?.vz ?: FloatArray(size)

    // Inverse mass (0 = static)
    val invMass: FloatArray = original?.invMass ?: FloatArray(size)

    // Temporary predicted positions
    val tx: FloatArray = original?.tx ?: FloatArray(size)
    val ty: FloatArray = original?.ty ?: FloatArray(size)
    val tz: FloatArray = original?.tz ?: FloatArray(size)

    // Granular radius
    val radius: FloatArray = original?.radius ?: FloatArray(size)

    val contactNx: FloatArray = original?.contactNx ?: FloatArray(size)
    val contactNy: FloatArray = original?.contactNy ?: FloatArray(size)
    val contactNz: FloatArray = original?.contactNz ?: FloatArray(size)
    val inContact: BooleanArray = original?.inContact ?: BooleanArray(size)

    // Material properties (can be per-material later)
    val staticFriction: FloatArray = original?.staticFriction ?: FloatArray(size)
    val dynamicFriction: FloatArray = original?.dynamicFriction ?: FloatArray(size)
    val cohesion: FloatArray = original?.cohesion ?: FloatArray(size) // 0 = dry, >0 = wet

    fun resize(newSize: Int): ParticleSet {
        if (newSize >= capacity / 2 - 16 && newSize <= capacity) {
            size = newSize
            return this
        }
        val clone = ParticleSet(newSize)
        copyInto(clone)
        return clone
    }

    @Suppress("DuplicatedCode")
    fun copyInto(dst: ParticleSet) {
        val size = min(size, dst.size)
        copyInto(px, dst.px, size)
        copyInto(py, dst.py, size)
        copyInto(pz, dst.pz, size)

        copyInto(ppx, dst.px, size)
        copyInto(ppy, dst.ppy, size)
        copyInto(ppz, dst.ppz, size)

        copyInto(vx, dst.vx, size)
        copyInto(vy, dst.vy, size)
        copyInto(vz, dst.vz, size)

        copyInto(invMass, dst.invMass, size)

        copyInto(tx, dst.tx, size)
        copyInto(ty, dst.ty, size)
        copyInto(tz, dst.tz, size)

        copyInto(radius, dst.radius, size)

        copyInto(contactNx, dst.contactNx, size)
        copyInto(contactNy, dst.contactNy, size)
        copyInto(contactNz, dst.contactNz, size)
        inContact.copyInto(dst.inContact, 0, 0, size)
    }

    private fun copyInto(src: FloatArray, dst: FloatArray, size: Int) {
        src.copyInto(dst, 0, 0, size)
    }
}
