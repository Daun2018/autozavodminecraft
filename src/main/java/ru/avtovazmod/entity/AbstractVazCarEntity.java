package ru.avtovazmod.entity;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import ru.avtovazmod.client.CarClientHooks;

/**
 * Базовый класс машины АвтоВАЗа.
 *
 * <h2>Почему {@code Entity}, а не {@code LivingEntity}/{@code VehicleEntity}</h2>
 * Машине не нужны здоровье, эффекты, ИИ и броня, зато нужна «лодочная» схема синхронизации:
 * физику считает <b>клиент водителя</b> ({@link #isControlledByLocalInstance()}), а сервер принимает позицию
 * из {@code ServerboundMoveVehiclePacket}. Остальные клиенты получают позицию и интерполируют её в
 * {@link #lerpTo}. Когда водителя нет, физику (инерцию, торможение) считает сервер.
 *
 * <h2>Физика</h2>
 * Скорость раскладывается в системе координат корпуса на продольную {@code vf} и боковую {@code vr}:
 * <ul>
 *   <li>газ/тормоз/задний ход меняют {@code vf}; без газа — накат с сопротивлением;</li>
 *   <li>боковая составляющая {@code vr} гасится «сцеплением» {@code grip} — ручник и срыв задней оси
 *       уменьшают сцепление, и машина скользит (дрифт);</li>
 *   <li>угол поворота руля сглаживается и зависит от скорости: на месте машина не поворачивает,
 *       на высокой скорости руль «тяжелее».</li>
 * </ul>
 */
public abstract class AbstractVazCarEntity extends Entity {

    public static final int MAX_SEATS = 4;

    private static final float DAMAGE_LIMIT = 40.0F;
    private static final double RAM_MIN_SPEED = 0.28D;
    private static final double SEAT_LATERAL = 0.37D;
    private static final float MAX_LOOK_ANGLE = 105.0F;

    private static final EntityDataAccessor<Float> DATA_DAMAGE =
            SynchedEntityData.defineId(AbstractVazCarEntity.class, EntityDataSerializers.FLOAT);

    // --- интерполяция позиции у наблюдателей ---
    private int lerpSteps;
    private double lerpX;
    private double lerpY;
    private double lerpZ;
    private float lerpYRot;
    private float lerpXRot;

    // --- управление ---
    /** Сглаженный руль: -1..1, положительное значение = поворот направо (по часовой, yaw растёт). */
    private float steerInput;

    // --- визуальное состояние (используется рендером) ---
    public float wheelRot;
    public float wheelRotO;
    /** Угол поворота передних колёс в градусах (+ = направо). */
    public float steer;
    public float steerO;

    private boolean engineSoundActive;

    protected AbstractVazCarEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.blocksBuilding = true;
    }

    /** Ходовые качества конкретной модели. */
    public abstract CarStats stats();

    /** Предмет, в который превращается машина (дроп и средняя кнопка мыши). */
    public abstract Item getSpawnItem();

    // =====================================================================
    // Синхронизируемые данные / NBT
    // =====================================================================

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_DAMAGE, 0.0F);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    public float getDamage() {
        return this.entityData.get(DATA_DAMAGE);
    }

    public void setDamage(float damage) {
        this.entityData.set(DATA_DAMAGE, damage);
    }

    // =====================================================================
    // Свойства сущности, коллизии
    // =====================================================================

    /** Твёрдый хитбокс: на машину можно встать, об неё упираются другие транспортные средства. */
    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public boolean isPickable() {
        return !this.isRemoved();
    }

    @Override
    public boolean isPushable() {
        return true;
    }

    /** Высота ступеньки: у «Нивы» 1.0, у «Семёрки» 0.6. */
    @Override
    public float maxUpStep() {
        return stats().stepHeight();
    }

    /** Замедление на песке/снегу/грязи считаем сами (см. {@link #simulate}), ванильное отключаем. */
    @Override
    protected float getBlockSpeedFactor() {
        return 1.0F;
    }

    @Override
    protected MovementEmission getMovementEmission() {
        return MovementEmission.NONE;
    }

    /** Модель длиннее хитбокса, поэтому расширяем зону отсечения, чтобы кузов не пропадал на краю экрана. */
    @Override
    public AABB getBoundingBoxForCulling() {
        return this.getBoundingBox().inflate(1.0D);
    }

    @Nullable
    @Override
    public ItemStack getPickResult() {
        return new ItemStack(getSpawnItem());
    }

    // =====================================================================
    // Посадка
    // =====================================================================

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        return this.getFirstPassenger() instanceof LivingEntity living ? living : null;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return this.getPassengers().size() < MAX_SEATS;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (player.isSecondaryUseActive()) {
            // Shift + ПКМ: «скрутить» машину обратно в предмет (если в ней никого нет)
            if (!this.getPassengers().isEmpty()) {
                return InteractionResult.PASS;
            }
            if (!this.level().isClientSide) {
                dropAsItem();
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        if (this.getPassengers().size() >= MAX_SEATS) {
            return InteractionResult.PASS;
        }
        if (!this.level().isClientSide) {
            return player.startRiding(this) ? InteractionResult.CONSUME : InteractionResult.PASS;
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * Места: 0 — водитель (слева спереди), 1 — переднее правое, 2/3 — задний ряд.
     * Смещение считается в системе корпуса и поворачивается на угол машины.
     * Локальная ось X направлена влево по ходу движения, Z — вперёд.
     */
    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction callback) {
        if (!this.hasPassenger(passenger)) {
            return;
        }
        int seat = Math.min(this.getPassengers().indexOf(passenger), MAX_SEATS - 1);
        CarStats s = stats();
        boolean left = (seat & 1) == 0;
        boolean rear = seat >= 2;
        double lateral = left ? SEAT_LATERAL : -SEAT_LATERAL;
        double forward = rear ? s.seatRearFwd() : s.seatFrontFwd();
        Vec3 offset = new Vec3(lateral, 0.0D, forward).yRot(-this.getYRot() * Mth.DEG_TO_RAD);
        callback.accept(passenger, this.getX() + offset.x, this.getY() + s.seatYOffset(), this.getZ() + offset.z);
        clampRotation(passenger);
    }

    @Override
    public void onPassengerTurned(Entity passenger) {
        clampRotation(passenger);
    }

    /** Корпус пассажира следует за машиной, голова может поворачиваться не больше чем на ±105°. */
    private void clampRotation(Entity entity) {
        entity.setYBodyRot(this.getYRot());
        float diff = Mth.wrapDegrees(entity.getYRot() - this.getYRot());
        float clamped = Mth.clamp(diff, -MAX_LOOK_ANGLE, MAX_LOOK_ANGLE);
        entity.yRotO += clamped - diff;
        entity.setYRot(entity.getYRot() + clamped - diff);
        entity.setYHeadRot(entity.getYRot());
    }

    /** Игрок выходит сбоку: водитель и задний левый — слева, остальные — справа. */
    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        int seat = Math.max(this.getPassengers().indexOf(passenger), 0);
        double side = (seat & 1) == 0 ? 1.6D : -1.6D;
        Vec3 target = this.position().add(new Vec3(side, 0.0D, 0.0D).yRot(-this.getYRot() * Mth.DEG_TO_RAD));
        BlockPos base = BlockPos.containing(target);
        for (Pose pose : passenger.getDismountPoses()) {
            for (int dy = 1; dy >= -1; dy--) {
                BlockPos pos = base.above(dy);
                double floor = this.level().getBlockFloorHeight(pos);
                if (!DismountHelper.isBlockFloorValid(floor)) {
                    continue;
                }
                Vec3 candidate = new Vec3(target.x, pos.getY() + floor, target.z);
                AABB box = passenger.getLocalBoundsForPose(pose).move(candidate);
                if (DismountHelper.canDismountTo(this.level(), passenger, box)) {
                    passenger.setPose(pose);
                    return candidate;
                }
            }
        }
        return super.getDismountLocationForPassenger(passenger);
    }

    // =====================================================================
    // Урон, дроп
    // =====================================================================

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) {
            return false;
        }
        if (this.level().isClientSide || this.isRemoved()) {
            return true;
        }
        this.setDamage(this.getDamage() + amount * 10.0F);
        this.markHurt();
        this.gameEvent(GameEvent.ENTITY_DAMAGE, source.getEntity());
        boolean creativePlayer = source.getEntity() instanceof Player p && p.getAbilities().instabuild;
        if (creativePlayer || this.getDamage() > DAMAGE_LIMIT) {
            dropAsItem();
        }
        return true;
    }

    /** Убирает машину из мира и выбрасывает предмет-спавн. */
    protected void dropAsItem() {
        if (this.level() instanceof ServerLevel serverLevel
                && serverLevel.getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
            this.spawnAtLocation(new ItemStack(getSpawnItem()));
        }
        this.discard();
    }

    // =====================================================================
    // Тик
    // =====================================================================

    @Override
    public void tick() {
        super.tick();
        tickLerp();

        if (this.isControlledByLocalInstance()) {
            // Клиент водителя (игрока) или сервер, когда за рулём нет игрока.
            simulate(this.getControllingPassenger());
        } else if (!this.level().isClientSide) {
            // Сервер, водитель-игрок: позицию присылает его клиент. Запоминаем фактическую скорость,
            // чтобы после выхода водителя машина плавно катилась дальше, а не вставала на месте.
            this.setDeltaMovement(this.getX() - this.xo, 0.0D, this.getZ() - this.zo);
        } else {
            this.setDeltaMovement(Vec3.ZERO);
        }

        if (this.level().isClientSide) {
            turnLocalPassengers();
            updateVisuals();
            if (!this.engineSoundActive && this.getControllingPassenger() != null) {
                this.engineSoundActive = true;
                CarClientHooks.startEngineSound(this);
            }
        } else {
            if (this.getDamage() > 0.0F) {
                this.setDamage(this.getDamage() - 1.0F);
            }
            tickRamming();
        }
    }

    // --- интерполяция позиции (как у лодки) ---

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
        this.lerpX = x;
        this.lerpY = y;
        this.lerpZ = z;
        this.lerpYRot = yRot;
        this.lerpXRot = xRot;
        this.lerpSteps = steps + 2;
    }

    private void tickLerp() {
        if (this.isControlledByLocalInstance()) {
            this.lerpSteps = 0;
            this.syncPacketPositionCodec(this.getX(), this.getY(), this.getZ());
        }
        if (this.lerpSteps > 0) {
            double d = 1.0D / this.lerpSteps;
            this.setPos(Mth.lerp(d, this.getX(), this.lerpX),
                    Mth.lerp(d, this.getY(), this.lerpY),
                    Mth.lerp(d, this.getZ(), this.lerpZ));
            this.setYRot(this.getYRot() + Mth.wrapDegrees(this.lerpYRot - this.getYRot()) * (float) d);
            this.setXRot(this.getXRot() + (this.lerpXRot - this.getXRot()) * (float) d);
            this.setRot(this.getYRot(), this.getXRot());
            --this.lerpSteps;
        }
    }

    // =====================================================================
    // Физика
    // =====================================================================

    /**
     * Один шаг физики. Вызывается на клиенте водителя (с реальным вводом) либо на сервере (водителя нет —
     * все входы нулевые, машина катится по инерции и тормозит).
     */
    private void simulate(@Nullable LivingEntity driver) {
        final CarStats s = stats();

        // --- ввод водителя: W/S = zza, A/D = xxa (A = +1), пробел = jumping (ручник) ---
        float throttle = 0.0F;
        float strafe = 0.0F;
        boolean handbrake = false;
        if (driver != null && driver.isAlive()) {
            throttle = Mth.clamp(driver.zza, -1.0F, 1.0F);
            strafe = Mth.clamp(driver.xxa, -1.0F, 1.0F);
            handbrake = driver.jumping;
        }

        // плавный руль: к цели быстрее, чем обратно в центр
        float steerTarget = -strafe;
        this.steerInput = moveToward(this.steerInput, steerTarget, steerTarget == 0.0F ? 0.25F : 0.16F);

        // --- базис корпуса (до поворота): f — вперёд, r — вправо ---
        final float yawRad = this.getYRot() * Mth.DEG_TO_RAD;
        final double fx = -Mth.sin(yawRad);
        final double fz = Mth.cos(yawRad);
        final double rx = -fz;
        final double rz = fx;

        Vec3 vel = this.getDeltaMovement();
        double vf = vel.x * fx + vel.z * fz;
        double vr = vel.x * rx + vel.z * rz;
        double vy = vel.y;

        // --- поверхность ---
        final boolean onGround = this.onGround();
        final boolean inWater = this.isInWater();
        final BlockState below = this.level().getBlockState(this.getOnPos());
        final BlockState at = this.level().getBlockState(this.blockPosition());
        final float friction = below.getBlock().getFriction();
        final boolean loose = isLooseSurface(below, at);

        double maxSpeed = s.maxSpeed() * (loose ? s.looseSurfaceFactor() : 1.0D);
        double accel = s.acceleration() * (loose ? 0.5D + 0.5D * s.looseSurfaceFactor() : 1.0D);
        if (inWater) {
            maxSpeed *= 0.35D;
            accel *= 0.5D;
        }

        // --- продольная динамика ---
        if (onGround) {
            if (throttle > 0.05F) {
                if (vf < -0.02D) {
                    vf += s.brake();                       // тормозим, пока катимся назад
                } else {
                    vf += accel * Math.max(0.0D, 1.0D - vf / maxSpeed);
                }
            } else if (throttle < -0.05F) {
                if (vf > 0.02D) {
                    vf -= s.brake();                       // торможение
                } else {
                    vf -= s.reverseAcceleration() * Math.max(0.0D, 1.0D - (-vf) / s.maxReverse());
                }
            } else {
                // накат: сопротивление качению + торможение двигателем; без водителя — стояночное трение
                vf = coast(vf, driver != null ? 0.987D : 0.93D, driver != null ? 0.003D : 0.01D);
            }
            if (handbrake) {
                vf = coast(vf, 0.97D, 0.005D);
            }
        } else {
            vf *= 0.995D;                                   // в воздухе тяги нет
        }

        // --- боковое сцепление: здесь рождается занос ---
        double grip = 0.0D;
        if (onGround) {
            grip = s.grip();
            double speedFraction = Math.abs(vf) / s.maxSpeed();
            if (handbrake) {
                grip = s.handbrakeGrip();
            } else if (s.rearWheelDrive() && throttle > 0.05F
                    && Math.abs(this.steerInput) > 0.55F && speedFraction > 0.65D) {
                grip *= 0.45D;                              // «Семёрка» срывается в занос под газом
            }
            // лёд и прочие скользкие блоки (трение 0.6 → 1.0; 0.98 → ~0.38)
            grip *= Mth.clamp(0.36D / (friction * friction), 0.2D, 1.2D);
            grip = Mth.clamp(grip, 0.02D, 0.9D);
        }
        double lost = vr * grip;
        vr -= lost;
        if (Math.abs(vf) > 0.01D) {
            vf += Math.copySign(Math.abs(lost) * 0.6D, vf); // часть погашенного скольжения переходит в тягу
        }

        // --- пределы скорости ---
        if (vf > maxSpeed) {
            vf = Math.max(maxSpeed, vf * 0.97D);
        }
        vf = Math.max(vf, -s.maxReverse());

        // --- поворот: только в движении, на высокой скорости руль «тяжелее» ---
        if (onGround) {
            double speed = Math.hypot(vf, vr);
            double ramp = Mth.clamp(speed / 0.10D, 0.0D, 1.0D);
            double highSpeed = 1.0D - 0.45D * Mth.clamp(speed / s.maxSpeed(), 0.0D, 1.0D);
            float direction = vf >= 0.0D ? 1.0F : -1.0F;     // задним ходом руль работает наоборот
            float dYaw = (float) (this.steerInput * s.turnRate() * ramp * highSpeed) * direction
                    * (handbrake ? 1.35F : 1.0F);
            this.setYRot(this.getYRot() + dYaw);
        }

        // --- вертикаль ---
        if (inWater) {
            vy = (vy - 0.02D) * 0.85D;
        } else if (!this.isNoGravity()) {
            vy = (vy - 0.08D) * 0.98D;
        }

        // Скорость собираем в СТАРОМ базисе: вектор скорости не вращается вместе с кузовом,
        // поэтому при повороте появляется боковая составляющая — это и есть снос/дрифт.
        this.setDeltaMovement(fx * vf + rx * vr, vy, fz * vf + rz * vr);
        this.move(MoverType.SELF, this.getDeltaMovement());
    }

    private static boolean isLooseSurface(BlockState below, BlockState at) {
        return below.is(BlockTags.SAND)
                || below.is(BlockTags.SNOW)
                || at.is(Blocks.SNOW)
                || below.is(Blocks.MUD)
                || below.is(Blocks.SOUL_SAND)
                || below.is(Blocks.SOUL_SOIL)
                || below.is(Blocks.FARMLAND)
                || below.is(Blocks.DIRT)
                || below.is(Blocks.COARSE_DIRT)
                || below.is(Blocks.PODZOL);
    }

    private static double coast(double v, double drag, double constant) {
        double mag = Math.max(0.0D, Math.abs(v) * drag - constant);
        return Math.copySign(mag, v);
    }

    private static float moveToward(float value, float target, float step) {
        if (value < target) {
            return Math.min(value + step, target);
        }
        return Math.max(value - step, target);
    }

    // =====================================================================
    // Урон сбитым существам (только сервер)
    // =====================================================================

    /** Фактическая горизонтальная скорость за последний тик (блоки/тик). */
    public double getHorizontalSpeed() {
        return Math.hypot(this.getX() - this.xo, this.getZ() - this.zo);
    }

    private void tickRamming() {
        double dx = this.getX() - this.xo;
        double dz = this.getZ() - this.zo;
        double speed = Math.hypot(dx, dz);
        LivingEntity driver = this.getControllingPassenger();
        if (speed < RAM_MIN_SPEED || driver == null) {
            return;
        }
        Vec3 dir = new Vec3(dx / speed, 0.0D, dz / speed);
        AABB area = this.getBoundingBox().inflate(0.35D, 0.0D, 0.35D);
        for (LivingEntity target : this.level().getEntitiesOfClass(LivingEntity.class, area,
                e -> e != driver && e.isAlive() && !this.hasPassenger(e) && !e.isPassengerOfSameVehicle(this))) {
            Vec3 toTarget = target.position().subtract(this.position());
            if (toTarget.x * dir.x + toTarget.z * dir.z <= 0.0D) {
                continue;                                    // позади — не задеваем
            }
            DamageSource source = driver instanceof Player player
                    ? this.damageSources().playerAttack(player)
                    : this.damageSources().generic();
            float damage = (float) ((speed - 0.2D) * 30.0D);
            if (target.hurt(source, damage)) {
                target.setDeltaMovement(target.getDeltaMovement().add(dir.x * speed * 1.4D, 0.35D, dir.z * speed * 1.4D));
                target.hurtMarked = true;
            }
        }
    }

    // =====================================================================
    // Клиент: визуальное состояние и камера
    // =====================================================================

    /** Пока машина поворачивает, камера локального игрока-пассажира поворачивается вместе с ней. */
    private void turnLocalPassengers() {
        float dYaw = this.getYRot() - this.yRotO;
        if (dYaw == 0.0F) {
            return;
        }
        for (Entity passenger : this.getPassengers()) {
            if (passenger instanceof Player player && player.isLocalPlayer()) {
                player.setYRot(player.getYRot() + dYaw);
                player.yRotO += dYaw;
                player.setYHeadRot(player.getYHeadRot() + dYaw);
            }
        }
    }

    /** Угол вращения колёс считается по реальному перемещению, поэтому одинаково работает у всех зрителей. */
    private void updateVisuals() {
        this.wheelRotO = this.wheelRot;
        this.steerO = this.steer;

        float yawRad = this.getYRot() * Mth.DEG_TO_RAD;
        double move = (this.getX() - this.xo) * -Mth.sin(yawRad) + (this.getZ() - this.zo) * Mth.cos(yawRad);
        this.wheelRot += (float) (move / stats().wheelRadius());

        float target;
        if (this.isControlledByLocalInstance() && this.getControllingPassenger() != null) {
            target = this.steerInput * 30.0F;
        } else {
            float yawChange = Mth.wrapDegrees(this.getYRot() - this.yRotO);
            target = Mth.clamp(yawChange * (float) Math.signum(move) * 5.0F, -30.0F, 30.0F);
        }
        this.steer = Mth.lerp(0.35F, this.steer, target);
    }

    /** Вызывается звуковым экземпляром, когда он остановился (водитель вышел). */
    public void setEngineSoundActive(boolean active) {
        this.engineSoundActive = active;
    }
}
