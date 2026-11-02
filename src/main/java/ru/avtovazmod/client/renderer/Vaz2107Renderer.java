package ru.avtovazmod.client.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import ru.avtovazmod.AvtovazMod;
import ru.avtovazmod.client.model.VazCarModel;
import ru.avtovazmod.entity.Vaz2107Entity;

public class Vaz2107Renderer extends VazCarRenderer<Vaz2107Entity> {
    private static final ResourceLocation TEXTURE = AvtovazMod.id("textures/entity/vaz2107.png");

    public Vaz2107Renderer(EntityRendererProvider.Context context) {
        super(context, VazCarModel.SEDAN_LAYER, TEXTURE);
    }
}
