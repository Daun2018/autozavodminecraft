package ru.avtovazmod.client;

import net.minecraft.client.Minecraft;
import ru.avtovazmod.client.sound.CarEngineSoundInstance;
import ru.avtovazmod.entity.AbstractVazCarEntity;

/**
 * Единственная точка, через которую общий (серверный) код сущности обращается к клиентским классам.
 * Метод вызывается только при {@code level().isClientSide}, поэтому на выделенном сервере класс не загружается.
 */
public final class CarClientHooks {

    public static void startEngineSound(AbstractVazCarEntity car) {
        Minecraft.getInstance().getSoundManager().play(new CarEngineSoundInstance(car));
    }

    private CarClientHooks() {
    }
}
