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

package net.ccbluex.liquidbounce.features.module.modules.render.hats.styles.legacy.modes

import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.features.module.modules.render.hats.styles.legacy.LegacyHatsStyle
import net.ccbluex.liquidbounce.render.ClientRenderPipelines
import net.ccbluex.liquidbounce.render.WorldRenderEnvironment
import net.ccbluex.liquidbounce.render.drawCustomMesh
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.segmentAngle
import net.ccbluex.liquidbounce.render.setColor
import net.ccbluex.liquidbounce.utils.math.fastCos
import net.ccbluex.liquidbounce.utils.math.fastSin
import kotlin.math.cos
import kotlin.math.sin

internal object LegacyHatsOrbs : LegacyHatsStyle("Orbs") {

    val color by color("color", Color4b(0, 0, 255, 125))

    private object HatOrbsSettings : ValueGroup("HatSettings") {

        object OrbSettings : ValueGroup("OrbSettings") {
            val centreOffset by float("CentreOffset", 0f, -1f..1f)
            val yOffset by float("YOffset", 0f, -0.5f..0.5f)
            val size by float("Size", 0.1f, 0.01f..0.5f)
            val count by int("Count", 6, 1..12)
            val spinSpeed by float("SpinSpeed", 2f, -10f..10f)
        }

        object OrbitSettings : ValueGroup("OrbitSettings") {
            val radius by float("Radius", 0.5f, 0f..2f)
            val speed by float("Speed", 0.5f, 0.1f..10f)
        }

        object WaveSettings : ToggleableValueGroup(this@LegacyHatsOrbs, "Wave", true) {
            val waveHeight by float("WaveHeight", 0.1f, 0.01f..1f)
            val waveSpeed by float("WaveSpeed", 2.0f, 0.1f..10f)
        }

        init {
            treeAll(OrbSettings, OrbitSettings, WaveSettings)
        }

    }

    init {
        tree(HatOrbsSettings)
    }

    override fun WorldRenderEnvironment.drawLegacyHat(isHurt: Boolean) {
        drawCustomMesh(ClientRenderPipelines.triangles(noDepthTest = !canBeCovered)) { matrix ->
            val time = ((System.currentTimeMillis() % 1000000L).toFloat() / 1000f) * HatOrbsSettings.OrbitSettings.speed

            // Loop for rendering each individual orb (orbit).
            for (i in 0 until HatOrbsSettings.OrbSettings.count) {
                val angle = (segmentAngle(i, HatOrbsSettings.OrbSettings.count) + time)

                val x = getPointX(angle, HatOrbsSettings.OrbitSettings.radius)
                val z = getPointZ(angle, HatOrbsSettings.OrbitSettings.radius)

                val y = if (HatOrbsSettings.WaveSettings.enabled) {
                    sin(time * HatOrbsSettings.WaveSettings.waveSpeed + i) *
                        HatOrbsSettings.WaveSettings.waveHeight
                } else {
                    0f
                }

                val rotAngle = getRotationAngle(HatOrbsSettings.OrbSettings.spinSpeed)
                val sinA = rotAngle.fastSin() * HatOrbsSettings.OrbSettings.size
                val cosA = rotAngle.fastCos() * HatOrbsSettings.OrbSettings.size

                val top = y + HatOrbsSettings.OrbSettings.size + HatOrbsSettings.OrbSettings.yOffset
                val bottom = y - HatOrbsSettings.OrbSettings.size - HatOrbsSettings.OrbSettings.yOffset

                val ax = x + sinA
                val az = z + cosA
                val bx = x + cosA
                val bz = z - sinA
                val cx = x - sinA
                val cz = z - cosA
                val dx = x - cosA
                val dz = z + sinA

                val ay =
                    y + ((HatOrbsSettings.OrbSettings.size + HatOrbsSettings.OrbSettings.yOffset) * HatOrbsSettings.OrbSettings.centreOffset).coerceIn(
                        bottom,
                        top
                    )

                val color = if (!isHurt) color else Color4b(255, 0, 0, color.a)
                // Rendering of the top part of the rhombus (4 faces/8 triangles).
                addVertex(matrix, x, top, z).setColor(color)
                addVertex(matrix, dx, ay, dz).setColor(color)
                addVertex(matrix, ax, ay, az).setColor(color)
                addVertex(matrix, x, top, z).setColor(color)
                addVertex(matrix, ax, ay, az).setColor(color)
                addVertex(matrix, bx, ay, bz).setColor(color)
                addVertex(matrix, x, top, z).setColor(color)
                addVertex(matrix, bx, ay, bz).setColor(color)
                addVertex(matrix, cx, ay, cz).setColor(color)
                addVertex(matrix, x, top, z).setColor(color)
                addVertex(matrix, cx, ay, cz).setColor(color)
                addVertex(matrix, dx, ay, dz).setColor(color)

                // Rendering of the bottom part of the rhombus (4 faces/8 triangles).
                addVertex(matrix, x, bottom, z).setColor(color)
                addVertex(matrix, dx, ay, dz).setColor(color)
                addVertex(matrix, ax, ay, az).setColor(color)
                addVertex(matrix, x, bottom, z).setColor(color)
                addVertex(matrix, ax, ay, az).setColor(color)
                addVertex(matrix, bx, ay, bz).setColor(color)
                addVertex(matrix, x, bottom, z).setColor(color)
                addVertex(matrix, bx, ay, bz).setColor(color)
                addVertex(matrix, cx, ay, cz).setColor(color)
                addVertex(matrix, x, bottom, z).setColor(color)
                addVertex(matrix, cx, ay, cz).setColor(color)
                addVertex(matrix, dx, ay, dz).setColor(color)
            }
        }
    }

    private fun getPointX(angle: Float, radius: Float) = sin(angle) * radius
    private fun getPointZ(angle: Float, radius: Float) = cos(angle) * radius

}
