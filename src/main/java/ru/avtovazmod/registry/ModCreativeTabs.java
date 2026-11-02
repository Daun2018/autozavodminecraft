package ru.avtovazmod.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import ru.avtovazmod.AvtovazMod;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AvtovazMod.MODID);

    /** Вкладка «АвтоВАЗ Мод» (название — в lang: itemGroup.avtovazmod.main). */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + AvtovazMod.MODID + ".main"))
                    .icon(() -> new ItemStack(ModItems.VAZ_2107_SPAWN.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.VAZ_2107_SPAWN.get());
                        output.accept(ModItems.VAZ_NIVA_SPAWN.get());
                    })
                    .build());

    private ModCreativeTabs() {
    }
}
