package ru.avtovazmod.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import ru.avtovazmod.registry.ModItems;

/**
 * ВАЗ-2121 «Нива»: полный привод (устойчива, срыв в занос только с ручником),
 * высота подъёма на блоки StepHeight = 1.0, не теряет скорость на песке/снегу/грязи.
 */
public class VazNivaEntity extends AbstractVazCarEntity {
    private static final CarStats STATS = new CarStats(
            0.62D,      // maxSpeed
            0.019D,     // acceleration
            0.20D,      // maxReverse
            0.012D,     // reverseAcceleration
            0.05D,      // brake
            3.8F,       // turnRate
            0.60D,      // grip
            0.22D,      // handbrakeGrip
            false,      // rearWheelDrive (4x4)
            1.0D,       // looseSurfaceFactor — без потери скорости
            1.0F,       // stepHeight = 1.0
            6.0F / 16.0F, // wheelRadius
            -0.1875D,   // seatFrontFwd
            -0.6875D,   // seatRearFwd
            0.08D       // seatYOffset
    );

    public VazNivaEntity(EntityType<? extends VazNivaEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public CarStats stats() {
        return STATS;
    }

    @Override
    public Item getSpawnItem() {
        return ModItems.VAZ_NIVA_SPAWN.get();
    }
}
