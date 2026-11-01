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
import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.misc.FriendManager
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleFreeLook
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleRotations
import net.ccbluex.liquidbounce.features.module.modules.render.hats.ModuleHats.HeightOffset
import net.ccbluex.liquidbounce.features.module.modules.render.hats.ModuleHats.modes
import net.ccbluex.liquidbounce.render.WorldRenderEnvironment
import net.ccbluex.liquidbounce.render.renderEnvironment
import net.ccbluex.liquidbounce.render.withPositionRelativeToCamera
import net.ccbluex.liquidbounce.render.withPush
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.minecraft.util.Mth
import net.minecraft.world.entity.EquipmentSlot
import org.joml.Quaternionf

abstract class HatsMode(name: String) : Mode(name) {
    final override val parent: ModeValueGroup<*>
        get() = modes

    // --- Settings ---
    private val equipmentOffset by float("ArmorOffset", 0.1f, 0f..1f)
    private val hurtMarked by boolean("ShowDamage", true)

    private class FriendsOptions : ValueGroup("FriendsOptions") {
        val friendView by boolean("ViewOnFriend", true)
        val distance by int("Distance", 64, 8..512, "blocks")
    }

    private val friendsOptions = tree(FriendsOptions())

    protected val showInFirstPerson by boolean("FirstPersonView", false)

    // --- Render ---
    protected abstract fun WorldRenderEnvironment.drawHat(isHurt: Boolean)

    @Suppress("unused")
    private val renderHandler = handler<WorldRenderEvent> { event ->
        val player = mc.player ?: return@handler

        event.renderEnvironment {
            for (entity in world.players()) {
                val isMe = entity == player
                val isFriend = FriendManager.isFriend(entity)
                val inDistance = player.distanceTo(entity) <= friendsOptions.distance

                val shouldRender = if (isMe) {
                    !mc.options.cameraType.isFirstPerson || showInFirstPerson || ModuleFreeLook.enabled
                } else {
                    inDistance && isFriend && friendsOptions.friendView
                }

                if (shouldRender) {
                    val hurtMarked = entity.hurtTime > 0 && hurtMarked

                    val equipOffset = if
                        (!entity.getItemBySlot(EquipmentSlot.HEAD).isEmpty) { equipmentOffset } else { 0.0F }

                    val pos = entity.interpolateCurrentPosition(event.partialTicks)

                    val rot = ModuleRotations.modelRotation.takeIf { ModuleRotations.enabled }
                    val prevRot = ModuleRotations.prevModelRotation.takeIf { ModuleRotations.enabled }

                    val headYaw =
                        Mth.rotLerp(event.partialTicks, prevRot?.yRot ?: entity.yHeadRotO, rot?.yRot ?: entity.yHeadRot)
                    val pitch =
                        Mth.lerp(event.partialTicks, prevRot?.xRot ?: entity.xRotO, rot?.xRot ?: entity.xRot)

                    val flatPos = entity.isVisuallySwimming || entity.isFallFlying || entity.isVisuallyCrawling
                    val neck = if (flatPos) entity.bbHeight * 0.6 else entity.eyeHeight - 0.21
                    val forward = if (flatPos) 0.5f else 0f
                    val height = HeightOffset.current()

                    withPositionRelativeToCamera(pos.add(0.0, neck, 0.0)) {
                        poseStack.withPush {
                            rotate(Quaternionf().rotationY(Math.toRadians(-headYaw.toDouble()).toFloat()))
                            rotate(Quaternionf().rotationX(Math.toRadians(pitch.toDouble()).toFloat()))

                            translate(0F, 0.35f + height + equipOffset, forward)
                            drawHat(hurtMarked)
                        }
                    }
                }
            }
        }
    }

    protected inline fun WorldRenderEnvironment.withHatRotation(
        angle: Float,
        block: WorldRenderEnvironment.() -> Unit,
    ) {
        poseStack.withPush {
            if (!Mth.equal(angle, 0f)) rotate(Quaternionf().rotationY(angle))
            block()
        }
    }

    protected fun getRotationAngle(speed: Float): Float {
        return if (Mth.equal(speed, 0f)) {
            0f
        } else {
            (System.currentTimeMillis() % 360000) * 0.001F * speed
        }
    }

}
