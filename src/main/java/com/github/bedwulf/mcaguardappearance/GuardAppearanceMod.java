package com.github.bedwulf.mcaguardappearance;

import net.minecraftforge.fml.common.Mod;

/**
 * MCA Guard Appearance — клиентский мод, заменяющий рендерер стражников
 * Guard Villagers на человеческую модель без носа.
 *
 * Серверную логику (AI, экипировку, спавн, сохранение) не затрагивает:
 * весь код работает только на клиенте (см. ClientEvents, Dist.CLIENT).
 */
@Mod(GuardAppearanceMod.MODID)
public final class GuardAppearanceMod {
    public static final String MODID = "mcaguardappearance";

    public GuardAppearanceMod() {
        // Регистрация обработчиков выполняется через @Mod.EventBusSubscriber(Dist.CLIENT)
        // в классе ClientEvents — на dedicated server он не загружается.
    }
}
