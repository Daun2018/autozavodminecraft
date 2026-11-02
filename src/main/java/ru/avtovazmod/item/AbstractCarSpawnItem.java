package ru.avtovazmod.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.shapes.VoxelShape;
import ru.avtovazmod.entity.AbstractVazCarEntity;

/** Предмет, который по ПКМ по блоку ставит машину; капот смотрит туда же, куда смотрит игрок. */
public abstract class AbstractCarSpawnItem extends Item {

    protected AbstractCarSpawnItem(Properties properties) {
        super(properties);
    }

    protected abstract EntityType<? extends AbstractVazCarEntity> carType();

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }

        BlockPos clicked = context.getClickedPos();
        BlockState state = level.getBlockState(clicked);
        boolean replaceable = state.getCollisionShape(level, clicked).isEmpty();
        BlockPos placePos = replaceable ? clicked : clicked.relative(context.getClickedFace());

        double y = placePos.getY();
        if (!replaceable && context.getClickedFace() == Direction.UP) {
            VoxelShape shape = state.getCollisionShape(level, clicked);
            y = clicked.getY() + shape.max(Direction.Axis.Y);   // корректно для плит, снега и т.п.
        }

        AbstractVazCarEntity car = carType().create(serverLevel);
        if (car == null) {
            return InteractionResult.FAIL;
        }
        Player player = context.getPlayer();
        float yaw = player != null ? player.getYRot() : 0.0F;
        car.moveTo(placePos.getX() + 0.5D, y, placePos.getZ() + 0.5D, yaw, 0.0F);

        if (!serverLevel.noCollision(car)) {
            if (player != null) {
                player.displayClientMessage(Component.translatable("message.avtovazmod.no_space"), true);
            }
            return InteractionResult.FAIL;
        }

        serverLevel.addFreshEntity(car);
        serverLevel.playSound(null, placePos, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.PLAYERS, 1.0F, 0.7F);
        serverLevel.gameEvent(player, GameEvent.ENTITY_PLACE, placePos);
        context.getItemInHand().consume(1, player);
        return InteractionResult.CONSUME;
    }
}
