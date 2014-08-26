package net.shadowmage.ancientwarfare.automation.tile.worksite;

import java.util.ArrayList;
import java.util.EnumSet;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.scoreboard.Team;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.ForgeDirection;
import net.shadowmage.ancientwarfare.automation.item.ItemWorksiteUpgrade;
import net.shadowmage.ancientwarfare.core.config.AWCoreStatics;
import net.shadowmage.ancientwarfare.core.interfaces.IBoundedTile;
import net.shadowmage.ancientwarfare.core.interfaces.IInteractableTile;
import net.shadowmage.ancientwarfare.core.interfaces.IOwnable;
import net.shadowmage.ancientwarfare.core.interfaces.ITorque.ITorqueReceiver;
import net.shadowmage.ancientwarfare.core.interfaces.IWorkSite;
import net.shadowmage.ancientwarfare.core.interfaces.IWorker;
import net.shadowmage.ancientwarfare.core.upgrade.WorksiteUpgrade;
import net.shadowmage.ancientwarfare.core.util.BlockPosition;
import net.shadowmage.ancientwarfare.core.util.InventoryTools;

public abstract class TileWorksiteBase extends TileEntity implements IWorkSite, IInventory, ISidedInventory, IInteractableTile, IBoundedTile, IOwnable, ITorqueReceiver
{
protected String owningPlayer = "";

protected ArrayList<ItemStack> inventoryOverflow = new ArrayList<ItemStack>();

private double maxEnergyStored = AWCoreStatics.energyPerWorkUnit*3;
private double maxInput = maxEnergyStored;
private double storedEnergy;
private double efficiency;

private EnumSet<WorksiteUpgrade> upgrades = EnumSet.noneOf(WorksiteUpgrade.class);

public TileWorksiteBase()
  {
  
  }

@Override
public int getBoundsMaxWidth(){return 3;}

@Override
public int getBoundsMaxHeight(){return 1;}

@Override
public EnumSet<WorksiteUpgrade> getUpgrades(){return upgrades;}

@Override
public EnumSet<WorksiteUpgrade> getValidUpgrades()
  {
  return EnumSet.of(
      WorksiteUpgrade.ENCHANTED_TOOLS_1,
      WorksiteUpgrade.ENCHANTED_TOOLS_2,
      WorksiteUpgrade.TOOL_QUALITY_1,
      WorksiteUpgrade.TOOL_QUALITY_2,
      WorksiteUpgrade.TOOL_QUALITY_3
      );
  }

@Override
public void onBlockBroken()
  {
  for(WorksiteUpgrade ug : this.upgrades)
    {
    InventoryTools.dropItemInWorld(worldObj, ItemWorksiteUpgrade.getStack(ug), xCoord, yCoord, zCoord);
    }
  efficiency = 0;
  upgrades.clear();
  }

@Override
public void addUpgrade(WorksiteUpgrade upgrade)
  {
  upgrades.add(upgrade);
  updateEfficiency();
  }

@Override
public void removeUpgrade(WorksiteUpgrade upgrade)
  {
  upgrades.remove(upgrade);
  updateEfficiency();
  }

@Override
public ForgeDirection getOrientation()
  {
  return ForgeDirection.getOrientation(getBlockMetadata());
  }

protected abstract boolean processWork();

protected abstract boolean hasWorksiteWork();

protected abstract void updateOverflowInventory();

protected abstract void updateWorksite();

@Override
public boolean shouldRenderInPass(int pass)
  {
  return pass==1;
  }

@Override
public abstract boolean onBlockClicked(EntityPlayer player);

@Override
public final void setEnergy(double energy)
  {
  this.storedEnergy = energy;
  if(this.storedEnergy>this.maxEnergyStored)
    {
    this.storedEnergy = this.maxEnergyStored;
    }
  }

@Override
public double getEnergyDrainFactor()
  {
  return 1;
  }

@Override
public void addEnergyFromPlayer(EntityPlayer player)
  {
  storedEnergy+=AWCoreStatics.energyPerWorkUnit;
  if(storedEnergy>getMaxEnergy()){storedEnergy=getMaxEnergy();}
  }

@Override
public final double addEnergy(ForgeDirection from, double energy)
  {
  if(canInput(from))
    {
    if(energy+getEnergyStored()>getMaxEnergy())
      {
      energy = getMaxEnergy()-getEnergyStored();
      }
    if(energy>getMaxInput())
      {
      energy = getMaxInput();
      }
    storedEnergy+=energy;
    if(storedEnergy>getMaxEnergy()){storedEnergy=getMaxEnergy();}
    return energy;    
    }
  return 0;
  }

@Override
public String toString()
  {
  return "Worksite Base["+storedEnergy+"]";
  }

@Override
public final double getMaxEnergy()
  {
  return maxEnergyStored;
  }

@Override
public final double getEnergyStored()
  {
  return storedEnergy;
  }

@Override
public final double getMaxInput()
  {
  return maxInput;
  }

@Override
public boolean canInput(ForgeDirection from)
  {
  return true;
  }

@Override
public boolean hasWork()
  {
  return storedEnergy < maxEnergyStored && !worldObj.isBlockIndirectlyGettingPowered(xCoord, yCoord, zCoord) && inventoryOverflow.isEmpty();  
  }

@Override
public final String getOwnerName()
  {  
  return owningPlayer;
  }

@Override
public final boolean canUpdate()
  {
  return true;
  }

@Override
public void updateEntity()
  {
  super.updateEntity();
  if(worldObj.isRemote){return;}  
  worldObj.theProfiler.startSection("AWWorksite");
  worldObj.theProfiler.startSection("InventoryOverflow");
  if(!inventoryOverflow.isEmpty())
    {
    updateOverflowInventory();
    } 
  worldObj.theProfiler.endStartSection("Check For Work");
  boolean hasWork = getEnergyStored() >= getEnergyPerUse() && hasWorksiteWork();
  worldObj.theProfiler.endStartSection("Process Work");
  if(hasWork)
    {
    if(processWork())
      {
      storedEnergy -= getEnergyPerUse();
      if(storedEnergy<0){storedEnergy = 0.d;}
      }    
    }
  worldObj.theProfiler.endStartSection("WorksiteBaseUpdate");
  updateWorksite();
  worldObj.theProfiler.endSection();
  worldObj.theProfiler.endSection();
  }

protected double getEnergyPerUse()
  {
  return AWCoreStatics.energyPerWorkUnit * efficiency;
  }

protected void updateEfficiency()
  {
  efficiency = 0.d;
  if(upgrades.contains(WorksiteUpgrade.ENCHANTED_TOOLS_1)){efficiency+=0.05;}
  if(upgrades.contains(WorksiteUpgrade.ENCHANTED_TOOLS_2)){efficiency+=0.1;}
  if(upgrades.contains(WorksiteUpgrade.TOOL_QUALITY_1)){efficiency+=0.05;}
  if(upgrades.contains(WorksiteUpgrade.TOOL_QUALITY_2)){efficiency+=0.15;}
  if(upgrades.contains(WorksiteUpgrade.TOOL_QUALITY_3)){efficiency+=0.25;}
  }

@Override
public void addEnergyFromWorker(IWorker worker)
  {  
  storedEnergy += AWCoreStatics.energyPerWorkUnit * worker.getWorkEffectiveness(getWorkType());
  if(storedEnergy>getMaxEnergy()){storedEnergy = getMaxEnergy();}
//  AWLog.logDebug("adding energy from worker..."+storedEnergy);
  }

@Override
public final void setOwnerName(String name)
  {
  if(name==null){name="";}
  this.owningPlayer = name;  
  }

@Override
public void writeToNBT(NBTTagCompound tag)
  {
  super.writeToNBT(tag);
  tag.setDouble("storedEnergy", storedEnergy);
  if(owningPlayer!=null)
    {
    tag.setString("owner", owningPlayer);
    }
  if(!inventoryOverflow.isEmpty())
    {
    NBTTagList list = new NBTTagList();
    for(ItemStack item : inventoryOverflow)
      {
      list.appendTag(InventoryTools.writeItemStack(item, new NBTTagCompound()));
      }
    tag.setTag("inventoryOverflow", list);
    }
  int[] ug = new int[getUpgrades().size()];
  int i = 0;
  for(WorksiteUpgrade u : getUpgrades())
    {
    ug[i] = u.ordinal();
    i++;
    }
  tag.setIntArray("upgrades", ug);
  }

@Override
public void readFromNBT(NBTTagCompound tag)
  {
  super.readFromNBT(tag);
  storedEnergy = tag.getDouble("storedEnergy");
  if(tag.hasKey("owner"))
    {
    owningPlayer = tag.getString("owner");
    }
  if(tag.hasKey("inventoryOverflow"))
    {
    NBTTagList list = tag.getTagList("inventoryOverflow", Constants.NBT.TAG_COMPOUND);
    NBTTagCompound itemTag;
    ItemStack stack;
    for(int i = 0; i < list.tagCount(); i++)
      {
      itemTag = list.getCompoundTagAt(i);
      stack = InventoryTools.readItemStack(itemTag);
      if(stack!=null)
        {
        inventoryOverflow.add(stack);
        }
      }
    }
  int[] ug = tag.getIntArray("upgrades");
  for(int i= 0; i < ug.length; i++)
    {
    addUpgrade(WorksiteUpgrade.values()[i]);
    }
  updateEfficiency();
  }

@Override
public final Team getTeam()
  {  
  if(owningPlayer!=null)
    {
    worldObj.getScoreboard().getPlayersTeam(owningPlayer);
    }
  return null;
  }

@Override
public final AxisAlignedBB getRenderBoundingBox()
  {
  if(hasWorkBounds() && getWorkBoundsMin()!=null && getWorkBoundsMax()!=null)
    {
    AxisAlignedBB bb = AxisAlignedBB.getBoundingBox(xCoord, yCoord, zCoord, xCoord+1, yCoord+1, zCoord+1);
    BlockPosition min = getWorkBoundsMin();
    BlockPosition max = getWorkBoundsMax();
    bb.minX = min.x < bb.minX ? min.x : bb.minX;
    bb.minY = min.y < bb.minY ? min.y : bb.minY;
    bb.minZ = min.z < bb.minZ ? min.z : bb.minZ;
    bb.maxX = max.x+1 > bb.maxX ? max.x+1 : bb.maxX;
    bb.maxY = max.y+1 > bb.maxY ? max.y+1 : bb.maxY;
    bb.maxZ = max.z+1 > bb.maxZ ? max.z+1 : bb.maxZ;
    return bb;
    }
  AxisAlignedBB bb = super.getRenderBoundingBox();
  return bb;
  }

}
