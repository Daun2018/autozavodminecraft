package ru.avtovazmod.client.sound;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import ru.avtovazmod.entity.AbstractVazCarEntity;
import ru.avtovazmod.registry.ModSounds;

/** Звук двигателя: слышен у всех, пока в машине есть водитель; высота и громкость растут со скоростью. */
public class CarEngineSoundInstance extends AbstractTickableSoundInstance {
    private final AbstractVazCarEntity car;

    public CarEngineSoundInstance(AbstractVazCarEntity car) {
        super(ModSounds.ENGINE_LOOP.get(), SoundSource.NEUTRAL, SoundInstance.createUnseededRandom());
        this.car = car;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.3F;
        this.pitch = 0.65F;
        this.x = car.getX();
        this.y = car.getY();
        this.z = car.getZ();
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {
        if (this.car.isRemoved() || this.car.getControllingPassenger() == null) {
            this.car.setEngineSoundActive(false);
            this.stop();
            return;
        }
        this.x = this.car.getX();
        this.y = this.car.getY();
        this.z = this.car.getZ();
        float t = Mth.clamp((float) (this.car.getHorizontalSpeed() / this.car.stats().maxSpeed()), 0.0F, 1.0F);
        this.pitch = 0.65F + 0.95F * t;
        this.volume = 0.35F + 0.45F * t;
    }
}
