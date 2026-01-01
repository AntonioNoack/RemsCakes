package me.anno.particles

import me.anno.utils.callbacks.I2U
import me.anno.utils.structures.arrays.IntArrayList
import kotlin.math.floor

class SpatialHashGrid(cellSize: Float) {

    private class Cell(var xi: Int, var yi: Int, var zi: Int) {
        val points = IntArrayList(4)
        fun add(point: Int) {
            points.add(point)
        }
    }

    private val invCellSize = 1f / cellSize
    private val map = HashMap<Long, Cell>(8 shl 10)
    private val pool = ArrayList<Cell>()

    // points should typically stay, where they are,
    //  so instead of rebuilding,
    //  we could also try to update them

    fun clear() {
        pool.addAll(map.values)
        map.clear()
    }

    fun insert(x: Float, y: Float, z: Float, index: Int) {
        val xi = floorDiv(x)
        val yi = floorDiv(y)
        val zi = floorDiv(z)
        val key = hashCell(xi, yi, zi)
        map.getOrPut(key) { createCell(xi, yi, zi) }.add(index)
    }

    private fun createCell(xi: Int, yi: Int, zi: Int): Cell {
        val oldCell = pool.removeLastOrNull() ?: return Cell(xi, yi, zi)
        oldCell.xi = xi
        oldCell.yi = yi
        oldCell.zi = zi
        oldCell.points.clear()
        return oldCell
    }

    fun queryPairs(callback: I2U) {
        for ((key0, cell) in map) {
            for (dx in -1..1) {
                for (dy in -1..1) {
                    for (dz in -1..1) {
                        val key = hashCell(cell.xi + dx, cell.yi + dy, cell.zi + dz)
                        val other = if (key == key0) cell else map[key] ?: continue
                        queryPairs(cell.points, other.points, callback)
                    }
                }
            }
        }
    }

    private fun queryPairs(cell: IntArrayList, other: IntArrayList, callback: I2U) {
        if (cell === other) {
            for (i in 1 until cell.size) {
                val ci = cell[i]
                for (j in 0 until i) {
                    callback.call(ci, cell[j])
                }
            }
        } else {
            for (i in 0 until cell.size) {
                for (j in 0 until other.size) {
                    callback.call(cell[i], other[j])
                }
            }
        }
    }

    private fun floorDiv(v: Float): Int =
        floor(v * invCellSize).toInt()

    private fun hashCell(x: Int, y: Int, z: Int): Long {
        // Large primes, spatial hash classic
        return (x * 73856093L) xor
                (y * 19349663L) xor
                (z * 83492791L)
    }
}
