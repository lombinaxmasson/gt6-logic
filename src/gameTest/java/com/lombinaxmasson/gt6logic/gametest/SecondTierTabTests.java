package com.lombinaxmasson.gt6logic.gametest;

import java.util.List;

import com.lombinaxmasson.gt6logic.Gt6Logic;
import com.lombinaxmasson.gt6logic.block.AdjustableCoolerBlock;
import com.lombinaxmasson.gt6logic.block.AdjustableHeaterBlock;
import com.lombinaxmasson.gt6logic.block.AdjustablePumpBlock;
import com.lombinaxmasson.gt6logic.block.ArithmeticBlock;
import com.lombinaxmasson.gt6logic.block.BatteryStewardBlock;
import com.lombinaxmasson.gt6logic.block.ChartRecorderBlock;
import com.lombinaxmasson.gt6logic.block.CompensationRepeaterBlock;
import com.lombinaxmasson.gt6logic.block.ControlValveBlock;
import com.lombinaxmasson.gt6logic.block.DigitDisplayBlock;
import com.lombinaxmasson.gt6logic.block.ElectricMeterBlock;
import com.lombinaxmasson.gt6logic.block.ElectricValveBlock;
import com.lombinaxmasson.gt6logic.block.FloatSwitchBlock;
import com.lombinaxmasson.gt6logic.block.FormulaModuleBlock;
import com.lombinaxmasson.gt6logic.block.PointerGaugeBlock;
import com.lombinaxmasson.gt6logic.block.ProgramThermostatBlock;
import com.lombinaxmasson.gt6logic.block.ProportionalControllerBlock;
import com.lombinaxmasson.gt6logic.block.RatioControllerBlock;
import com.lombinaxmasson.gt6logic.block.SignalMeterBlock;
import com.lombinaxmasson.gt6logic.block.TankBlock;
import com.lombinaxmasson.gt6logic.block.ThermostatBlock;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** The creative tab keeps the GT-side blocks and uses the electric meter as its icon. */
@GameTestHolder("gt6logic")
@PrefixGameTestTemplate(false)
public final class SecondTierTabTests {
    private static final List<String> REMOVED = List.of(
            "gt6logic:indicator",
            "gt6logic:warning_beacon",
            "gt6logic:console_panel",
            "gt6logic:delay_relay",
            "gt6logic:counter",
            "gt6logic:interlock",
            "gt6logic:selector_switch",
            "gt6logic:kitchen_timer",
            "gt6logic:big_lever",
            "gt6logic:labeled_button",
            "gt6logic:emergency_stop",
            "gt6logic:alarm_bell");

    private SecondTierTabTests() {}

    @GameTest(template = "test_empty", timeoutTicks = 20)
    public static void creativeTabListsTheRemainingBlocks(GameTestHelper helper) {
        CreativeModeTab tab = Gt6Logic.CREATIVE_TAB.get();
        tab.buildContents(new CreativeModeTab.ItemDisplayParameters(
                FeatureFlags.VANILLA_SET, true, helper.getLevel().registryAccess()));
        helper.assertTrue(tab.getIconItem().is(ElectricMeterBlock.ITEM.get()), "creative tab icon is the electric meter");
        for (ItemStack stack : tab.getDisplayItems()) {
            String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            helper.assertTrue(!REMOVED.contains(id), id + " was removed");
        }
        assertListed(helper, tab, SignalMeterBlock.BLOCK.get(), "block.gt6logic.signal_meter", "Signal Meter", "信号表");
        assertListed(helper, tab, CompensationRepeaterBlock.BLOCK.get(),
                "block.gt6logic.compensation_repeater", "Compensation Repeater", "补偿中继器");
        assertListed(helper, tab, ArithmeticBlock.BLOCK.get(), "block.gt6logic.arithmetic", "Arithmetic Module", "算术模块");
        assertListed(helper, tab, RatioControllerBlock.BLOCK.get(), "block.gt6logic.ratio_controller", "Ratio Controller", "配比控制器");
        assertListed(helper, tab, ProportionalControllerBlock.BLOCK.get(),
                "block.gt6logic.proportional_controller", "Proportional Controller", "比例控制器");
        assertListed(helper, tab, ProgramThermostatBlock.BLOCK.get(), "block.gt6logic.program_thermostat", "Program Thermostat", "程序控温器");
        assertListed(helper, tab, FormulaModuleBlock.BLOCK.get(), "block.gt6logic.formula_module", "Formula Module", "可编程模块");
        assertListed(helper, tab, TankBlock.BLOCK.get(), "block.gt6logic.tank", "Tank", "储罐");
        assertListed(helper, tab, ElectricValveBlock.BLOCK.get(), "block.gt6logic.electric_valve", "Electric Valve", "电动阀");
        assertListed(helper, tab, ControlValveBlock.BLOCK.get(), "block.gt6logic.control_valve", "Control Valve", "调节阀");
        assertListed(helper, tab, AdjustablePumpBlock.BLOCK.get(), "block.gt6logic.adjustable_pump", "Adjustable Pump", "可调泵");
        assertListed(helper, tab, FloatSwitchBlock.BLOCK.get(), "block.gt6logic.float_switch", "Float Switch", "浮球开关");
        assertListed(helper, tab, AdjustableHeaterBlock.BLOCK.get(), "block.gt6logic.adjustable_heater", "Adjustable Heater", "可调加热器");
        assertListed(helper, tab, AdjustableCoolerBlock.BLOCK.get(), "block.gt6logic.adjustable_cooler", "Adjustable Cooler", "可调冷却器");
        assertListed(helper, tab, ThermostatBlock.BLOCK.get(), "block.gt6logic.thermostat", "Thermostat", "恒温器");
        assertListed(helper, tab, ElectricMeterBlock.BLOCK.get(), "block.gt6logic.electric_meter", "Electric Meter", "电表");
        assertListed(helper, tab, BatteryStewardBlock.BLOCK.get(), "block.gt6logic.battery_steward", "Battery Steward", "蓄电池管家");
        assertListed(helper, tab, DigitDisplayBlock.BLOCK.get(), "block.gt6logic.digit_display", "Digit Display", "数码管");
        assertListed(helper, tab, PointerGaugeBlock.BLOCK.get(), "block.gt6logic.pointer_gauge", "Pointer Gauge", "指针表");
        assertListed(helper, tab, ChartRecorderBlock.BLOCK.get(), "block.gt6logic.chart_recorder", "Chart Recorder", "记录仪");
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
