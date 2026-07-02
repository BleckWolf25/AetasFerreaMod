/**
 * @file CustomChestLayer.java
 *
 * @version 1.0.0
 * @author BleckWolf25
 * @license MIT
 *
 * @summary Render layer that draws saddlebag chest models on the Aetas Horse when equipped.
 *
 * @description
 * Extends RenderLayer to bake and render a pair of symmetrical chest models (left and right)
 * using the vanilla donkey skin atlas, aligned to the horse body pivot in model space.
 * Only renders when the underlying entity is a HorseEventHandler that has its chest flag set
 * and is not invisible.
 *
 * @since 20/05/2026
 * @updated 24/06/2026
 */
// ---------- PACKAGE
package com.aetasferrea.aetasferreamod.client;

// ---------- IMPORTS
import java.util.Objects;
import javax.annotation.Nonnull;

import com.aetasferrea.aetasferreamod.entity.HorseEventHandler;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HorseModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.horse.Horse;

// ---------- CLASS: CustomChestLayer
public class CustomChestLayer extends RenderLayer<Horse, HorseModel<Horse>> {

    // ---------- CONSTANTS
    // Reuses the vanilla donkey skin so the chest cubes match the donkey saddlebag UV layout
    @Nonnull
    private static final ResourceLocation DONKEY_TEXTURE =
            Objects.requireNonNull(ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/horse/donkey.png"));

    // ---------- FIELDS
    private final ModelPart leftChest;
    private final ModelPart rightChest;

    // ---------- CONSTRUCTOR
    public CustomChestLayer(RenderLayerParent<Horse, HorseModel<Horse>> renderer) {
        super(renderer);

        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        // Left chest: rotated 90° around Y so it faces outward from the horse's left flank
        partdefinition.addOrReplaceChild("left_chest",
                Objects.requireNonNull(CubeListBuilder.create().texOffs(26, 21).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 8.0F, 3.0F)),
                Objects.requireNonNull(PartPose.offsetAndRotation(-6.0F, -8.0F, 0.0F, 0.0F, -1.5707964F, 0.0F)));

        // Right chest: mirror of left, rotated opposite direction
        partdefinition.addOrReplaceChild("right_chest",
                Objects.requireNonNull(CubeListBuilder.create().texOffs(26, 21).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 8.0F, 3.0F)),
                Objects.requireNonNull(PartPose.offsetAndRotation(6.0F, -8.0F, 0.0F, 0.0F, 1.5707964F, 0.0F)));

        LayerDefinition layerdef = LayerDefinition.create(meshdefinition, 64, 64);
        ModelPart root = layerdef.bakeRoot();
        this.leftChest = root.getChild("left_chest");
        this.rightChest = root.getChild("right_chest");
    }

    // ---------- RENDER
    @Override
    public void render(@Nonnull PoseStack poseStack, @Nonnull MultiBufferSource buffer,
                       int packedLight, @Nonnull Horse horse,
                       float limbSwing, float limbSwingAmount, float partialTick,
                       float ageInTicks, float netHeadYaw, float headPitch) {

        if (!(horse instanceof HorseEventHandler customHorse) || !customHorse.hasChest() || customHorse.isInvisible()) return;

        @Nonnull RenderType renderType = Objects.requireNonNull(RenderType.entityCutoutNoCull(DONKEY_TEXTURE));
        @Nonnull VertexConsumer vertexconsumer = Objects.requireNonNull(buffer.getBuffer(renderType));

        poseStack.pushPose();
        // Translate to the vanilla Horse body pivot: (0, 11, 9) model units = (0, 0.6875, 0.3) blocks
        poseStack.translate(0.0F, 0.6875F, 0.3000F);

        this.leftChest.render(poseStack, vertexconsumer, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        this.rightChest.render(poseStack, vertexconsumer, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        poseStack.popPose();
    }
}
