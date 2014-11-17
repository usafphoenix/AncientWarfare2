package net.shadowmage.ancientwarfare.structure.town;

import java.util.Random;

import net.minecraft.world.World;
import net.shadowmage.ancientwarfare.structure.template.StructureTemplate;
import net.shadowmage.ancientwarfare.structure.template.StructureTemplateManager;
import net.shadowmage.ancientwarfare.structure.template.build.StructureBuilder;
import net.shadowmage.ancientwarfare.structure.town.TownTemplate.TownWallEntry;

public class TownGeneratorWalls
{

public static void generateWalls(World world, TownBoundingArea area, TownTemplate template, Random rng)
  {
  if(template.getWallStyle()<=0){return;}//no walls
  int minX = area.getWallMinX();
  int minZ = area.getWallMinZ();
  int maxX = area.getWallMaxX();
  int maxZ = area.getWallMaxZ();
  int minY = area.getSurfaceY()+1;
  int x, z;
  int facingDirection;
  
  if(template.getWallStyle()>0)//has at least corner sections
    {
    //construct NW corner
    facingDirection = Direction.SOUTH.ordinal();    
    constructTemplate(world, getCornerSection(rng, template), facingDirection, minX, minY, minZ);
    
    //construct NE corner
    facingDirection = Direction.WEST.ordinal();
    constructTemplate(world, getCornerSection(rng, template), facingDirection, maxX, minY, minZ);
    
    //construct SE corner
    facingDirection = Direction.NORTH.ordinal();
    constructTemplate(world, getCornerSection(rng, template), facingDirection, maxX, minY, maxZ);
    
    //construct SW corner
    facingDirection = Direction.EAST.ordinal();
    constructTemplate(world, getCornerSection(rng, template), facingDirection, minX, minY, maxZ);
    }
  
  if(template.getWallStyle()>1)//has wall sections
    {
  //construct N wall
    facingDirection = Direction.SOUTH.ordinal();  
    for(int i = 1; i < area.getChunkWidth()-2; i++)
      {
      x = minX + 16*i;
      z = minZ;
      constructTemplate(world, getWallSection(rng, template, i, area.getChunkWidth()-1), facingDirection, x, minY, z);
      }
    
    //construct E wall
    facingDirection = Direction.WEST.ordinal();
    for(int i = 1; i < area.getChunkLength()-2; i++)
      {
      x = maxX;
      z = minZ + 16*i;
      constructTemplate(world, getWallSection(rng, template, i, area.getChunkLength()-1), facingDirection, x, minY, z);
      }
    
    //construct S wall
    facingDirection = Direction.NORTH.ordinal();
    for(int i = 1; i < area.getChunkWidth()-2; i++)
      {
      x = maxX - 16*i;
      z = maxZ;
      constructTemplate(world, getWallSection(rng, template, i, area.getChunkWidth()-1), facingDirection, x, minY, z);
      }
    
    //construct W wall
    facingDirection = Direction.EAST.ordinal();
    for(int i = 1; i < area.getChunkLength()-2; i++)
      {
      x = minX;
      z = maxZ - 16*i;
      constructTemplate(world, getWallSection(rng, template, i, area.getChunkLength()-1), facingDirection, x, minY, z);
      }    
    }
  }

private static StructureTemplate getWallSection(Random rng, TownTemplate template, int index, int wallLength)
  {
  if(template.getWallStyle()==2)//random weighted
    {    
    int middle = (wallLength / 2);
    if(index==middle)//return a gate piece
      {
      return StructureTemplateManager.instance().getTemplate(template.getRandomWeightedGate(rng));      
      }
    else
      {
      return StructureTemplateManager.instance().getTemplate(template.getRandomWeightedWall(rng));      
      }
    }
  else if(template.getWallStyle()==3)//patterned
    {
    int[] pattern = template.getWallPattern(wallLength);
    if(pattern!=null && wallLength<pattern.length)
      {
      TownWallEntry entry = template.getWall(template.getWallPattern(wallLength)[index]);
      if(entry!=null)
        {
        return StructureTemplateManager.instance().getTemplate(entry.templateName);
        }        
      }  
    }
  return null;
  }

private static StructureTemplate getCornerSection(Random rng, TownTemplate template)
  {
  return StructureTemplateManager.instance().getTemplate(template.getRandomWeightedCorner(rng));
  }

private static void constructTemplate(World world, StructureTemplate template, int face, int x, int y, int z)
  {
  if(template==null){return;}
  new StructureBuilder(world, template, face, x, y, z).instantConstruction();
  }

}
