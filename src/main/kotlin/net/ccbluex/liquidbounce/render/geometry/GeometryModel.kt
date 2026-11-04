/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */

package net.ccbluex.liquidbounce.render.geometry

import net.ccbluex.liquidbounce.render.engine.type.Vec3f
import net.minecraft.world.phys.Vec3

data class GeometryModel(val texture: String, val bones: List<Bone>) {
    fun bone(name: String): Bone? = bones.firstOrNull { it.name == name }
}

data class Bone(val name: String, val pivot: Vec3, val parent: String?, val cubes: List<Cube>, val children: List<Bone>)

enum class CubeFace { DOWN, UP, NORTH, SOUTH, WEST, EAST }
data class UvRect(val x1: Float, val y1: Float, val x2: Float, val y2: Float)

/**
 * A single box of the model.
 *
 * [origin] is the minimum corner (not the center) and is expected to be already
 * centered on X/Z by the parser; [size] is `to - from` from the JSON, with no
 * centering applied.
 */
data class Cube(
    val origin: Vec3,
    val size: Vec3,
    val rotation: Vec3?,
    val rotationOrigin: Vec3?,
    val faces: Map<CubeFace, UvRect>
)

/**
 * Emits the six faces of [cube] into [sink] as `(x, y, z, u, v)` tuples.
 *
 * Coordinates are converted from model pixels to blocks here (`/ 16`), so the
 * sink receives positions in the same units as [PoseStack] transformations.
 * Faces missing from [Cube.faces] are skipped entirely.
 */
fun emitCube(
    cube: Cube,
    sink: (x: Float, y: Float, z: Float, u: Float, v: Float) -> Unit
) {
    val W = cube.size.x.toFloat()
    val H = cube.size.y.toFloat()
    val D = cube.size.z.toFloat()

    val x0 = cube.origin.x.toFloat() / 16f
    val x1 = x0 + W / 16f
    val y0 = cube.origin.y.toFloat() / 16f
    val y1 = y0 + H / 16f
    val z0 = cube.origin.z.toFloat() / 16f
    val z1 = z0 + D / 16f

    fun quad(a: Vec3f, b: Vec3f, c: Vec3f, d: Vec3f, r: UvRect) {
        sink(a.x, a.y, a.z, r.x1, r.y1)
        sink(b.x, b.y, b.z, r.x2, r.y1)
        sink(c.x, c.y, c.z, r.x2, r.y2)
        sink(d.x, d.y, d.z, r.x1, r.y2)
    }

    fun quadOf(f: CubeFace, a: Vec3f, b: Vec3f, c: Vec3f, d: Vec3f) {
        cube.faces[f]?.let { quad(a, b, c, d, it) }
    }

    val xs = floatArrayOf(x0, x1)
    val ys = floatArrayOf(y0, y1)
    val zs = floatArrayOf(z0, z1)

    /**
     * Rotates [p] around [pivot] by [rotation] degrees: `p' = R(p - c) + c`.
     *
     * The X angle is negated because [Vec3f.rotateX] uses the opposite handedness
     * to the one Minecraft block models use. That sign was found empirically:
     * flipping it makes ears/bones point backwards. Do not "fix" it.
     */
    fun rotateAround(p: Vec3f, pivot: Vec3?, rotation: Vec3?): Vec3f {
        if (rotation == null || pivot == null) return p
        if (rotation.x == 0.0 && rotation.y == 0.0 && rotation.z == 0.0) return p
        val c = Vec3f(pivot.x / 16f, pivot.y / 16f, pivot.z / 16f)

        return (p - c)
            .rotateX(Math.toRadians(-rotation.x).toFloat())
            .rotateY(Math.toRadians(rotation.y).toFloat())
            .rotateZ(Math.toRadians(rotation.z).toFloat()) + c
    }

    fun v(ix: Int, iy: Int, iz: Int): Vec3f {
        val p = Vec3f(xs[ix], ys[iy], zs[iz])
        return rotateAround(p, cube.rotationOrigin, cube.rotation)
    }

    quadOf(CubeFace.UP, v(0, 1, 0), v(1, 1, 0), v(1, 1, 1), v(0, 1, 1))
    quadOf(CubeFace.DOWN, v(0, 0, 1), v(1, 0, 1), v(1, 0, 0), v(0, 0, 0))
    quadOf(CubeFace.NORTH, v(1, 1, 0), v(0, 1, 0), v(0, 0, 0), v(1, 0, 0))
    quadOf(CubeFace.SOUTH, v(0, 1, 1), v(1, 1, 1), v(1, 0, 1), v(0, 0, 1))
    quadOf(CubeFace.EAST, v(1, 1, 1), v(1, 1, 0), v(1, 0, 0), v(1, 0, 1))
    quadOf(CubeFace.WEST, v(0, 1, 0), v(0, 1, 1), v(0, 0, 1), v(0, 0, 0))

}
