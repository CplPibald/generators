/*
 * Copyright (c) bdew, 2014
 * https://github.com/bdew/generators
 *
 * This mod is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://bdew.net/minecraft-mod-public-license/
 */

package net.bdew.generators.modules.euOutput

import ic2.api.energy.tile.{IEnergyAcceptor, IEnergySource}
import net.bdew.generators.compat.Ic2EnetRegister
import net.bdew.generators.config.Tuning
import net.bdew.lib.data.DataSlotDouble
import net.bdew.lib.data.base.UpdateKind
import net.bdew.lib.multiblock.data.OutputConfigPower
import net.bdew.lib.multiblock.interact.CIPowerProducer
import net.bdew.lib.multiblock.tile.{RSControllableOutput, TileOutput}
import net.bdew.lib.rotate.RotatableTile
import net.minecraft.tileentity.TileEntity
import net.minecraftforge.common.util.ForgeDirection

abstract class TileEuOutputBase(val maxOutput: Int, val tier: Int) extends TileOutput[OutputConfigPower] with RSControllableOutput with IEnergySource with Ic2EnetRegister with RotatableTile {
  val kind = "PowerOutput"
  val ratio = Tuning.getSection("Power").getFloat("EU_MJ_Ratio")

  val buffer = DataSlotDouble("buffer", this).setUpdate(UpdateKind.SAVE)

  override val outputConfigType = classOf[OutputConfigPower]
  override def makeCfgObject(face: ForgeDirection) = new OutputConfigPower("EU")

  override def canConnectToFace(d: ForgeDirection): Boolean = {
    if (rotation.value != d) return false
    val tile = myPos.neighbour(d).getTile[IEnergyAcceptor](worldObj).getOrElse(return false)
    return tile.acceptsEnergyFrom(this, d.getOpposite)
  }

  override def onConnectionsChanged(added: Set[ForgeDirection], removed: Set[ForgeDirection]) {
    sendUnload()
  }

  override def emitsEnergyTo(receiver: TileEntity, direction: ForgeDirection) =
    getCore.isDefined && rotation.value == direction

  def getCfg: Option[OutputConfigPower] = {
    val core = getCoreAs[CIPowerProducer].getOrElse(return None)
    val oNum = core.outputFaces.find(_._1.origin == myPos).getOrElse(return None)._2
    Some(core.outputConfig.getOrElse(oNum, return None).asInstanceOf[OutputConfigPower])
  }

  override def getOfferedEnergy: Double = {
    if (buffer > 0 && checkCanOutput(getCfg.getOrElse(return 0)))
      Math.min(buffer, maxOutput)
    else
      0
  }

  override def drawEnergy(amount: Double) {
    buffer := buffer - amount
  }

  override def getSourceTier = tier

  def doOutput(face: ForgeDirection, cfg: OutputConfigPower): Unit = {
    getCoreAs[CIPowerProducer] foreach { core =>
      if (buffer < maxOutput) {
        val extracted = core.extract(maxOutput / ratio, false) * ratio
        buffer += extracted
        cfg.updateAvg(extracted)
        core.outputConfig.updated()
      } else {
        cfg.updateAvg(0)
        core.outputConfig.updated()
      }
    }
  }
}

class TileEuOutputLV extends TileEuOutputBase(128, 1)

class TileEuOutputMV extends TileEuOutputBase(512, 2)

class TileEuOutputHV extends TileEuOutputBase(2048, 3)

class TileEuOutputEV extends TileEuOutputBase(8192, 4)
