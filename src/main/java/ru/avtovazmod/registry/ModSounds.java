package ru.avtovazmod.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import ru.avtovazmod.AvtovazMod;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, AvtovazMod.MODID);

    /** Зацикленный звук двигателя (assets/avtovazmod/sounds/car/engine.ogg, описан в sounds.json). */
    public static final DeferredHolder<SoundEvent, SoundEvent> ENGINE_LOOP = SOUNDS.register("car.engine",
            () -> SoundEvent.createVariableRangeEvent(AvtovazMod.id("car.engine")));

    private ModSounds() {
    }
}
