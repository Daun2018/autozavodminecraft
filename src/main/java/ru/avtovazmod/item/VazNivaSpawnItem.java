package ru.avtovazmod.item;

import net.minecraft.world.entity.EntityType;
import ru.avtovazmod.entity.AbstractVazCarEntity;
import ru.avtovazmod.registry.ModEntities;

public class VazNivaSpawnItem extends AbstractCarSpawnItem {

    public VazNivaSpawnItem(Properties properties) {
        super(properties);
    }

    @Override
    protected EntityType<? extends AbstractVazCarEntity> carType() {
        return ModEntities.VAZ_NIVA.get();
    }
}
