package joshie.harvestmoon.blocks;

import static joshie.harvestmoon.helpers.CropHelper.removeFarmland;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFarmland;
import net.minecraft.world.Explosion;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.EnumPlantType;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.common.util.ForgeDirection;
import cpw.mods.fml.common.registry.GameRegistry;

public class BlockSoil extends BlockFarmland {
    public BlockSoil() {
        super();
        setTickRandomly(false);
    }
    
    @Override
    public BlockSoil setBlockName(String name) {
        super.setBlockName(name);
        GameRegistry.registerBlock(this, "soil");
        return this;
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int side) {
        removeFarmland(world, x, y, z);
    }

    @Override
    public void onBlockDestroyedByExplosion(World world, int x, int y, int z, Explosion explosion) {
        removeFarmland(world, x, y, z);
    }

    @Override
    public boolean canSustainPlant(IBlockAccess world, int x, int y, int z, ForgeDirection direction, IPlantable plantable) {
        EnumPlantType type = plantable.getPlantType(world, x, y + 1, z);
        return type == EnumPlantType.Crop || type == EnumPlantType.Plains;
    }

    public static boolean hydrate(World world, int x, int y, int z) {
        int meta = world.getBlockMetadata(x, y, z);
        boolean ret = meta == 7 ? false : world.setBlockMetadataWithNotify(x, y, z, 7, 2);
        if(ret) {
            hydrate(world, x, y + 1, z);
        }
        
        return ret;
    }

    //Returns false if the soil is no longer farmland
    public static boolean dehydrate(World world, int x, int y, int z) {
        Block block = world.getBlock(x, y + 1, z);
        int meta = world.getBlockMetadata(x, y, z);
        if (block instanceof IPlantable && world.getBlock(x, y, z).canSustainPlant(world, x, y, z, ForgeDirection.UP, (IPlantable) block)) {
            world.setBlockMetadataWithNotify(x, y, z, 0, 2);
            return true;
        } else if (meta == 7) {
            world.setBlockMetadataWithNotify(x, y, z, 0, 2);
            return true;
        } else {
            return false;
        }
    }

    public static boolean isHydrated(World world, int x, int y, int z) {
        return world.getBlock(x, y, z) instanceof BlockSoil && world.getBlockMetadata(x, y, z) == 7;
    }
}
