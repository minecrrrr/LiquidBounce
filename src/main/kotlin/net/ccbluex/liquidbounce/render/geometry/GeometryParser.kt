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

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.ccbluex.liquidbounce.LiquidBounce
import net.minecraft.world.phys.Vec3

/**
 * The model's UV grid: how many units the JSON `uv` values are measured in.
 * Defaults to 16x16, which is what Blockbench exports with a texture grid of 16.
 */
private data class Grid(val width: Int, val height: Int)

object GeometryParser {

    private val DEFAULT_GRID = Grid(16, 16)

    fun parse(modelPath: String, texture: String): GeometryModel {
        val root = LiquidBounce.resource(modelPath).use { JsonParser.parseReader(it.reader()).asJsonObject }

        val grid = root.getAsJsonObject("grid")?.let(::readGrid) ?: DEFAULT_GRID

        val bones = root.getAsJsonArray("bones")?.let { parseV2(it, grid) }
            ?: root.getAsJsonArray("elements")?.let { parseV1(it, grid) }
            ?: error("$modelPath: neither 'bones' nor 'elements'")

        return GeometryModel(texture, bones)
    }

    /**
     * Java Block/Item: a flat list of cubes, wrapped into a single `root` bone.
     */
    private fun parseV1(elements: JsonArray, grid: Grid): List<Bone> =
        listOf(
            Bone(
                name = "root",
                pivot = Vec3.ZERO,
                parent = null,
                cubes = elements.map { readCube(it, grid) },
                children = emptyList()
            )
        )

    /**
     * v2: bones with `parent`/`pivot`.
     *
     * Cubes are read, but the hierarchy is not built yet: every bone is flat
     * (`children = emptyList()`). That is enough for models to render, but a
     * parent's rotation does not affect its children yet.
     *
     * Next step: two passes — collect every bone first, then link parents to
     * children and reject cycles.
     */
    private fun parseV2(bones: JsonArray, grid: Grid): List<Bone> {
        return bones.map { el ->
            val o = el.asJsonObject

            Bone(
                name = o["name"]?.asString ?: error("bone without 'name'"),
                pivot = o.getAsJsonArray("pivot")
                    ?.let(::readVec3)?.centered()
                    ?: Vec3.ZERO,
                parent = o["parent"]?.takeIf { !it.isJsonNull }?.asString,
                cubes = o.getAsJsonArray("cubes")?.map { readCube(it, grid) }.orEmpty(),
                children = emptyList(),
            )
        }
    }

    private fun readVec3(array: JsonArray) =
        Vec3(array[0].asDouble, array[1].asDouble, array[2].asDouble)

    /**
     * JSON coordinates are measured from the corner of a 16x16x16 box, while the
     * renderer expects the model to be centered on X/Z. Y is left untouched so
     * that "height above zero" stays meaningful.
     */
    private fun Vec3.centered() =
        Vec3(x - 8.0, y, z - 8.0)

    private fun readRotation(o: JsonObject): Vec3 {
        if (o.get("angle") != null) {
            val a = o.get("angle").asDouble
            return when (o.get("axis")?.asString?.lowercase()) {
                "x" -> Vec3(a, 0.0, 0.0)
                "y" -> Vec3(0.0, a, 0.0)
                "z" -> Vec3(0.0, 0.0, a)
                else -> Vec3.ZERO
            }
        }

        return Vec3(
            o.get("x")?.asDouble ?: 0.0,
            o.get("y")?.asDouble ?: 0.0,
            o.get("z")?.asDouble ?: 0.0,
        )
    }

    private fun readGrid(o: JsonObject): Grid =
        Grid(o["width"]?.asInt ?: 16, o["height"]?.asInt ?: 16)

    /**
     * Reads one cube. `origin` and the rotation pivot are centered with
     * [centered]; `size` is a plain `to - from` difference and must never be
     * centered. UVs are normalized by [grid], so `uv` above `16` (larger UV
     * grids) keeps working.
     *
     * Rotation supports both Blockbench shapes: `{x, y, z}` and `{angle, axis}`.
     */
    private fun readCube(el: JsonElement, grid: Grid): Cube {
        val e = el.asJsonObject

        val from = e.getAsJsonArray("from")?.let(::readVec3)
            ?: error("cube without 'from'")
        val to = e.getAsJsonArray("to")?.let(::readVec3)
            ?: error("cube without 'to'")

        val faces = HashMap<CubeFace, UvRect>()
        val facesJson = e.getAsJsonObject("faces")

        val (gw, gh) = grid

        for (face in CubeFace.entries) {
            val faceJson = facesJson?.getAsJsonObject(face.name.lowercase()) ?: continue
            val uv = faceJson.getAsJsonArray("uv") ?: continue
            faces[face] = UvRect(
                uv[0].asFloat / gw,
                uv[1].asFloat / gh,
                uv[2].asFloat / gw,
                uv[3].asFloat / gh,
            )
        }

        val rot = e.getAsJsonObject("rotation")
        val rotation = rot?.let(::readRotation)
        val rotationOrigin = rot?.getAsJsonArray("origin")
            ?.let(::readVec3)?.centered()
            ?: Vec3.ZERO

        return Cube(
            origin = from.centered(),
            size = Vec3(to.x - from.x, to.y - from.y, to.z - from.z),
            rotation = rotation,
            rotationOrigin = rotationOrigin,
            faces = faces,
        )
    }

}
