package com.builtbroken.mffs.content.field;

import com.builtbroken.mffs.MFFS;
import com.builtbroken.mffs.api.IForceFieldBlock;
import com.builtbroken.mffs.api.IProjector;
import com.builtbroken.mffs.api.fortron.IFortronStorage;
import com.builtbroken.mffs.api.modules.IFieldModule;
import com.builtbroken.mffs.api.security.IBiometricIdentifier;
import com.builtbroken.mffs.api.security.Permission;
import com.builtbroken.mffs.api.vector.Vector3D;
import com.builtbroken.mffs.common.items.modules.projector.ItemModuleGlow;
import com.builtbroken.mffs.common.items.modules.projector.ItemModuleShock;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.List;
import java.util.Random;

/**
 * Created by pwaln on 6/24/2016.
 */
public class BlockForceField extends Block implements ITileEntityProvider, IForceFieldBlock //TODO add waila support to fix camo
{
    /**
     * Force Field Block to reference!
     */
    public static BlockForceField BLOCK_FORCE_FIELD;
    public static HashMap<Item, Integer> errorCount = new HashMap();

    /**
     * Default method.
     */
    public BlockForceField()
    {
        super(Material.glass);
        setResistance(999.0F);
        setBlockUnbreakable();
        setCreativeTab(null);
        setTickRandomly(true);
    }

    @Override
    public int tickRate(World p_149738_1_)
    {
        return 10;
    }

    @Override
    public void onBlockAdded(World world, int x, int y, int z)
    {
        if (!world.isRemote)
        {
            world.scheduleBlockUpdate(x, y, z, this, (x + y + z) % 10);
        }
    }

    @Override
    public void updateTick(World world, int x, int y, int z, Random random)
    {
        if (!world.isRemote)
        {
            //Schedule next tick, we want blocks to not all update at the same time
            world.scheduleBlockUpdate(x, y, z, this, (x + y + z) % 10);

            TileEntity tile = world.getTileEntity(x, y, z);
            if (tile instanceof TileForceField && ((TileForceField) tile).shouldDestroy())
            {
                world.setBlockToAir(x, y, z);
            }
        }
    }


    @Override
    public IProjector getProjector(IBlockAccess access, int x, int y, int z)
    {
        TileEntity tile = access.getTileEntity(x, y, z);
        if (tile instanceof TileForceField)
        {
            return ((TileForceField) tile).getProjector();
        }
        return null;
    }

    @Override
    public void weakenForceField(World world, int x, int y, int z, int joules)
    {
        IProjector proj = getProjector(world, x, y, z);
        if (proj != null)
        {
            ((IFortronStorage) proj).provideFortron(joules, true); //TODO scale power
        }

        if (!world.isRemote) //TODO ask field if should break
        {
            world.setBlockToAir(x, y, z);
        }
    }

    /**
     * Is this block (a) opaque and (b) a full 1m cube?  This determines whether or not to render the shared face of two
     * adjacent blocks and also whether the player can attach torches, redstone wire, etc to this block.
     */
    @Override
    public boolean isOpaqueCube()
    {
        return false;
    }

    /**
     * If this block doesn't render as an ordinary block it will return False (examples: signs, buttons, stairs, etc)
     */
    @Override
    public boolean renderAsNormalBlock()
    {
        return false;
    }

    /**
     * Return true if a player with Silk Touch can harvest this block directly, and not its normal drops.
     */
    @Override
    protected boolean canSilkHarvest()
    {
        return false;
    }

    /**
     * Returns the quantity of items to drop on block destruction.
     *
     * @param p_149745_1_
     */
    @Override
    public int quantityDropped(Random p_149745_1_)
    {
        return 0;
    }

    @SideOnly(Side.CLIENT)
    public IIcon getIcon(IBlockAccess access, int x, int y, int z, int side)
    {
        TileEntity tile = access.getTileEntity(x, y, z);
        if (tile instanceof TileForceField)
        {
            ItemStack camo = ((TileForceField) tile).camouflageMaterial;
            if (camo != null && camo.getItem() instanceof ItemBlock)
            {
                ItemBlock block = (ItemBlock) camo.getItem();
                return block.field_150939_a.getIcon(side, camo.getItemDamage());
            }
        }
        return this.getIcon(side, access.getBlockMetadata(x, y, z));
    }

    /**
     * Returns a integer with hex for 0xrrggbb with this color multiplied against the blocks color. Note only called
     * when first determining what to render.
     *
     * @param world
     * @param x
     * @param y
     * @param z
     */
    @Override
    public int colorMultiplier(IBlockAccess world, int x, int y, int z)
    {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof TileForceField)
        {
            ItemStack stack = ((TileForceField) tile).camouflageMaterial;
            if (stack != null && stack.getItem() instanceof ItemBlock)
            {
                return ((ItemBlock) stack.getItem()).field_150939_a.colorMultiplier(world, x, y, z);
            }
        }
        return super.colorMultiplier(world, x, y, z);
    }

    /**
     * Called when a player hits the block. Args: world, x, y, z, player
     *
     * @param world
     * @param x
     * @param y
     * @param z
     * @param entity
     */
    @Override
    public void onBlockClicked(World world, int x, int y, int z, EntityPlayer entity)
    {
        IProjector proj = getProjector(world, x, y, z);
        if (proj != null)
        {
            for (ItemStack stack : proj.getModuleStacks(proj.getModuleSlots()))
            {
                if (((IFieldModule) stack.getItem()).onCollideWithForcefield(world, x, y, z, entity, stack))
                {
                    return;
                }
            }
        }
    }

    /**
     * Triggered whenever an entity collides with this block (enters into the block). Args: world, x, y, z, entity
     *
     * @param world
     * @param x
     * @param y
     * @param z
     * @param entity
     */
    @Override
    public void onEntityCollidedWithBlock(World world, int x, int y, int z, Entity entity)
    {
        if (world.isRemote)
        {
            return;
        }

        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof TileForceField)
        {
            IProjector proj = getProjector(world, x, y, z);
            if (proj == null)
            {
                return;
            }

            for (ItemStack module : proj.getModuleStacks(proj.getModuleSlots()))
            {
                if (((IFieldModule) module.getItem()).onCollideWithForcefield(world, x, y, z, entity, module))
                {
                    return;
                }
            }

            if (Vector3D.distance(tile, entity.posX + .4, entity.posY + .4, entity.posZ + .4) >= .5)
            {
                return;
            }

            IBiometricIdentifier bio = proj.getBiometricIdentifier();
            if (entity instanceof EntityLiving)
            {
                ((EntityLiving) entity).addPotionEffect(new PotionEffect(Potion.confusion.getId(), 80, 3));
                ((EntityLiving) entity).addPotionEffect(new PotionEffect(Potion.moveSlowdown.getId(), 20, 1));
                if (entity instanceof EntityPlayer)
                {
                    EntityPlayer pl = (EntityPlayer) entity;
                    if (pl.isSneaking())
                    {
                        if (pl.capabilities.isCreativeMode)
                        {
                            return;
                        }

                        if (bio != null && bio.isAccessGranted(pl.getGameProfile().getName(), Permission.WARP))
                        {
                            return;
                        }
                    }
                    entity.attackEntityFrom(ItemModuleShock.SHOCK_SOURCE, 100);
                }
            }
        }
    }

    /**
     * Returns a bounding box from the pool of bounding boxes (this means this box can change after the pool has been
     * cleared to be reused)
     *
     * @param world
     * @param x
     * @param y
     * @param z
     */
    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) //TODO change to not need sneaking
    {
        IProjector proj = getProjector(world, x, y, z);
        if (proj != null)
        {
            IBiometricIdentifier bio = proj.getBiometricIdentifier();
            List<EntityPlayer> entities = world.getEntitiesWithinAABB(EntityPlayer.class, AxisAlignedBB.getBoundingBox(x, y, z, x + 1, y + 0.9D, z + 1));
            for (EntityPlayer pl : entities)
            {
                if (pl == null)
                {
                    continue;
                }

                if (pl.isSneaking() &&
                        (pl.capabilities.isCreativeMode || bio != null && bio.isAccessGranted(pl.getGameProfile().getName(), Permission.WARP)))
                {
                    return null;
                }
            }
        }
        return AxisAlignedBB.getBoundingBox(x + 0.0625, y + 0.0625, z + .0625, x + .9375, y + .9375, z + .9375);
    }

    /**
     * Get a light value for the block at the specified coordinates, normal ranges are between 0 and 15
     *
     * @param world The current world
     * @param x     X Position
     * @param y     Y position
     * @param z     Z position
     * @return The light value
     */
    @Override
    public int getLightValue(IBlockAccess world, int x, int y, int z)
    {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof TileForceField)
        {
            IProjector proj = ((TileForceField) tile).getProjector();
            if (proj != null)
            {
                return Math.min(proj.getModuleCount(ItemModuleGlow.class), 64) / 64 * 15;
            }
        }
        return 0;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean shouldSideBeRendered(IBlockAccess world, int x, int y, int z, int p_149646_5_)
    {
        ItemStack stack = null;
        try
        {
            TileEntity tile = world.getTileEntity(x, y, z);
            if (tile instanceof TileForceField)
            {
                stack = ((TileForceField) tile).camouflageMaterial;
                return stack != null && ((ItemBlock) stack.getItem()).field_150939_a.shouldSideBeRendered(world, x, y, z, p_149646_5_);
            }
        }
        catch (Exception e)
        {
            //Entry
            Item item = stack != null ? stack.getItem() : null;

            //If over 5 errors, suppress additional errors
            if (errorCount.get(item) < 5)
            {
                //Error message
                String msg = "BlockForceField#shouldSideBeRendered(%s, %d, %d, %d, %d) - Unexpected error while handling call. ItemStack = %s";
                msg = String.format(msg, world, x, y, z, p_149646_5_, stack);
                MFFS.INSTANCE.logger().error(msg, e);

                //Track errors per item
                if (!errorCount.containsKey(item))
                {
                    errorCount.put(item, 1);
                }
                else
                {
                    errorCount.put(item, errorCount.get(item) + 1);
                }
            }
        }
        return super.shouldSideBeRendered(world, x, y, z, p_149646_5_);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getRenderType()
    {
        return 0;//RenderForceFieldHandler.RENDER_ID;
    }

    @Override
    public void registerBlockIcons(IIconRegister reg)
    {
        this.blockIcon = reg.registerIcon(MFFS.DOMAIN + ":forceField");
    }

    /**
     * Returns which pass should this block be rendered on. 0 for solids and 1 for alpha
     */
    @Override
    public int getRenderBlockPass()
    {
        return 1;
    }

    /**
     * Returns a new instance of a block's tile entity class. Called on placing the block.
     *
     * @param p_149915_1_
     * @param p_149915_2_
     */
    @Override
    public TileEntity createNewTileEntity(World p_149915_1_, int p_149915_2_)
    {
        return new TileForceField();
    }
}
