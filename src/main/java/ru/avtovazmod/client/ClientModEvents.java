package ru.avtovazmod.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import ru.avtovazmod.AvtovazMod;
import ru.avtovazmod.client.model.VazCarModel;
import ru.avtovazmod.client.renderer.Vaz2107Renderer;
import ru.avtovazmod.client.renderer.VazNivaRenderer;
import ru.avtovazmod.registry.ModEntities;

/** Клиентские события на MOD-шине: слои моделей и рендереры. */
@EventBusSubscriber(modid = AvtovazMod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(VazCarModel.SEDAN_LAYER, VazCarModel::createSedanLayer);
        event.registerLayerDefinition(VazCarModel.NIVA_LAYER, VazCarModel::createNivaLayer);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.VAZ_2107.get(), Vaz2107Renderer::new);
        event.registerEntityRenderer(ModEntities.VAZ_NIVA.get(), VazNivaRenderer::new);
    }

    private ClientModEvents() {
    }
}
