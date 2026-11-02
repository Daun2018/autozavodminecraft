package ru.avtovazmod.client.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import ru.avtovazmod.AvtovazMod;
import ru.avtovazmod.client.model.VazCarModel;
import ru.avtovazmod.entity.VazNivaEntity;

public class VazNivaRenderer extends VazCarRenderer<VazNivaEntity> {
    private static final ResourceLocation TEXTURE = AvtovazMod.id("textures/entity/vaz_niva.png");

    public VazNivaRenderer(EntityRendererProvider.Context context) {
        super(context, VazCarModel.NIVA_LAYER, TEXTURE);
    }
}
