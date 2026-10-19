package com.lombinaxmasson.gt6logic.block;

import com.gregtech.gregtech.api.signal.GtSignalCarrier;
import com.lombinaxmasson.gt6logic.control.Formula;
import com.lombinaxmasson.gt6logic.control.Formula.Value;
import com.lombinaxmasson.gt6logic.signal.SignalBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

/** Front is {@code a}, back is {@code b}. The line is one of the built-in formulas. */
public final class FormulaModuleBlockEntity extends SignalBlockEntity {
    private static final String LINE = "Formula";

    private String formula = Formula.PRESETS[0];

    public FormulaModuleBlockEntity(BlockPos pos, BlockState state) {
        super(FormulaModuleBlock.TYPE.get(), pos, state);
    }

    public String formula() {
        return formula;
    }

    public Component status() {
        Value value = Formula.evaluate(formula, 0L, 0L);
        String key = value.ok() ? "message.gt6logic.formula.line" : "message.gt6logic.formula.bad";
        return Component.translatable(key, formula);
    }

    public void adjust(Player player) {
        formula = Formula.PRESETS[Formula.nextPreset(formula)];
        setChanged();
    }

    /** A test, or a later screen, can store a line the right-click list does not contain. */
    public void setFormula(String line) {
        formula = line == null || line.length() > Formula.MAX_LENGTH ? Formula.PRESETS[0] : line;
        setChanged();
    }

    @Override
    protected void onTick() {
        BlockState state = getBlockState();
        if (!state.hasProperty(LogicPlateBlock.FACING)) {
            return;
        }
        Direction facing = state.getValue(LogicPlateBlock.FACING);
        Value value = Formula.evaluate(formula, read(facing), read(facing.getOpposite()));
        long signal = value.ok() ? GtSignalCarrier.clamp(value.signal()) : 0L;
        PlateOutput.value(this, signal, facing, facing.getOpposite());
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString(LINE, formula);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        String saved = tag.getString(LINE);
        formula = saved.isEmpty() || saved.length() > Formula.MAX_LENGTH ? Formula.PRESETS[0] : saved;
    }
}
