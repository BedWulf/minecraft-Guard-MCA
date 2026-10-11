package com.github.bedwulf.mcaguardappearance;

import com.github.bedwulf.mcaguardappearance.client.HumanGuardRenderer;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tallestegg.guardvillagers.GuardEntityType;

/**
 * Клиентские события. Загружается только на стороне клиента (Dist.CLIENT),
 * поэтому обращения к клиентским классам на dedicated server невозможны.
 *
 * Мы подписываемся на то же штатное событие Forge (RegisterRenderers), что и
 * Guard Villagers. Так как в mods.toml указано ordering="AFTER" для guardvillagers,
 * наш обработчик вызывается после их обработчика и EntityRenderers.put перезаписывает
 * провайдер рендерера — подмена без reflection и mixin.
 */
@Mod.EventBusSubscriber(modid = GuardAppearanceMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ClientEvents {

    private ClientEvents() {}

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // GuardEntityType.GUARD — подтверждённый путь к RegistryObject<EntityType<Guard>> (javap).
        EntityRenderers.register(GuardEntityType.GUARD.get(), HumanGuardRenderer::new);
    }
}
