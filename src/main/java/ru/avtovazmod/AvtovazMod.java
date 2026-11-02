package ru.avtovazmod;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import ru.avtovazmod.registry.ModCreativeTabs;
import ru.avtovazmod.registry.ModEntities;
import ru.avtovazmod.registry.ModItems;
import ru.avtovazmod.registry.ModSounds;

/** Главный класс мода. Вся регистрация — через DeferredRegister. */
@Mod(AvtovazMod.MODID)
public class AvtovazMod {
    public static final String MODID = "avtovazmod";

    public AvtovazMod(IEventBus modEventBus) {
        ModEntities.ENTITIES.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModSounds.SOUNDS.register(modEventBus);
        ModCreativeTabs.TABS.register(modEventBus);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
