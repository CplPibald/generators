/*
 * Copyright (c) bdew, 2014
 * https://github.com/bdew/generators
 *
 * This mod is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://bdew.net/minecraft-mod-public-license/
 */

package net.bdew.generators.modules.euOutput

import cpw.mods.fml.relauncher.{Side, SideOnly}
import net.bdew.generators.Generators
import net.bdew.generators.modules.BaseModule
import net.bdew.lib.Misc
import net.bdew.lib.multiblock.block.BlockOutput
import net.bdew.lib.rotate.{IconType, RotatableTileBlock}
import net.minecraft.client.renderer.texture.IIconRegister
import net.minecraft.util.IIcon
import net.minecraft.world.{IBlockAccess, World}
import net.minecraftforge.common.util.ForgeDirection

class BlockEuOutputBase[T <: TileEuOutputBase](name: String, texture: String, TEClass: Class[T])
  extends BaseModule(name, "PowerOutput", TEClass) with BlockOutput[T]
  with RotatableTileBlock {
  var frontIcon: IIcon = null

  override def setFacing(world: World, x: Int, y: Int, z: Int, facing: ForgeDirection) {
    super.setFacing(world, x, y, z, facing)
    getTE(world, x, y, z).tryConnect()
  }

  override def getDefaultFacing = ForgeDirection.SOUTH

  @SideOnly(Side.CLIENT)
  override def registerBlockIcons(ir: IIconRegister) {
    blockIcon = ir.registerIcon(Misc.iconName(Generators.modId, "euoutput", "main"))
    frontIcon = ir.registerIcon(Misc.iconName(Generators.modId, "euoutput", "front_" + texture))
  }

  def getIcon(meta: Int, kind: IconType.Value) = if (kind == IconType.FRONT) frontIcon else blockIcon

  override def canConnectRedstone(world: IBlockAccess, x: Int, y: Int, z: Int, side: Int) = true
}

object BlockEuOutputLV extends BlockEuOutputBase("EuOutputLV", "lv", classOf[TileEuOutputLV])

object BlockEuOutputMV extends BlockEuOutputBase("EuOutputMV", "mv", classOf[TileEuOutputMV])

object BlockEuOutputHV extends BlockEuOutputBase("EuOutputHV", "hv", classOf[TileEuOutputHV])

object BlockEuOutputEV extends BlockEuOutputBase("EuOutputEV", "ev", classOf[TileEuOutputEV])