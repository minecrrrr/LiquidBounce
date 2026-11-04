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

import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.RefreshArrayListEvent
import net.ccbluex.liquidbounce.features.misc.FriendManager
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleFreeLook
import net.ccbluex.liquidbounce.features.module.modules.render.hats.styles.StyleMode
import net.ccbluex.liquidbounce.features.module.modules.render.hats.styles.legacy.modes.LegacyHatsCone
import net.ccbluex.liquidbounce.features.module.modules.render.hats.styles.legacy.modes.LegacyHatsFlower
import net.ccbluex.liquidbounce.features.module.modules.render.hats.styles.legacy.modes.LegacyHatsHalo
import net.ccbluex.liquidbounce.features.module.modules.render.hats.styles.legacy.modes.LegacyHatsImage
import net.ccbluex.liquidbounce.features.module.modules.render.hats.styles.legacy.modes.LegacyHatsOrbs
import net.ccbluex.liquidbounce.features.module.modules.render.hats.styles.legacy.modes.LegacyHatsStar
import net.ccbluex.liquidbounce.features.module.modules.render.hats.styles.model.ModelHatsStyle
import net.ccbluex.liquidbounce.render.EntityModelAttachments
import net.ccbluex.liquidbounce.render.utils.AnimatedValueGroup
import net.minecraft.world.entity.LivingEntity
import org.joml.Vector2f

object ModuleHats : ClientModule("Hats", ModuleCategories.RENDER) {
    override val tag: String
        get() = styles.activeMode.let { style ->
            style.detail?.let { "$it (${style.name})" } ?: style.name
        }

    object HeightOffset : AnimatedValueGroup("HeightOffset") {
        override val curve = curve("Height") {
            "Progress" x 0f..1f
            "Offset" y 0f..2f
            points(Vector2f(0f, 0.2f), Vector2f(1f, 0.2f))
        }
    }

    init {
        tree(HeightOffset)
        EntityModelAttachments.register(ModelHatsStyle)
    }

    val styles = choices("Style", 0) {
        arrayOf(LegacyHatsStyle, ModelHatsStyle)
    }.apply { onChanged { EventManager.callEvent(RefreshArrayListEvent) } }

    internal object LegacyHatsStyle : StyleMode("Legacy") {
        val hats = modes("Hat", 0) {
            arrayOf(
                LegacyHatsHalo,
                LegacyHatsCone,
                LegacyHatsOrbs,
                LegacyHatsFlower,
                LegacyHatsStar,
                LegacyHatsImage
            )
        }.apply { onChanged { EventManager.callEvent(RefreshArrayListEvent) } }

        override val detail: String
            get() = hats.activeMode.name
    }

    fun shouldRender(
        entity: LivingEntity,
        showInFirstPerson: Boolean,
        allowFriends: Boolean,
        distance: Int
    ): Boolean {
        val isMe = entity == player
        val renderFriends = FriendManager.isFriend(entity) && allowFriends
        val inDistance = player.distanceTo(entity) <= distance

        return when {
            isMe -> !mc.options.cameraType.isFirstPerson || showInFirstPerson || ModuleFreeLook.enabled
            else -> inDistance && renderFriends
        }
    }
}
