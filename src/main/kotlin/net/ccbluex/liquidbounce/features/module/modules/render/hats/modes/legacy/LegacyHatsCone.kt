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

package net.ccbluex.liquidbounce.features.module.modules.render.hats.modes.legacy

import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.features.module.modules.render.hats.HatsColorSettings
import net.ccbluex.liquidbounce.features.module.modules.render.hats.ModuleHats.canBeCovered
import net.ccbluex.liquidbounce.features.module.modules.render.hats.modes.legacy.LegacyHatsCone.HatConeSettings.outerRadius
import net.ccbluex.liquidbounce.features.module.modules.render.hats.modes.legacy.LegacyHatsCone.HatConeSettings.peak
import net.ccbluex.liquidbounce.render.ClientRenderPipelines
import net.ccbluex.liquidbounce.render.WorldRenderEnvironment
import net.ccbluex.liquidbounce.render.drawCustomMesh
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.segmentAngle
import net.ccbluex.liquidbounce.render.setColor
import kotlin.math.cos
import kotlin.math.sin

internal object LegacyHatsCone : LegacyHatsMode("Cone") {

    private val colors = HatsColorSettings()

    private object HatConeSettings : ValueGroup("HatSettings") {
        val outerRadius by float("OuterRadius", 0.6f, 0.1f..2f)
        val peak by float("Peak", 0.3f, 0.01f..2f)
    }

    init {
        tree(HatConeSettings)
        tree(colors)
    }

    override fun WorldRenderEnvironment.drawLegacyHat(isHurt: Boolean) {
        drawCustomMesh(ClientRenderPipelines.triangles(noDepthTest = !canBeCovered)) { matrix ->
            val segments = 128
            for (i in 0 until segments) {
                val angle = segmentAngle(i, segments)
                val nextAngle = segmentAngle(i + 1, segments)
                val cosine = cos(angle)
                val sine = sin(angle)
                val nextCosine = cos(nextAngle)
                val nextSine = sin(nextAngle)

                val color = if (!isHurt) colors.getCurrentStepColor(angle) else Color4b(255, 0, 0, colors.firstColor.a)

                addVertex(
                    matrix,
                    cosine * outerRadius,
                    0f,
                    sine * outerRadius
                ).setColor(color)
                addVertex(
                    matrix,
                    nextCosine * outerRadius,
                    0f,
                    nextSine * outerRadius
                ).setColor(color)
                addVertex(matrix, 0f, peak, 0f).setColor(color)
            }
        }
    }
}
