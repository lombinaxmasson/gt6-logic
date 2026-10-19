package com.lombinaxmasson.gt6logic;

import org.slf4j.Logger;

import com.gregtech.gregtech.api.addon.GregTechAddon;
import com.gregtech.gregtech.api.addon.GregTechAddons;
import com.lombinaxmasson.gt6logic.block.AlarmBellBlock;
import com.lombinaxmasson.gt6logic.block.CompensationRepeaterBlock;
import com.lombinaxmasson.gt6logic.block.CounterBlock;
import com.lombinaxmasson.gt6logic.block.DelayRelayBlock;
import com.lombinaxmasson.gt6logic.block.IndicatorBlock;
import com.lombinaxmasson.gt6logic.block.InterlockBlock;
import com.lombinaxmasson.gt6logic.block.SelectorSwitchBlock;
import com.lombinaxmasson.gt6logic.block.SignalMeterBlock;
import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * GT6 Logic: friendly control blocks and process machines for GT6CE.
 *
 * <p>The addon entry point is registered in the constructor because GT6 closes
 * addon registration before common setup begins.</p>
 */
@Mod(Gt6Logic.MODID)
public final class Gt6Logic {
    public static final String MODID = "gt6logic";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CREATIVE_TAB =
            CREATIVE_MODE_TABS.register("main", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.gt6logic"))
                    .withTabsBefore(CreativeModeTabs.REDSTONE_BLOCKS)
                    .icon(() -> new ItemStack(IndicatorBlock.ITEM.get()))
                    .displayItems((parameters, output) -> ITEMS.getEntries()
                            .forEach(item -> output.accept(item.get())))
                    .build());

    public Gt6Logic(IEventBus modEventBus, ModContainer modContainer) {
        IndicatorBlock.bootstrap();
        AlarmBellBlock.bootstrap();
        DelayRelayBlock.bootstrap();
        CounterBlock.bootstrap();
        InterlockBlock.bootstrap();
        SignalMeterBlock.bootstrap();
        CompensationRepeaterBlock.bootstrap();
        SelectorSwitchBlock.bootstrap();
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);

        GregTechAddons.register(new GregTechAddon() {
            @Override
            public String id() {
                return MODID;
            }

            @Override
            public void onRecipesReady(Context context) {
                LOGGER.info("gt6logic: ready on {} {} (GT6CE addon API {})",
                        context.platform(), context.minecraftVersion(), context.apiVersion());
            }
        });
    }
}
