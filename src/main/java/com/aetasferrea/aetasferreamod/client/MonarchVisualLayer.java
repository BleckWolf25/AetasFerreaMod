/**
 * @file MonarchVisualLayer.java
 *
 * @version 1.0.0
 * @author BleckWolf25
 * @license MIT
 *
 * @summary Emissive 3D render layer for the Aetas Ferrea Hollow Monarch.
 *
 * @description
 * Implements 3D glowing lava fissures along the Monarch's ribcage, collarbones,
 * elbows, and knees, along with glowing crimson eyes. Reuses vanilla magma and
 * redstone block textures rendered using RenderType.eyes for full-bright glow.
 *
 * @since 01/07/2026
 * @updated 01/07/2026
 */
// ---------- PACKAGE
package com.aetasferrea.aetasferreamod.client;

// ---------- IMPORTS
import java.util.Objects;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.ZombieModel;
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
import net.minecraft.world.entity.monster.Husk;

import javax.annotation.Nonnull;

// ---------- CLASS: MonarchVisualLayer
public class MonarchVisualLayer<T extends Husk, M extends ZombieModel<T>> extends RenderLayer<T, M> {

    // ---------- FIELDS
    private final ModelPart bodyCracks;
    private final ModelPart leftArmCracks;
    private final ModelPart rightArmCracks;
    private final ModelPart leftLegCracks;
    private final ModelPart rightLegCracks;
    private final ModelPart headEyes;
    private final ModelPart lavaCore;

    // ---------- CONSTRUCTOR
    public MonarchVisualLayer(RenderLayerParent<T, M> renderer) {
        super(renderer);

        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        // Body Cracks (protruding through front and back of iron chestplate at Z = -3.15F and Z = 2.7F)
        CubeListBuilder bodyBuilder = CubeListBuilder.create()
                // Collarbone
                .texOffs(0, 0).addBox(-4.0F, 1.0F, -3.15F, 8.0F, 1.5F, 0.6F)
                // Left Ribs (Front & Side)
                .texOffs(0, 2).addBox(-4.8F, 3.5F, -3.15F, 4.0F, 1.2F, 0.6F)
                .texOffs(0, 4).addBox(-4.8F, 6.0F, -3.15F, 4.0F, 1.2F, 0.6F)
                .texOffs(0, 6).addBox(-4.8F, 8.5F, -3.15F, 4.0F, 1.2F, 0.6F)
                // Right Ribs (Front & Side)
                .texOffs(0, 2).addBox(0.8F, 3.5F, -3.15F, 4.0F, 1.2F, 0.6F)
                .texOffs(0, 4).addBox(0.8F, 6.0F, -3.15F, 4.0F, 1.2F, 0.6F)
                .texOffs(0, 6).addBox(0.8F, 8.5F, -3.15F, 4.0F, 1.2F, 0.6F)
                // Back Spine Cracks
                .texOffs(0, 0).addBox(-1.5F, 2.0F, 2.7F, 3.0F, 8.0F, 0.6F);
        partdefinition.addOrReplaceChild("body_cracks", Objects.requireNonNull(bodyBuilder), Objects.requireNonNull(PartPose.ZERO));

        // Internal & External Lava Core (glowing lava streams breaking through iron chestplate)
        CubeListBuilder lavaCoreBuilder = CubeListBuilder.create()
                .texOffs(0, 0).addBox(-3.8F, 2.0F, -3.12F, 7.6F, 8.0F, 6.24F);
        partdefinition.addOrReplaceChild("lava_core", Objects.requireNonNull(lavaCoreBuilder), Objects.requireNonNull(PartPose.ZERO));

        // Left Arm Elbow Crack (protruding through iron sleeve)
        CubeListBuilder leftArmBuilder = CubeListBuilder.create()
                .texOffs(0, 8).addBox(-1.5F, 3.5F, -3.12F, 5.0F, 1.5F, 6.24F);
        partdefinition.addOrReplaceChild("left_arm_cracks", Objects.requireNonNull(leftArmBuilder), Objects.requireNonNull(PartPose.ZERO));

        // Right Arm Elbow Crack (protruding through iron sleeve)
        CubeListBuilder rightArmBuilder = CubeListBuilder.create()
                .texOffs(0, 8).addBox(-3.5F, 3.5F, -3.12F, 5.0F, 1.5F, 6.24F);
        partdefinition.addOrReplaceChild("right_arm_cracks", Objects.requireNonNull(rightArmBuilder), Objects.requireNonNull(PartPose.ZERO));

        // Left Leg Knee Crack (protruding through iron leggings/boots)
        CubeListBuilder leftLegBuilder = CubeListBuilder.create()
                .texOffs(0, 8).addBox(-2.5F, 5.5F, -3.12F, 5.0F, 1.5F, 6.24F);
        partdefinition.addOrReplaceChild("left_leg_cracks", Objects.requireNonNull(leftLegBuilder), Objects.requireNonNull(PartPose.ZERO));

        // Right Leg Knee Crack (protruding through iron leggings/boots)
        CubeListBuilder rightLegBuilder = CubeListBuilder.create()
                .texOffs(0, 8).addBox(-2.5F, 5.5F, -3.12F, 5.0F, 1.5F, 6.24F);
        partdefinition.addOrReplaceChild("right_leg_cracks", Objects.requireNonNull(rightLegBuilder), Objects.requireNonNull(PartPose.ZERO));

        // Crimson Glowing Eyes
        CubeListBuilder headBuilder = CubeListBuilder.create()
                .texOffs(0, 0).addBox(-2.0F, -4.2F, -4.1F, 0.4F, 0.4F, 0.2F)
                .texOffs(0, 0).addBox(1.6F, -4.2F, -4.1F, 0.4F, 0.4F, 0.2F);
        partdefinition.addOrReplaceChild("head_eyes", Objects.requireNonNull(headBuilder), Objects.requireNonNull(PartPose.ZERO));

        LayerDefinition layerdef = LayerDefinition.create(meshdefinition, 16, 16);
        ModelPart root = layerdef.bakeRoot();
        this.bodyCracks = root.getChild("body_cracks");
        this.lavaCore = root.getChild("lava_core");
        this.leftArmCracks = root.getChild("left_arm_cracks");
        this.rightArmCracks = root.getChild("right_arm_cracks");
        this.leftLegCracks = root.getChild("left_leg_cracks");
        this.rightLegCracks = root.getChild("right_leg_cracks");
        this.headEyes = root.getChild("head_eyes");
    }

    // ---------- RENDER
    @Override
    public void render(@Nonnull PoseStack poseStack, @Nonnull MultiBufferSource buffer,
                       int packedLight, @Nonnull T husk,
                       float limbSwing, float limbSwingAmount, float partialTick,
                       float ageInTicks, float netHeadYaw, float headPitch) {

        if (!ClientEvents.isHollowMonarch(husk) || husk.isInvisible()) return;

        // Render Magma cracks (using magma block texture)
        ResourceLocation magmaTexture = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/magma.png");
        RenderType magmaRenderType = RenderType.eyes(Objects.requireNonNull(magmaTexture));
        VertexConsumer magmaConsumer = buffer.getBuffer(Objects.requireNonNull(magmaRenderType));

        // Body Cracks
        poseStack.pushPose();
        this.getParentModel().body.translateAndRotate(poseStack);
        this.bodyCracks.render(poseStack, Objects.requireNonNull(magmaConsumer), packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        poseStack.popPose();

        // Left Arm Elbow
        poseStack.pushPose();
        this.getParentModel().leftArm.translateAndRotate(poseStack);
        this.leftArmCracks.render(poseStack, Objects.requireNonNull(magmaConsumer), packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        poseStack.popPose();

        // Right Arm Elbow
        poseStack.pushPose();
        this.getParentModel().rightArm.translateAndRotate(poseStack);
        this.rightArmCracks.render(poseStack, Objects.requireNonNull(magmaConsumer), packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        poseStack.popPose();

        // Left Leg Knee
        poseStack.pushPose();
        this.getParentModel().leftLeg.translateAndRotate(poseStack);
        this.leftLegCracks.render(poseStack, Objects.requireNonNull(magmaConsumer), packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        poseStack.popPose();

        // Right Leg Knee
        poseStack.pushPose();
        this.getParentModel().rightLeg.translateAndRotate(poseStack);
        this.rightLegCracks.render(poseStack, Objects.requireNonNull(magmaConsumer), packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        poseStack.popPose();

        // Render Internal Lava Core and glowing lava cracks (using animated lava_still texture)
        ResourceLocation lavaTexture = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/lava_still.png");
        RenderType lavaRenderType = RenderType.eyes(Objects.requireNonNull(lavaTexture));
        VertexConsumer lavaConsumer = buffer.getBuffer(Objects.requireNonNull(lavaRenderType));

        // Torso Lava Core & Cracks
        poseStack.pushPose();
        this.getParentModel().body.translateAndRotate(poseStack);
        this.lavaCore.render(poseStack, Objects.requireNonNull(lavaConsumer), packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        this.bodyCracks.render(poseStack, Objects.requireNonNull(lavaConsumer), packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        poseStack.popPose();

        // Left Arm Lava
        poseStack.pushPose();
        this.getParentModel().leftArm.translateAndRotate(poseStack);
        this.leftArmCracks.render(poseStack, Objects.requireNonNull(lavaConsumer), packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        poseStack.popPose();

        // Right Arm Lava
        poseStack.pushPose();
        this.getParentModel().rightArm.translateAndRotate(poseStack);
        this.rightArmCracks.render(poseStack, Objects.requireNonNull(lavaConsumer), packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        poseStack.popPose();

        // Left Leg Lava
        poseStack.pushPose();
        this.getParentModel().leftLeg.translateAndRotate(poseStack);
        this.leftLegCracks.render(poseStack, Objects.requireNonNull(lavaConsumer), packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        poseStack.popPose();

        // Right Leg Lava
        poseStack.pushPose();
        this.getParentModel().rightLeg.translateAndRotate(poseStack);
        this.rightLegCracks.render(poseStack, Objects.requireNonNull(lavaConsumer), packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        poseStack.popPose();

        // Render Glowing Red Eyes (using redstone block texture)
        ResourceLocation redstoneTexture = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/redstone_block.png");
        RenderType eyesRenderType = RenderType.eyes(Objects.requireNonNull(redstoneTexture));
        VertexConsumer eyesConsumer = buffer.getBuffer(Objects.requireNonNull(eyesRenderType));

        poseStack.pushPose();
        this.getParentModel().head.translateAndRotate(poseStack);
        this.headEyes.render(poseStack, Objects.requireNonNull(eyesConsumer), packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        poseStack.popPose();
    }
}
