package com.github.bedwulf.mcaguardappearance.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;
import tallestegg.guardvillagers.entities.Guard;

/**
 * Временный минимальный рендерер этапа B: человеческая модель vanilla
 * (PLAYER-геометрия, носа нет) без кастомных поз. На этапе C будет расширен
 * собственной HumanGuardModel, позами рук и слоем брони.
 */
public class HumanGuardRenderer extends HumanoidMobRenderer<Guard, HumanoidModel<Guard>> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("textures/entity/player/wide_steve.png");

    public HumanGuardRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeMesh(ModelLayers.PLAYER), 0.0F), 0.5F);
    }

    @Override
    protected void scale(Guard entity, PoseStack poseStack, float partialTick) {
        // масштаб стража подтверждён байткодом оригинального GuardRenderer: 0.9375
        poseStack.scale(0.9375F, 0.9375F, 0.9375F);
    }

    @Override
    public ResourceLocation getTextureLocation(Guard entity) {
        return TEXTURE;
    }
}
