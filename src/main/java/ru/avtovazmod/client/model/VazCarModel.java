package ru.avtovazmod.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import ru.avtovazmod.AvtovazMod;
import ru.avtovazmod.entity.AbstractVazCarEntity;

/**
 * Общая модель легковушки. Две раскладки (седан и «Нива») строятся одним методом по параметрам {@link Shape}.
 *
 * <p>Система координат модели — ванильная: 16 px = 1 блок, ось Y направлена вниз, земля на y = 24,
 * капот смотрит в -Z, +X — левая сторона машины. Текстура 256x256 разбита на горизонтальные полосы цвета
 * (кузов / стекло / тёмное / серое / фара / стоп-сигнал), поэтому раскраска сводится к заливке полос.
 *
 * <p>Стекло вынесено в отдельную часть {@code glass}: оно рисуется вторым проходом полупрозрачным слоем,
 * а в кузове нет сплошных стенок — из салона от первого лица всё видно через оконные проёмы.
 */
public class VazCarModel<T extends AbstractVazCarEntity> extends EntityModel<T> {
    public static final ModelLayerLocation SEDAN_LAYER = new ModelLayerLocation(AvtovazMod.id("vaz2107"), "main");
    public static final ModelLayerLocation NIVA_LAYER = new ModelLayerLocation(AvtovazMod.id("vaz_niva"), "main");

    private static final int TEX_SIZE = 256;
    private static final int V_BODY = 0;
    private static final int V_GLASS = 80;
    private static final int V_DARK = 140;
    private static final int V_GRAY = 190;
    private static final int V_LIGHT = 210;
    private static final int V_TAIL = 230;

    private final ModelPart root;
    private final ModelPart solid;
    private final ModelPart glass;
    private final ModelPart steering;
    private final ModelPart wheelFL;
    private final ModelPart wheelFR;
    private final ModelPart wheelRL;
    private final ModelPart wheelRR;

    public VazCarModel(ModelPart root) {
        this.root = root;
        this.solid = root.getChild("solid");
        this.glass = root.getChild("glass");
        this.steering = this.solid.getChild("steering");
        this.wheelFL = this.solid.getChild("wheel_fl");
        this.wheelFR = this.solid.getChild("wheel_fr");
        this.wheelRL = this.solid.getChild("wheel_rl");
        this.wheelRR = this.solid.getChild("wheel_rr");
    }

    /** Геометрия кузова в пикселях. */
    private record Shape(int halfW, int bodyTop, int bodyBottom, int halfL,
                         int cabinTop, int cabinZ0, int cabinLen,
                         int wheelR, int axleFront, int axleRear, float wheelX) {
    }

    public static LayerDefinition createSedanLayer() {
        // низкий седан: капот и багажник, трёхобъёмный кузов
        return create(new Shape(12, 13, 5, 22, 29, -8, 24, 5, 14, 14, 11.5F));
    }

    public static LayerDefinition createNivaLayer() {
        // высокий клиренс, большие колёса, длинная «коробка» салона
        return create(new Shape(12, 17, 7, 22, 30, -9, 26, 6, 14, 14, 11.5F));
    }

    private static LayerDefinition create(Shape s) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        final int hw = s.halfW();
        final int cabinHalf = hw - 1;
        final int pillarH = (s.cabinTop() - 2) - s.bodyTop();
        final int frontBackZ = s.cabinZ0() + 14;                    // спинки передних кресел
        final int rearBackZ = s.cabinZ0() + s.cabinLen() - 4;       // спинка заднего дивана
        final int cabinEnd = s.cabinZ0() + s.cabinLen();

        CubeListBuilder body = CubeListBuilder.create();
        // нижняя часть кузова
        body.texOffs(0, V_BODY)
                .addBox(-hw, -s.bodyTop(), -s.halfL(), 2 * hw, s.bodyTop() - s.bodyBottom(), 2 * s.halfL());
        // крыша
        body.addBox(-cabinHalf, -s.cabinTop(), s.cabinZ0(), 2 * cabinHalf, 2, s.cabinLen());
        // стойки: A (перед), B (середина), C (зад) с обеих сторон
        int[] pillarZ = {s.cabinZ0(), s.cabinZ0() + s.cabinLen() / 2 - 1, cabinEnd - 2};
        for (int z : pillarZ) {
            body.addBox(-cabinHalf, -(s.cabinTop() - 2), z, 2, pillarH, 2);
            body.addBox(cabinHalf - 2, -(s.cabinTop() - 2), z, 2, pillarH, 2);
        }
        // фары и стоп-сигналы
        body.texOffs(0, V_LIGHT)
                .addBox(-cabinHalf, -(s.bodyTop() - 1), -s.halfL() - 0.6F, 5, 3, 0.6F)
                .addBox(cabinHalf - 5, -(s.bodyTop() - 1), -s.halfL() - 0.6F, 5, 3, 0.6F);
        body.texOffs(0, V_TAIL)
                .addBox(-cabinHalf, -(s.bodyTop() - 1), s.halfL(), 5, 3, 0.6F)
                .addBox(cabinHalf - 5, -(s.bodyTop() - 1), s.halfL(), 5, 3, 0.6F);
        // бамперы и кресла (тёмная полоса)
        body.texOffs(0, V_DARK)
                .addBox(-hw - 0.5F, -(s.bodyBottom() + 4), -s.halfL() - 1.5F, 2 * hw + 1, 3, 1.5F)
                .addBox(-hw - 0.5F, -(s.bodyBottom() + 4), s.halfL(), 2 * hw + 1, 3, 1.5F)
                .addBox(2.5F, -(s.bodyTop() + 8), frontBackZ, 7, 8, 2)
                .addBox(-9.5F, -(s.bodyTop() + 8), frontBackZ, 7, 8, 2)
                .addBox(-9.5F, -(s.bodyTop() + 8), rearBackZ, 19, 8, 2);

        PartDefinition solid = root.addOrReplaceChild("solid", body, PartPose.offset(0.0F, 24.0F, 0.0F));

        // руль водителя (слева, +X), вращается вокруг Z
        solid.addOrReplaceChild("steering",
                CubeListBuilder.create().texOffs(0, V_DARK).addBox(-3.0F, -3.0F, -0.5F, 6, 6, 1),
                PartPose.offset(6.0F, -(s.bodyTop() + 5), frontBackZ - 9.0F));

        // колёса: центр на высоте радиуса, вращение вокруг X, передние ещё и поворачиваются вокруг Y
        solid.addOrReplaceChild("wheel_fl", wheel(s.wheelR(), +1), PartPose.offset(s.wheelX(), -s.wheelR(), -s.axleFront()));
        solid.addOrReplaceChild("wheel_fr", wheel(s.wheelR(), -1), PartPose.offset(-s.wheelX(), -s.wheelR(), -s.axleFront()));
        solid.addOrReplaceChild("wheel_rl", wheel(s.wheelR(), +1), PartPose.offset(s.wheelX(), -s.wheelR(), s.axleRear()));
        solid.addOrReplaceChild("wheel_rr", wheel(s.wheelR(), -1), PartPose.offset(-s.wheelX(), -s.wheelR(), s.axleRear()));

        // стёкла: лобовое, заднее и боковые — тонкие панели в проёмах между стойками
        CubeListBuilder glass = CubeListBuilder.create().texOffs(0, V_GLASS)
                .addBox(-(cabinHalf - 2), -(s.cabinTop() - 2), s.cabinZ0() + 0.5F, 2 * (cabinHalf - 2), pillarH, 0.5F)
                .addBox(-(cabinHalf - 2), -(s.cabinTop() - 2), cabinEnd - 1.0F, 2 * (cabinHalf - 2), pillarH, 0.5F)
                .addBox(-cabinHalf + 0.75F, -(s.cabinTop() - 2), s.cabinZ0() + 2, 0.5F, pillarH, s.cabinLen() - 4)
                .addBox(cabinHalf - 1.25F, -(s.cabinTop() - 2), s.cabinZ0() + 2, 0.5F, pillarH, s.cabinLen() - 4);
        root.addOrReplaceChild("glass", glass, PartPose.offset(0.0F, 24.0F, 0.0F));

        return LayerDefinition.create(mesh, TEX_SIZE, TEX_SIZE);
    }

    /** Покрышка + крестовина колпака на внешней стороне (чтобы вращение было видно). side: +1 = левая (+X). */
    private static CubeListBuilder wheel(int r, int side) {
        float outer = side > 0 ? 1.5F : -2.1F;
        float bar = r - 1.5F;
        return CubeListBuilder.create()
                .texOffs(0, V_DARK).addBox(-1.5F, -r, -r, 3, 2 * r, 2 * r)
                .texOffs(0, V_GRAY)
                .addBox(outer, -bar, -0.75F, 0.6F, 2 * bar, 1.5F)
                .addBox(outer, -0.75F, -bar, 0.6F, 1.5F, 2 * bar);
    }

    /** Все анимируемые углы приходят из рендерера (с учётом partialTick), поэтому setupAnim пустой. */
    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
    }

    /**
     * @param wheelRoll угол качения колёс, радианы (+ = едем вперёд)
     * @param steerDeg  поворот передних колёс, градусы (+ = направо)
     */
    public void prepare(float wheelRoll, float steerDeg) {
        float steerRad = steerDeg * Mth.DEG_TO_RAD;
        this.wheelFL.xRot = wheelRoll;
        this.wheelFR.xRot = wheelRoll;
        this.wheelRL.xRot = wheelRoll;
        this.wheelRR.xRot = wheelRoll;
        this.wheelFL.yRot = steerRad;
        this.wheelFR.yRot = steerRad;
        this.steering.zRot = -steerRad * 4.0F;   // руль крутится сильнее колёс
    }

    /** Непрозрачные части (кузов, колёса, руль, сиденья). */
    public void renderSolid(PoseStack poseStack, VertexConsumer consumer, int light, int overlay, int color) {
        this.solid.visible = true;
        this.glass.visible = false;
        this.root.render(poseStack, consumer, light, overlay, color);
        this.glass.visible = true;
    }

    /** Полупрозрачное стекло — рисуется отдельным проходом с RenderType.entityTranslucent. */
    public void renderGlass(PoseStack poseStack, VertexConsumer consumer, int light, int overlay, int color) {
        this.solid.visible = false;
        this.glass.visible = true;
        this.root.render(poseStack, consumer, light, overlay, color);
        this.solid.visible = true;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer consumer, int light, int overlay, int color) {
        this.root.render(poseStack, consumer, light, overlay, color);
    }
}
