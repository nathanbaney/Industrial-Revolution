package me.steven.indrev.blocks

import me.steven.indrev.blockentities.MachineBlockEntity
import me.steven.indrev.gui.IRScreenHandlerFactory
import me.steven.indrev.items.IRMachineUpgradeItem
import me.steven.indrev.items.IRWrenchItem
import me.steven.indrev.utils.Tier
import me.steven.indrev.utils.getShortEnergyDisplay
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.block.Block
import net.minecraft.block.BlockEntityProvider
import net.minecraft.block.BlockState
import net.minecraft.block.InventoryProvider
import net.minecraft.block.entity.BlockEntity
import net.minecraft.client.item.TooltipContext
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.SidedInventory
import net.minecraft.item.BlockItem
import net.minecraft.item.ItemPlacementContext
import net.minecraft.item.ItemStack
import net.minecraft.particle.ParticleTypes
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.server.world.ServerWorld
import net.minecraft.stat.Stats
import net.minecraft.state.StateManager
import net.minecraft.state.property.BooleanProperty
import net.minecraft.text.LiteralText
import net.minecraft.text.Text
import net.minecraft.text.TranslatableText
import net.minecraft.util.ActionResult
import net.minecraft.util.Formatting
import net.minecraft.util.Hand
import net.minecraft.util.ItemScatterer
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.world.BlockView
import net.minecraft.world.World
import net.minecraft.world.WorldAccess
import java.util.*

open class MachineBlock(
    settings: Settings,
    val tier: Tier,
    private val screenHandler: ((Int, PlayerInventory, ScreenHandlerContext) -> ScreenHandler)?,
    private val blockEntityProvider: () -> MachineBlockEntity
) : Block(settings), BlockEntityProvider, InventoryProvider {

    init {
        if (this.defaultState.contains(WORKING_PROPERTY))
            this.defaultState = stateManager.defaultState.with(WORKING_PROPERTY, false)
    }

    override fun appendProperties(builder: StateManager.Builder<Block, BlockState>?) {
        super.appendProperties(builder)
        builder?.add(WORKING_PROPERTY)
    }

    override fun getPlacementState(ctx: ItemPlacementContext?): BlockState? {
        return defaultState.with(WORKING_PROPERTY, false)
    }

    override fun createBlockEntity(view: BlockView?): BlockEntity? = blockEntityProvider()

    override fun buildTooltip(
        stack: ItemStack?,
        view: BlockView?,
        tooltip: MutableList<Text>?,
        options: TooltipContext?
    ) {
        tooltip?.add(TranslatableText("block.machines.tooltip.io", LiteralText("${tier.io} LF/tick").formatted(Formatting.WHITE)).formatted(Formatting.BLUE))
        val infoTag = stack?.getSubTag("MachineInfo") ?: return
        val energy = infoTag.getDouble("Energy")
        tooltip?.add(TranslatableText("gui.widget.energy").formatted(Formatting.BLUE).append(LiteralText(": ${getShortEnergyDisplay(energy)} LF").formatted(Formatting.WHITE)))
    }

    override fun onUse(
        state: BlockState?,
        world: World,
        pos: BlockPos?,
        player: PlayerEntity?,
        hand: Hand?,
        hit: BlockHitResult?
    ): ActionResult? {
        val blockEntity = world.getBlockEntity(pos) as? MachineBlockEntity ?: return ActionResult.FAIL
        val stack = player?.mainHandStack
        val item = stack?.item
        if (item is IRWrenchItem || item is IRMachineUpgradeItem) return ActionResult.PASS
        else if (screenHandler != null
            && blockEntity.inventoryController != null) {
            player?.openHandledScreen(IRScreenHandlerFactory(screenHandler, pos!!))?.ifPresent { syncId ->
                blockEntity.viewers[player.uuid] = syncId
            }
        }
        return ActionResult.SUCCESS
    }

    override fun onStateReplaced(state: BlockState, world: World, pos: BlockPos?, newState: BlockState, moved: Boolean) {
        if (!state.isOf(newState.block)) {
            val blockEntity = world.getBlockEntity(pos) as? MachineBlockEntity ?: return
            if (blockEntity.inventoryController != null) {
                ItemScatterer.spawn(world, pos, blockEntity.inventoryController!!.inventory)
                world.updateComparators(pos, this)
            }
            super.onStateReplaced(state, world, pos, newState, moved)
        }
    }

    override fun afterBreak(world: World?, player: PlayerEntity?, pos: BlockPos?, state: BlockState?, blockEntity: BlockEntity?, toolStack: ItemStack?) {
        player?.incrementStat(Stats.MINED.getOrCreateStat(this))
        player?.addExhaustion(0.005f)
        if (world is ServerWorld) {
            getDroppedStacks(state, world, pos, blockEntity, player, toolStack).forEach { stack ->
                val item = stack.item
                if (blockEntity is MachineBlockEntity && item is BlockItem && item.block is MachineBlock) {
                    val tag = stack.getOrCreateSubTag("MachineInfo")
                    tag.putDouble("Energy", blockEntity.energy)
                    val temperatureController = blockEntity.temperatureController
                    if (temperatureController != null)
                        tag.putDouble("Temperature", temperatureController.temperature)
                }
                dropStack(world, pos, stack)
            }
        }
        state!!.onStacksDropped(world, pos, toolStack)
    }

    override fun onPlaced(world: World?, pos: BlockPos?, state: BlockState?, placer: LivingEntity?, itemStack: ItemStack?) {
        super.onPlaced(world, pos, state, placer, itemStack)
        val tag = itemStack?.getSubTag("MachineInfo") ?: return
        val blockEntity = world?.getBlockEntity(pos) as? MachineBlockEntity ?: return
        val temperatureController = blockEntity.temperatureController
        val energy = tag.getDouble("Energy")
        blockEntity.energy = energy
        if (temperatureController != null) {
            val temperature = tag.getDouble("Temperature")
            temperatureController.temperature = temperature
        }
    }

    override fun getInventory(state: BlockState?, world: WorldAccess?, pos: BlockPos?): SidedInventory {
        val blockEntity = world?.getBlockEntity(pos) as? InventoryProvider
            ?: throw IllegalArgumentException("tried to retrieve an inventory from an invalid block entity")
        return blockEntity.getInventory(state, world, pos)
    }

    @Environment(EnvType.CLIENT)
    override fun randomDisplayTick(state: BlockState?, world: World, pos: BlockPos, random: Random?) {
        if (state?.contains(WORKING_PROPERTY) == true && state[WORKING_PROPERTY]) {
            val d = pos.x.toDouble() + 0.5
            val e = pos.y.toDouble() + 1.0
            val f = pos.z.toDouble() + 0.5
            world.addParticle(ParticleTypes.SMOKE, d, e, f, 0.0, 0.0, 0.0)
        }
    }

    companion object {
        val WORKING_PROPERTY: BooleanProperty = BooleanProperty.of("working")
    }
}