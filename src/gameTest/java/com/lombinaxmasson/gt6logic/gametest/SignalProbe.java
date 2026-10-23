package com.lombinaxmasson.gt6logic.gametest;

import com.gregtech.gregtech.api.signal.GtSignalCarrier;
import com.gregtech.gregtech.api.signal.GtSignalReceiver;
import com.lombinaxmasson.gt6logic.Gt6Logic;
import com.lombinaxmasson.gt6logic.signal.SignalBlock;
import com.lombinaxmasson.gt6logic.signal.SignalBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;

/** GameTest-only source. It has no item and is not in the published jar. */
@EventBusSubscriber(modid = Gt6Logic.MODID)
public final class SignalProbe {
    public static Block BLOCK;
    public static BlockEntityType<?> TYPE;

    private SignalProbe() {}

    @SubscribeEvent
    public static void register(RegisterEvent event) {
        event.register(Registries.BLOCK, helper -> {
            if (BLOCK != null) {
                return;
            }
            BLOCK = new ProbeBlock();
            helper.register(ResourceLocation.fromNamespaceAndPath(Gt6Logic.MODID, "signal_probe"), BLOCK);
        });
        event.register(Registries.BLOCK_ENTITY_TYPE, helper -> {
            if (TYPE != null) {
                return;
            }
            if (BLOCK == null) {
                throw new IllegalStateException("signal probe block is missing");
            }
            TYPE = BlockEntityType.Builder.of(Entity::new, BLOCK).build(null);
            helper.register(ResourceLocation.fromNamespaceAndPath(Gt6Logic.MODID, "signal_probe"), TYPE);
        });
    }

    public static final class ProbeBlock extends SignalBlock {
        ProbeBlock() {
            super(Properties.of());
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new Entity(pos, state);
        }
    }

    public static final class Entity extends SignalBlockEntity {
        private boolean echoInputs;

        Entity(BlockPos pos, BlockState state) {
            super(TYPE, pos, state);
        }

        public void setPending(Direction side, long value) {
            output().set(side.ordinal(), value);
        }

        public void echoInputs(boolean echo) {
            this.echoInputs = echo;
        }

        @Override
        protected void onTick() {
            if (!echoInputs) {
                return;
            }
            for (Direction face : Direction.values()) {
                output().set(face.ordinal(), read(face));
            }
        }
    }

    public static Block redAlloyWire() {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("gregtech", "wire_01_redalloy");
        if (!BuiltInRegistries.BLOCK.containsKey(id)) {
            throw new IllegalStateException("missing " + id);
        }
        return BuiltInRegistries.BLOCK.get(id);
    }

    public static BlockState connect(BlockState state, Direction side) {
        for (Property<?> property : state.getProperties()) {
            if (property instanceof BooleanProperty flag && property.getName().equals(side.getSerializedName())) {
                return state.setValue(flag, true);
            }
        }
        throw new IllegalStateException("no " + side.getSerializedName() + " port on " + state);
    }

    public static boolean connected(BlockState state, Direction side) {
        for (Property<?> property : state.getProperties()) {
            if (property instanceof BooleanProperty flag && property.getName().equals(side.getSerializedName())) {
                return state.getValue(flag);
            }
        }
        throw new IllegalStateException("no " + side.getSerializedName() + " port on " + state);
    }

    public static boolean isReceiver(BlockEntity entity) {
        return entity instanceof GtSignalReceiver;
    }
}
