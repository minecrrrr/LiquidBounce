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

package net.ccbluex.liquidbounce.features.module.modules.render.hats

import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.features.misc.FriendManager
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleFreeLook
import net.minecraft.world.entity.LivingEntity

abstract class HatsMode(name: String) : Mode(name) {

    protected fun shouldRender(
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
