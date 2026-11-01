/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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

package net.ccbluex.liquidbounce.features.module.modules.render.hats

import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.modules.render.hats.modes.legacy.LegacyHatsCone
import net.ccbluex.liquidbounce.features.module.modules.render.hats.modes.legacy.LegacyHatsFlower
import net.ccbluex.liquidbounce.features.module.modules.render.hats.modes.legacy.LegacyHatsHalo
import net.ccbluex.liquidbounce.features.module.modules.render.hats.modes.legacy.LegacyHatsImage
import net.ccbluex.liquidbounce.features.module.modules.render.hats.modes.legacy.LegacyHatsOrbs
import net.ccbluex.liquidbounce.features.module.modules.render.hats.modes.legacy.LegacyHatsStar
import net.ccbluex.liquidbounce.features.module.modules.render.hats.modes.model.ModelHatsTest
import net.ccbluex.liquidbounce.render.utils.AnimatedValueGroup
import org.joml.Vector2f

object ModuleHats : ClientModule("Hats", ModuleCategories.RENDER) {
    override val tag: String
        get() = styles.activeMode.let { style -> "${style.hats.activeMode.name} (${style.name})" }

    val canBeCovered by boolean("CanBeCovered", default = false)

    object HeightOffset : AnimatedValueGroup("HeightOffset") {
        override val curve = curve("Height") {
            "Progress" x 0f..1f
            "Offset" y 0f..2f
            points(Vector2f(0f, 0.2f), Vector2f(1f, 0.2f))
        }
    }

    init {
        tree(HeightOffset)
    }

    val styles = choices("Style", 0) {
        arrayOf(
            LegacyHatsStyle,
            ModelHatsStyle
        )
    }

    abstract class StyleMode(name: String) : Mode(name) {
        final override val parent: ModeValueGroup<*>
            get() = styles

        abstract val hats: ModeValueGroup<*>
    }

    internal object LegacyHatsStyle : StyleMode("Legacy") {
        override val hats = modes("Hat", 0) {
            arrayOf(
                LegacyHatsHalo,
                LegacyHatsCone,
                LegacyHatsOrbs,
                LegacyHatsFlower,
                LegacyHatsStar,
                LegacyHatsImage
            )
        }
    }

    internal object ModelHatsStyle : StyleMode("Model") {
        override val hats = modes("Hat", 0) {
            arrayOf(
                ModelHatsTest
            )
        }
    }
}
