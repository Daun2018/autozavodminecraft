package ru.avtovazmod.registry;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import ru.avtovazmod.AvtovazMod;
import ru.avtovazmod.item.Vaz2107SpawnItem;
import ru.avtovazmod.item.VazNivaSpawnItem;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(AvtovazMod.MODID);

    public static final DeferredItem<Vaz2107SpawnItem> VAZ_2107_SPAWN =
            ITEMS.register("vaz2107_spawn", () -> new Vaz2107SpawnItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<VazNivaSpawnItem> VAZ_NIVA_SPAWN =
            ITEMS.register("vaz_niva_spawn", () -> new VazNivaSpawnItem(new Item.Properties().stacksTo(1)));

    private ModItems() {
    }
}
