package com.lombinaxmasson.gt6logic.gametest;

import com.lombinaxmasson.gt6logic.Gt6Logic;
import com.lombinaxmasson.gt6logic.block.CompensationRepeaterBlock;
import com.lombinaxmasson.gt6logic.block.CounterBlock;
import com.lombinaxmasson.gt6logic.block.DelayRelayBlock;
import com.lombinaxmasson.gt6logic.block.IndicatorBlock;
import com.lombinaxmasson.gt6logic.block.InterlockBlock;
import com.lombinaxmasson.gt6logic.block.SelectorSwitchBlock;
import com.lombinaxmasson.gt6logic.block.SignalMeterBlock;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** The six second-tier blocks are in the creative tab, under the same indicator icon. */
@GameTestHolder("gt6logic")
@PrefixGameTestTemplate(false)
public final class SecondTierTabTests {
    private SecondTierTabTests() {}

    @GameTest(template = "test_empty", timeoutTicks = 20)
    public static void creativeTabListsTheSixBlocks(GameTestHelper helper) {
        CreativeModeTab tab = Gt6Logic.CREATIVE_TAB.get();
        tab.buildContents(new CreativeModeTab.ItemDisplayParameters(
                FeatureFlags.VANILLA_SET, true, helper.getLevel().registryAccess()));
        helper.assertTrue(tab.getIconItem().is(IndicatorBlock.ITEM.get()), "creative tab icon stays the indicator");
        assertListed(helper, tab, DelayRelayBlock.BLOCK.get(), "block.gt6logic.delay_relay", "Delay Relay", "延时继电器");
        assertListed(helper, tab, CounterBlock.BLOCK.get(), "block.gt6logic.counter", "Counter", "计数器");
        assertListed(helper, tab, InterlockBlock.BLOCK.get(), "block.gt6logic.interlock", "Interlock", "联锁开关");
        assertListed(helper, tab, SignalMeterBlock.BLOCK.get(), "block.gt6logic.signal_meter", "Signal Meter", "信号表");
        assertListed(helper, tab, CompensationRepeaterBlock.BLOCK.get(),
                "block.gt6logic.compensation_repeater", "Compensation Repeater", "补偿中继器");
        assertListed(helper, tab, SelectorSwitchBlock.BLOCK.get(), "block.gt6logic.selector_switch", "Selector Switch", "选择开关");
        helper.succeed();
    }

    private static void assertListed(
            GameTestHelper helper, CreativeModeTab tab, Block block, String key, String english, String chinese) {
        boolean listed = false;
        for (ItemStack stack : tab.getDisplayItems()) {
            if (stack.is(block.asItem())) {
                listed = true;
            }
        }
        helper.assertTrue(listed, key + " is in the creative tab");
        helper.assertTrue(english.equals(GameTestSupport.translation("en_us.json", key)), "english " + key);
        helper.assertTrue(chinese.equals(GameTestSupport.translation("zh_cn.json", key)), "chinese " + key);
    }
}
