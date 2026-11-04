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

package net.ccbluex.liquidbounce.render

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.model.EntityModel
import net.minecraft.client.model.HeadedModel
import net.minecraft.client.model.HumanoidModel
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.entity.state.EntityRenderState
import net.minecraft.client.renderer.rendertype.RenderType
import net.minecraft.world.entity.LivingEntity
import java.util.concurrent.CopyOnWriteArrayList

interface EntityModelAttachment {
    val anchor: ModelAnchor
    fun isVisible(entity: LivingEntity, state: EntityRenderState): Boolean
    fun renderType(): RenderType?
    fun emit(pose: PoseStack.Pose, vc: VertexConsumer, light: Int, entity: LivingEntity)
}

object EntityModelAttachments {
    private val attachments = CopyOnWriteArrayList<EntityModelAttachment>()

    fun register(attachment: EntityModelAttachment) { attachments += attachment }

    fun submit(
        model: EntityModel<*>,
        state: EntityRenderState,
        entity: LivingEntity,
        poseStack: PoseStack,
        collector: SubmitNodeCollector
    ) {
        for (attachment in attachments) {
            if (!attachment.isVisible(entity, state)) continue
            val type = attachment.renderType() ?: continue

            poseStack.pushPose()
            model.root().translateAndRotate(poseStack)
            attachment.anchor.translate(model, poseStack)
            collector.submitCustomGeometry(poseStack, type) { p, vc ->
                attachment.emit(p, vc, state.lightCoords, entity)
            }
            poseStack.popPose()
        }
    }
}

enum class ModelAnchor {
    HEAD, BODY, LEFT_ARM, RIGHT_ARM, LEFT_LEG, RIGHT_LEG;

    fun translate(model: EntityModel<*>, pose: PoseStack) {
        if (this == HEAD) {
            (model as HeadedModel).translateToHead(pose)
            return
        }
        val humanoid = (model as HumanoidModel<*>)
        when (this) {
            BODY -> humanoid.body
            LEFT_ARM -> humanoid.leftArm
            RIGHT_ARM -> humanoid.rightArm
            LEFT_LEG -> humanoid.leftLeg
            RIGHT_LEG -> humanoid.rightLeg
        }.translateAndRotate(pose)
    }
}
