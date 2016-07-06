/*
 * Copyright (c) bdew, 2014 - 2016
 * https://github.com/bdew/generators
 *
 * This mod is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://bdew.net/minecraft-mod-public-license/
 */

package net.bdew.generators.modules.pressure

import net.bdew.generators.modules.BaseModule
import net.bdew.lib.Misc
import net.bdew.lib.multiblock.block.BlockOutput
import net.bdew.lib.multiblock.data.OutputConfigFluidSlots
import net.bdew.lib.multiblock.interact.CIFluidOutputSelect
import net.bdew.lib.multiblock.tile.{RSControllableOutput, TileOutput}
import net.bdew.pressure.api.{IPressureConnectableBlock, IPressureConnection, IPressureInject, PressureAPI}
import net.minecraft.block.state.IBlockState
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import net.minecraft.world.IBlockAccess

object BlockPressureOutput extends BaseModule("PressureOutputSelect", "FluidOutputSelect", classOf[TilePressureOutput])
  with BlockOutput[TilePressureOutput] with BlockNotifyUpdates with IPressureConnectableBlock {
  override def canConnectTo(world: IBlockAccess, pos: BlockPos, side: EnumFacing) =
    getTE(world, pos).exists(_.getCore.isDefined)
  override def isTraversable(world: IBlockAccess, pos: BlockPos) = false
  override def canConnectRedstone(state: IBlockState, world: IBlockAccess, pos: BlockPos, side: EnumFacing): Boolean = true
}

class TilePressureOutput extends TileOutput[OutputConfigFluidSlots] with PressureModule with RSControllableOutput with IPressureInject {
  val kind: String = "FluidOutputSelect"

  override val outputConfigType = classOf[OutputConfigFluidSlots]

  override def getCore = getCoreAs[CIFluidOutputSelect]

  override def canConnectToFace(d: EnumFacing) =
    PressureAPI.HELPER.canPipeConnectFrom(worldObj, pos.offset(d), d.getOpposite)

  override def makeCfgObject(face: EnumFacing) = new OutputConfigFluidSlots(getCore.get.outputSlotsDef)

  override def invalidateConnection(direction: EnumFacing) = connections -= direction

  var connections = Map.empty[EnumFacing, IPressureConnection]

  override def doOutput(face: EnumFacing, cfg: OutputConfigFluidSlots) = {
    val outputted = if (checkCanOutput(cfg)) {
      if (!connections.isDefinedAt(face))
        connections ++= Option(PressureAPI.HELPER.recalculateConnectionInfo(this, face)) map { cObj => face -> cObj }
      for {
        core <- getCore
        tSlot <- Misc.asInstanceOpt(cfg.slot, classOf[core.outputSlotsDef.Slot])
        toSend <- Option(core.outputFluid(tSlot, Int.MaxValue, false))
        conn <- connections.get(face)
      } yield {
        val filled = conn.pushFluid(toSend, true)
        if (filled > 0) {
          core.outputFluid(tSlot, filled, true)
          core.outputConfig.updated()
          filled
        } else 0
      }
    } else None
    cfg.updateAvg(outputted.getOrElse(0).toDouble)
  }
}

