package me.anno.particles.broadphase

import me.anno.maths.Maths.clamp
import me.anno.utils.callbacks.I2U
import me.anno.utils.structures.arrays.IntArrayList
import org.joml.AABBf
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

class DenseParticleGrid(cellSize: Float, val bounds: AABBf) : ParticleBroadphase {

    val nx = max(floor(bounds.deltaX / cellSize).toInt(), 1)
    val ny = max(floor(bounds.deltaY / cellSize).toInt(), 1)
    val nz = max(floor(bounds.deltaZ / cellSize).toInt(), 1)

    private val invSizeX = nx / bounds.deltaX
    private val invSizeY = ny / bounds.deltaY
    private val invSizeZ = nz / bounds.deltaZ

    val size = Math.multiplyExact(nx, Math.multiplyExact(ny, nz))
    val cells = Array(size) { IntArrayList(4) }

    // points should typically stay, where they are,
    //  so instead of rebuilding,
    //  we could also try to update them

    override fun clear() {
        for (i in cells.indices) {
            cells[i].clear()
        }
    }

    override fun insert(x: Float, y: Float, z: Float, index: Int) {
        val xi = clamp(((x - bounds.minX) * invSizeX).toInt(), 0, nx - 1)
        val yi = clamp(((y - bounds.minY) * invSizeY).toInt(), 0, ny - 1)
        val zi = clamp(((z - bounds.minZ) * invSizeZ).toInt(), 0, nz - 1)
        val key = getIndex(xi, yi, zi)
        cells[key].add(index)
    }

    override fun queryPairs(callback: I2U) {
        for (yi in 0 until ny) {
            for (zi in 0 until nz) {
                for (xi in 0 until nx) {
                    queryPairsI(xi, yi, zi, callback)
                }
            }
        }
    }

    private fun queryPairsI(xi: Int, yi: Int, zi: Int, callback: I2U) {
        val i = getIndex(xi, yi, zi)
        val cell = cells[i]
        if (cell.isEmpty()) return

        SparseParticleGrid.queryPairsSame(cell, callback)

        for (yj in max(yi - 1, 0) until min(yi + 2, ny)) {
            for (zj in max(zi - 1, 0) until min(zi + 2, nz)) {
                for (xj in max(xi - 1, 0) until min(xi + 2, nx)) {
                    val j = getIndex(xj, yj, zj)
                    if (i < j) {
                        val other = cells[j]
                        SparseParticleGrid.queryPairsDiff(cell, other, callback, false)
                    }
                }
            }
        }
    }

    private fun getIndex(xi: Int, yi: Int, zi: Int): Int {
        return xi + (zi + yi * nz) * nx
    }
}
