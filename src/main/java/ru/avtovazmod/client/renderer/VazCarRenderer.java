package ru.avtovazmod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import ru.avtovazmod.client.model.VazCarModel;
import ru.avtovazmod.entity.AbstractVazCarEntity;

/** Общий рендерер машин: два прохода (кузов, затем полупрозрачное стекло). */
public class VazCarRenderer<T extends AbstractVazCarEntity> extends EntityRenderer<T> {
    private final VazCarModel<T> model;
    private final ResourceLocation texture;

    public VazCarRenderer(EntityRendererProvider.Context context, ModelLayerLocation layer, ResourceLocation texture) {
        super(context);
        this.model = new VazCarModel<>(context.bakeLayer(layer));
        this.texture = texture;
        this.shadowRadius = 1.0F;
    }

    @Override
    public void render(T car, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        // та же цепочка трансформаций, что у ванильных мобов: капот модели (-Z) смотрит по курсу сущности
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - entityYaw));
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.translate(0.0F, -1.501F, 0.0F);

        this.model.prepare(Mth.lerp(partialTick, car.wheelRotO, car.wheelRot),
                Mth.lerp(partialTick, car.steerO, car.steer));

        VertexConsumer solid = buffer.getBuffer(this.model.renderType(this.texture));
        this.model.renderSolid(poseStack, solid, packedLight, OverlayTexture.NO_OVERLAY, -1);

        VertexConsumer glass = buffer.getBuffer(RenderType.entityTranslucent(this.texture));
        this.model.renderGlass(poseStack, glass, packedLight, OverlayTexture.NO_OVERLAY, -1);

        poseStack.popPose();
        super.render(car, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(T car) {
        return this.texture;
    }
}
