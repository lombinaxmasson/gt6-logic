package com.lombinaxmasson.gt6logic.client;

import org.joml.Vector3f;

import com.lombinaxmasson.gt6logic.Gt6Logic;
import com.lombinaxmasson.gt6logic.block.SelectorSwitchBlock;
import com.lombinaxmasson.gt6logic.block.SelectorSwitchBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

/**
 * Six lamps on the plate. Each lamp is tied to a direction in the north-facing model,
 * then turned with the plate, so the lit one is the face the switch is listening to.
 */
public final class SelectorSwitchRenderer implements BlockEntityRenderer<SelectorSwitchBlockEntity> {
    private static final ResourceLocation PIP =
            ResourceLocation.fromNamespaceAndPath(Gt6Logic.MODID, "textures/block/pip.png");
    private static final float Z = -0.5F + 2.4F / 16F;
    private static final float HALF = 0.055F;

    /** Model-pixel centers on the north-facing plate. The direction is the one that lamp means before turning. */
    private static final float[][] LAMPS = {
            {8f, 8f},
            {8f, 13f},
            {8f, 5f},
            {3f, 8f},
            {13f, 8f},
            {8f, 2f}
    };
    private static final Direction[] MEANS = {
            Direction.NORTH,
            Direction.UP,
            Direction.DOWN,
            Direction.WEST,
            Direction.EAST,
            Direction.SOUTH
    };

    public SelectorSwitchRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(
            SelectorSwitchBlockEntity entity,
            float partialTick,
            PoseStack pose,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay) {
        Direction facing = entity.getBlockState().getValue(SelectorSwitchBlock.FACING);
        boolean passing = entity.getBlockState().getValue(SelectorSwitchBlock.POWERED);
        int color = passing ? 0xFFFFF1C4 : 0xFFE0A050;
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        pose.mulPose(PlateTurn.of(facing));
        for (int i = 0; i < LAMPS.length; i++) {
            if (worldDirection(facing, MEANS[i]) != entity.input()) {
                continue;
            }
            float x = LAMPS[i][0] / 16F - 0.5F;
            float y = LAMPS[i][1] / 16F - 0.5F;
            FaceMark.quad(pose, buffer, PIP, x - HALF, y - HALF, x + HALF, y + HALF, Z, color);
        }
        pose.popPose();
    }

    private static Direction worldDirection(Direction facing, Direction modelDirection) {
        Vector3f step = new Vector3f(modelDirection.getStepX(), modelDirection.getStepY(), modelDirection.getStepZ());
        step.rotate(PlateTurn.of(facing));
        return Direction.getNearest(step.x, step.y, step.z);
    }
}
