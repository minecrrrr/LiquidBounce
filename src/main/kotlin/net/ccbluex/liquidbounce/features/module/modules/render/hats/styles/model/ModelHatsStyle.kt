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

package net.ccbluex.liquidbounce.features.module.modules.render.hats.styles.model

import com.mojang.blaze3d.platform.NativeImage
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.math.Axis
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.LiquidBounce.logger
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.features.misc.FriendManager
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleFreeCam
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleFreeLook
import net.ccbluex.liquidbounce.features.module.modules.render.hats.ModuleHats
import net.ccbluex.liquidbounce.features.module.modules.render.hats.ModuleHats.HeightOffset
import net.ccbluex.liquidbounce.features.module.modules.render.hats.styles.StyleMode
import net.ccbluex.liquidbounce.features.module.modules.render.hats.styles.model.ModelHatsStyle.Rotation.pitch
import net.ccbluex.liquidbounce.features.module.modules.render.hats.styles.model.ModelHatsStyle.Rotation.roll
import net.ccbluex.liquidbounce.features.module.modules.render.hats.styles.model.ModelHatsStyle.Rotation.yaw
import net.ccbluex.liquidbounce.render.EntityModelAttachment
import net.ccbluex.liquidbounce.render.ModelAnchor
import net.ccbluex.liquidbounce.render.geometry.GeometryModel
import net.ccbluex.liquidbounce.render.geometry.GeometryParser
import net.ccbluex.liquidbounce.render.geometry.emit
import net.ccbluex.liquidbounce.utils.render.asTexture
import net.minecraft.client.renderer.entity.state.EntityRenderState
import net.minecraft.client.renderer.rendertype.RenderType
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity

internal object ModelHatsStyle : StyleMode("Model"), EntityModelAttachment {
    override val anchor: ModelAnchor = ModelAnchor.HEAD

    private val hat by enumChoice("Hat", HatPreset.CHRISTMAS).onChange { reload(it); it }
    private val equipmentOffset by float("ArmorOffset", 0.1875f, 0f..1f)
    private val height = HeightOffset
    private val scale by float("Scale", 1f, 0f..10f)

    private class FriendsOptions : ToggleableValueGroup(this, "FriendsOptions", true) {
        val distance by int("Distance", 64, 8..512, "blocks")
    }

    object Rotation : ValueGroup("Rotation") {
        val yaw by float("Yaw", 0f, -180f..180f, "Deg")
        val pitch by float("Pitch", 0f, -180f..180f, "Deg")
        val roll by float("Roll", 0f, -180f..180f, "Deg")
    }

    private val friendsOptions = tree(FriendsOptions())

    @Volatile
    private var model: GeometryModel? = null

    @Volatile
    private var activeRenderType: RenderType? = null

    private fun reload(preset: HatPreset = hat) {
        runCatching {
            model = GeometryParser.parse(preset.model, preset.texture)

            val id = Identifier.fromNamespaceAndPath("liquidbounce", preset.texture)
            val tex = LiquidBounce.resource(preset.texture).use { NativeImage.read(it) }.asTexture { preset.texture }
            mc.textureManager.register(id, tex)
            activeRenderType = RenderTypes.entityCutout(id)
        }.onFailure {
            logger.error("Failed to load hat model ${preset.model}", it)
            model = null
            activeRenderType = null
        }
    }

    init {
        tree(Rotation)
        reload()
    }

    override val detail: String
        get() = hat.tag

    override fun isVisible(entity: LivingEntity, state: EntityRenderState): Boolean {
        return (ModuleHats.running && running) && if (entity === mc.player) {
            !mc.options.cameraType.isFirstPerson || ModuleFreeLook.enabled || ModuleFreeCam.enabled
        } else {
            friendsOptions.enabled &&
                FriendManager.isFriend(entity) &&
                player.distanceTo(entity) <= friendsOptions.distance
        }
    }

    override fun emit(pose: PoseStack.Pose, vc: VertexConsumer, light: Int, entity: LivingEntity) {
        val m = model ?: return

        pose.scale(scale, -scale, scale)
        pose.translate(
            0f,
            height.current() + if (!entity.getItemBySlot(EquipmentSlot.HEAD).isEmpty) equipmentOffset else 0f,
            0f
        )
        pose.rotateDegrees(Axis.XP, pitch)
        pose.rotateDegrees(Axis.YP, yaw)
        pose.rotateDegrees(Axis.ZP, roll)

        m.emit { x, y, z, u, v ->
            vc.addVertex(pose, x, y, z)
                .setColor(-1)
                .setUv(u, v)
                .setLight(light)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setNormal(pose, 0f, 1f, 0f)
        }
    }

    override fun renderType(): RenderType? = activeRenderType

    @Suppress("unused")
    enum class HatPreset(override val tag: String) : Tagged {
        CHRISTMAS("Christmas");

        val model get() = "hats/models/${name.lowercase()}.json"
        val texture get() = "hats/textures/${name.lowercase()}.png"
    }
}
