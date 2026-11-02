package ru.avtovazmod.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import ru.avtovazmod.AvtovazMod;
import ru.avtovazmod.entity.Vaz2107Entity;
import ru.avtovazmod.entity.VazNivaEntity;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, AvtovazMod.MODID);

    /** ВАЗ-2107 «Семёрка». Хитбокс — квадрат 2.0 x 2.0, высота 1.6 (AABB не вращается). */
    public static final DeferredHolder<EntityType<?>, EntityType<Vaz2107Entity>> VAZ_2107 =
            ENTITIES.register("vaz2107", () -> EntityType.Builder
                    .of(Vaz2107Entity::new, MobCategory.MISC)
                    .sized(2.0F, 1.6F)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build("vaz2107"));

    /** ВАЗ-2121 «Нива». */
    public static final DeferredHolder<EntityType<?>, EntityType<VazNivaEntity>> VAZ_NIVA =
            ENTITIES.register("vaz_niva", () -> EntityType.Builder
                    .of(VazNivaEntity::new, MobCategory.MISC)
                    .sized(2.0F, 1.8F)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build("vaz_niva"));

    private ModEntities() {
    }
}
