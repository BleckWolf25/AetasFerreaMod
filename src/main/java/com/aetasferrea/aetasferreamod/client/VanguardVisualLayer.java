/**
 * @file VanguardVisualLayer.java
 *
 * @version 1.0.0
 * @author BleckWolf25
 * @license MIT
 *
 * @summary Custom render layer for the Aetas Ferrea Nether Guardian.
 *
 * @description
 * Renders tattered, swaying dark banner fabrics hanging from the ribcage
 * and spine of the Nether Guardian. Uses vanilla gray wool texture and sways
 * dynamically based on entity age and movement.
 *
 * @since 30/06/2026
 * @updated 01/07/2026
 */
// ---------- PACKAGE
package com.aetasferrea.aetasferreamod.client;

// ---------- IMPORTS
import java.util.Objects;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.SkeletonModel;
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
import net.minecraft.world.entity.monster.AbstractSkeleton;

import javax.annotation.Nonnull;

// ---------- CLASS: VanguardVisualLayer
public class VanguardVisualLayer<T extends AbstractSkeleton, M extends SkeletonModel<T>> extends RenderLayer<T, M> {

    // ---------- FIELDS
    private final ModelPart bannerFront;
    private final ModelPart bannerBack;
    private final ModelPart leftHead;
    private final ModelPart rightHead;
    private final ModelPart leftHeadEyes;
    private final ModelPart rightHeadEyes;

    // ---------- CONSTRUCTOR
    public VanguardVisualLayer(RenderLayerParent<T, M> renderer) {
        super(renderer);

        // Banners (16x16 sheet)
        MeshDefinition bannerMesh = new MeshDefinition();
        PartDefinition bannerRoot = bannerMesh.getRoot();
        bannerRoot.addOrReplaceChild("banner_front", Objects.requireNonNull(CubeListBuilder.create().texOffs(0, 0).addBox(-1.5F, 0.0F, -0.05F, 3.0F, 10.0F, 0.1F)), Objects.requireNonNull(PartPose.offset(0.0F, 8.0F, -2.05F)));
        bannerRoot.addOrReplaceChild("banner_back", Objects.requireNonNull(CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, 0.0F, -0.05F, 4.0F, 12.0F, 0.1F)), Objects.requireNonNull(PartPose.offset(0.0F, 5.0F, 2.05F)));
        ModelPart bakedBanners = LayerDefinition.create(bannerMesh, 16, 16).bakeRoot();
        this.bannerFront = bakedBanners.getChild("banner_front");
        this.bannerBack = bakedBanners.getChild("banner_back");

        // Small Heads (64x32 texture sheet to align with skeleton head layout)
        MeshDefinition headMesh = new MeshDefinition();
        PartDefinition headRoot = headMesh.getRoot();
        headRoot.addOrReplaceChild("left_head", Objects.requireNonNull(CubeListBuilder.create().texOffs(0, 0).addBox(-2.5F, -5.0F, -2.5F, 5.0F, 5.0F, 5.0F)), Objects.requireNonNull(PartPose.offset(-5.0F, 1.0F, 0.0F)));
        headRoot.addOrReplaceChild("right_head", Objects.requireNonNull(CubeListBuilder.create().texOffs(0, 0).addBox(-2.5F, -5.0F, -2.5F, 5.0F, 5.0F, 5.0F)), Objects.requireNonNull(PartPose.offset(5.0F, 1.0F, 0.0F)));
        headRoot.addOrReplaceChild("left_head_eyes", Objects.requireNonNull(CubeListBuilder.create().texOffs(0, 0).addBox(-1.5F, -3.5F, -2.52F, 3.0F, 1.0F, 0.1F)), Objects.requireNonNull(PartPose.offset(-5.0F, 1.0F, 0.0F)));
        headRoot.addOrReplaceChild("right_head_eyes", Objects.requireNonNull(CubeListBuilder.create().texOffs(0, 0).addBox(-1.5F, -3.5F, -2.52F, 3.0F, 1.0F, 0.1F)), Objects.requireNonNull(PartPose.offset(5.0F, 1.0F, 0.0F)));
        ModelPart bakedHeads = LayerDefinition.create(headMesh, 64, 32).bakeRoot();
        this.leftHead = bakedHeads.getChild("left_head");
        this.rightHead = bakedHeads.getChild("right_head");
        this.leftHeadEyes = bakedHeads.getChild("left_head_eyes");
        this.rightHeadEyes = bakedHeads.getChild("right_head_eyes");
    }

    // ---------- RENDER
    @Override
    public void render(@Nonnull PoseStack poseStack, @Nonnull MultiBufferSource buffer,
                       int packedLight, @Nonnull T skeleton,
                       float limbSwing, float limbSwingAmount, float partialTick,
                       float ageInTicks, float netHeadYaw, float headPitch) {

        if (!ClientEvents.isNetherGuardian(skeleton) || skeleton.isInvisible()) return;

        // Render gray wool tattered fabric
        ResourceLocation fabricTexture = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/gray_wool.png");
        RenderType renderType = RenderType.entityCutoutNoCull(Objects.requireNonNull(fabricTexture));
        VertexConsumer vertexConsumer = buffer.getBuffer(Objects.requireNonNull(renderType));

        poseStack.pushPose();
        this.getParentModel().body.translateAndRotate(poseStack);

        // Wind/Idle Sway
        float sway = (float) Math.sin(ageInTicks * 0.08F) * 0.05F;
        // Movement-based Sway
        float speed = limbSwingAmount;
        float motionSway = (float) Math.sin(limbSwing * 0.4F) * 0.15F * speed;

        // Apply rotations relative to body
        this.bannerFront.xRot = sway + motionSway;
        this.bannerBack.xRot = -sway - motionSway + (speed * 0.15F); // flares out slightly behind when running

        this.bannerFront.render(poseStack, Objects.requireNonNull(vertexConsumer), packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        this.bannerBack.render(poseStack, Objects.requireNonNull(vertexConsumer), packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        poseStack.popPose();

        // Wind/Idle Sway for the small heads
        float yawSway = (float) Math.sin(ageInTicks * 0.05F) * 0.1F;

        // Sync them to head yaw and pitch, but damp it slightly so they look like sub-heads!
        this.leftHead.yRot = netHeadYaw * ((float)Math.PI / 180F) * 0.8F + yawSway - 0.2F;
        this.leftHead.xRot = headPitch * ((float)Math.PI / 180F) * 0.8F;

        this.rightHead.yRot = netHeadYaw * ((float)Math.PI / 180F) * 0.8F - yawSway + 0.2F;
        this.rightHead.xRot = headPitch * ((float)Math.PI / 180F) * 0.8F;

        // Sync eye rotations with head rotations
        this.leftHeadEyes.yRot = this.leftHead.yRot;
        this.leftHeadEyes.xRot = this.leftHead.xRot;
        this.rightHeadEyes.yRot = this.rightHead.yRot;
        this.rightHeadEyes.xRot = this.rightHead.xRot;

        // Render heads using entity's base texture
        ResourceLocation entityTexture = this.getTextureLocation(skeleton);
        RenderType entityRenderType = RenderType.entityCutoutNoCull(Objects.requireNonNull(entityTexture));
        VertexConsumer entityVertexConsumer = buffer.getBuffer(Objects.requireNonNull(entityRenderType));

        poseStack.pushPose();
        this.getParentModel().body.translateAndRotate(poseStack);
        this.leftHead.render(poseStack, Objects.requireNonNull(entityVertexConsumer), packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        this.rightHead.render(poseStack, Objects.requireNonNull(entityVertexConsumer), packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        poseStack.popPose();

        // Render Glowing Emissive Eyes on the Shoulder Heads (using redstone block texture)
        ResourceLocation eyesTexture = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/redstone_block.png");
        RenderType eyesRenderType = RenderType.eyes(Objects.requireNonNull(eyesTexture));
        VertexConsumer eyesConsumer = buffer.getBuffer(Objects.requireNonNull(eyesRenderType));

        poseStack.pushPose();
        this.getParentModel().body.translateAndRotate(poseStack);
        this.leftHeadEyes.render(poseStack, Objects.requireNonNull(eyesConsumer), packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        this.rightHeadEyes.render(poseStack, Objects.requireNonNull(eyesConsumer), packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        poseStack.popPose();
    }
}
