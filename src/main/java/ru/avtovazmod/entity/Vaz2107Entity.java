package ru.avtovazmod.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import ru.avtovazmod.registry.ModItems;

/**
 * ВАЗ-2107 «Семёрка»: задний привод, средний разгон, на скорости с газом и рулём срывается в занос,
 * ручник (Пробел) даёт управляемый дрифт. На песке/снегу/грязи теряет скорость, высота подъёма 0.6.
 */
public class Vaz2107Entity extends AbstractVazCarEntity {
    private static final CarStats STATS = new CarStats(
            0.70D,      // maxSpeed  (~14 бл/с)
            0.022D,     // acceleration
            0.22D,      // maxReverse
            0.012D,     // reverseAcceleration
            0.05D,      // brake
            4.4F,       // turnRate
            0.50D,      // grip
            0.14D,      // handbrakeGrip
            true,       // rearWheelDrive
            0.55D,      // looseSurfaceFactor
            0.6F,       // stepHeight
            5.0F / 16.0F, // wheelRadius
            -0.25D,     // seatFrontFwd
            -0.625D,    // seatRearFwd
            -0.05D      // seatYOffset
    );

    public Vaz2107Entity(EntityType<? extends Vaz2107Entity> type, Level level) {
        super(type, level);
    }

    @Override
    public CarStats stats() {
        return STATS;
    }

    @Override
    public Item getSpawnItem() {
        return ModItems.VAZ_2107_SPAWN.get();
    }
}
