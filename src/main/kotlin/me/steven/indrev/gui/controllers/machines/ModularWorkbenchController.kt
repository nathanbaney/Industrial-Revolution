package me.steven.indrev.gui.controllers.machines

import io.github.cottonmc.cotton.gui.widget.WBar
import io.github.cottonmc.cotton.gui.widget.WGridPanel
import io.github.cottonmc.cotton.gui.widget.data.HorizontalAlignment
import me.steven.indrev.IndustrialRevolution
import me.steven.indrev.blockentities.modularworkbench.ModularWorkbenchBlockEntity
import me.steven.indrev.gui.controllers.IRGuiController
import me.steven.indrev.gui.widgets.misc.WStaticTooltip
import me.steven.indrev.gui.widgets.misc.WText
import me.steven.indrev.gui.widgets.misc.WTooltipedItemSlot
import me.steven.indrev.items.armor.IRModularArmor
import me.steven.indrev.items.armor.IRModuleItem
import me.steven.indrev.tools.modular.ArmorModule
import me.steven.indrev.tools.modular.IRModularItem
import me.steven.indrev.utils.*
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.text.LiteralText
import net.minecraft.text.TranslatableText
import net.minecraft.util.Formatting

class ModularWorkbenchController(syncId: Int, playerInventory: PlayerInventory, ctx: ScreenHandlerContext) :
    IRGuiController(
        IndustrialRevolution.MODULAR_WORKBENCH_HANDLER,
        syncId,
        playerInventory,
        ctx
    ) {

    val blockInventory: Inventory
        get() = blockInventory

    init {
        val root = WGridPanel()
        setRootPanel(root)
        configure("block.indrev.modular_workbench", ctx, playerInventory, blockInventory)

        val armorSlot = WTooltipedItemSlot.of(blockInventory, 2, TranslatableText("gui.indrev.modular_armor_slot_type"))
        root.add(armorSlot, 1.5, 3.5)

        val moduleSlot = WTooltipedItemSlot.of(blockInventory, 1, TranslatableText("gui.indrev.module_slot_type"))
        root.add(moduleSlot, 1.5, 1.0)

        val process = createProcessBar(WBar.Direction.DOWN, PROCESS_VERTICAL_EMPTY, PROCESS_VERTICAL_FULL, 2, 3)
        root.add(process, 1.5, 2.2)

        val info = WStaticTooltip()
        info.setSize(100, 60)
        root.add(info, 3, 1)

        addTextInfo(root)

        root.validate(this)
    }

    private fun addTextInfo(panel: WGridPanel) {
        val armorInfoText = WText({
            val stack = blockInventory.getStack(2)
            if (!stack.isEmpty)
                TranslatableText(stack.item.translationKey).formatted(Formatting.DARK_PURPLE, Formatting.UNDERLINE)
            else LiteralText.EMPTY
        }, HorizontalAlignment.LEFT)

        val moduleToInstall = WText({
            val (stack, item) = blockInventory.getStack(1)
            if (!stack.isEmpty && item is IRModuleItem) {
                TranslatableText(item.translationKey).formatted(Formatting.GRAY, Formatting.ITALIC)
            } else LiteralText.EMPTY
        }, HorizontalAlignment.LEFT)

        val modulesInstalled = WText({
            val (stack, item) = blockInventory.getStack(2)
            if (!stack.isEmpty && item is IRModularItem<*>) {
                val modules = item.getCount(stack).toString()
                MODULE_COUNT().append(LiteralText(modules).formatted(Formatting.WHITE))
            } else LiteralText.EMPTY
        }, HorizontalAlignment.LEFT)

        val shield = WText({
            val (stack, item) = blockInventory.getStack(2)
            if (!stack.isEmpty && item is IRModularArmor) {
                val shield = item.getMaxShield(ArmorModule.PROTECTION.getLevel(stack)).toString()
                SHIELD_TEXT().append(LiteralText(shield).formatted(Formatting.WHITE))
            } else LiteralText.EMPTY
        }, HorizontalAlignment.LEFT)

        val installing = WText({
            val state = ModularWorkbenchBlockEntity.State.values()[propertyDelegate[4]]
            if (state == ModularWorkbenchBlockEntity.State.INSTALLING) {
                INSTALLING_TEXT()
            } else LiteralText.EMPTY
        }, HorizontalAlignment.LEFT)

        val progress = WText({
            val stack = blockInventory.getStack(1)
            val item = stack.item
            if (!stack.isEmpty && item is IRModuleItem) {
                val progress = propertyDelegate!![2]
                when (ModularWorkbenchBlockEntity.State.values()[propertyDelegate[4]]) {
                    ModularWorkbenchBlockEntity.State.INCOMPATIBLE -> INCOMPATIBLE_TEXT()
                    ModularWorkbenchBlockEntity.State.MAX_LEVEL -> MAX_LEVEL_TEXT()
                    else -> {
                        val percent = ((progress / 1200f) * 100).toInt()
                        PROGRESS_TEXT().append(LiteralText("$percent%"))
                    }
                }
            } else LiteralText.EMPTY
        }, HorizontalAlignment.LEFT)

        panel.add(installing, 3, 3)
        panel.add(progress, 3, 4)
        panel.add(armorInfoText, 3, 1)
        panel.add(modulesInstalled, 3.0, 1.5)
        panel.add(shield, 3, 2)
        panel.add(moduleToInstall, 3.0, 3.5)
    }

    companion object {
        val SCREEN_ID = identifier("modular_workbench_screen")
        val SHIELD_TEXT = { TranslatableText("gui.indrev.shield").formatted(Formatting.BLUE) }
        val PROGRESS_TEXT = { TranslatableText("gui.indrev.progress").formatted(Formatting.BLUE) }
        val MODULE_COUNT = { TranslatableText("gui.indrev.modules_installed").formatted(Formatting.BLUE) }
        val INSTALLING_TEXT = { TranslatableText("gui.indrev.installing").formatted(Formatting.DARK_PURPLE, Formatting.UNDERLINE) }
        val INCOMPATIBLE_TEXT = { TranslatableText("gui.indrev.incompatible").formatted(Formatting.RED) }
        val MAX_LEVEL_TEXT = { TranslatableText("gui.indrev.max_level").formatted(Formatting.RED) }
    }

}