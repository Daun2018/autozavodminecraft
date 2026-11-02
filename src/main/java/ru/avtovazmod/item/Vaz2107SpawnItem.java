package ru.avtovazmod.item;

import net.minecraft.world.entity.EntityType;
import ru.avtovazmod.entity.AbstractVazCarEntity;
import ru.avtovazmod.registry.ModEntities;

public class Vaz2107SpawnItem extends AbstractCarSpawnItem {

    public Vaz2107SpawnItem(Properties properties) {
        super(properties);
    }

    @Override
    protected EntityType<? extends AbstractVazCarEntity> carType() {
        return ModEntities.VAZ_2107.get();
    }
}
