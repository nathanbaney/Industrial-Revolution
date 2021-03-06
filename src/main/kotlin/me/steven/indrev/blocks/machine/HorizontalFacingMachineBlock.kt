package me.steven.indrev.blocks.machine

import me.steven.indrev.blockentities.MachineBlockEntity
import me.steven.indrev.config.IConfig
import me.steven.indrev.utils.Tier
import me.steven.indrev.utils.TransferMode
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.item.ItemPlacementContext
import net.minecraft.item.ItemStack
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.state.StateManager
import net.minecraft.state.property.DirectionProperty
import net.minecraft.state.property.Properties
import net.minecraft.util.BlockRotation
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World

open class HorizontalFacingMachineBlock(
    settings: Settings,
    tier: Tier,
    config: IConfig?,
    screenHandler: ((Int, PlayerInventory, ScreenHandlerContext) -> ScreenHandler)?,
    blockEntityProvider: () -> MachineBlockEntity<*>
) : MachineBlock(settings, tier, config, screenHandler, blockEntityProvider) {

    override fun getPlacementState(ctx: ItemPlacementContext?): BlockState? {
        super.getPlacementState(ctx)
        return this.defaultState.with(HORIZONTAL_FACING, ctx?.playerFacing?.opposite)
    }

    override fun appendProperties(builder: StateManager.Builder<Block, BlockState>?) {
        super.appendProperties(builder)
        builder?.add(HORIZONTAL_FACING)
    }

    override fun onPlaced(world: World?, pos: BlockPos, state: BlockState?, placer: LivingEntity?, itemStack: ItemStack?) {
        val blockEntity = world?.getBlockEntity(pos)
        if (blockEntity is MachineBlockEntity<*>) {
            val direction = state?.get(HORIZONTAL_FACING) ?: return
            val inventoryController = blockEntity.inventoryComponent ?: return
            val itemConfig = inventoryController.itemConfig
            itemConfig[direction.rotateYClockwise()] = TransferMode.INPUT
            itemConfig[direction.rotateYCounterclockwise()] = TransferMode.OUTPUT
        }
        super.onPlaced(world, pos, state, placer, itemStack)
    }

    override fun rotate(state: BlockState, rotation: BlockRotation): BlockState {
        return state.with(HORIZONTAL_FACING, getRotated(state[HORIZONTAL_FACING], rotation))
    }

    companion object {
        val HORIZONTAL_FACING: DirectionProperty = Properties.HORIZONTAL_FACING

        fun getRotated(direction: Direction, rotation: BlockRotation): Direction = when (rotation) {
            BlockRotation.NONE -> direction
            BlockRotation.CLOCKWISE_90 -> direction.rotateYClockwise()
            BlockRotation.CLOCKWISE_180 -> direction.opposite
            BlockRotation.COUNTERCLOCKWISE_90 -> direction.rotateYCounterclockwise()
        }
    }
}