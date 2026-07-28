package zmaster587.libVulpes.items;

import io.netty.buffer.ByteBuf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import com.mojang.realmsclient.gui.ChatFormatting;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.registry.LanguageRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import zmaster587.libVulpes.LibVulpes;
import zmaster587.libVulpes.api.LibVulpesBlocks;
import zmaster587.libVulpes.block.BlockMeta;
import zmaster587.libVulpes.block.BlockTile;
import zmaster587.libVulpes.block.multiblock.BlockMultiblockMachine;
import zmaster587.libVulpes.inventory.GuiHandler;
import zmaster587.libVulpes.inventory.TextureResources;
import zmaster587.libVulpes.inventory.modules.IButtonInventory;
import zmaster587.libVulpes.inventory.modules.IModularInventory;
import zmaster587.libVulpes.inventory.modules.ModuleBase;
import zmaster587.libVulpes.inventory.modules.ModuleButton;
import zmaster587.libVulpes.inventory.modules.ModuleContainerPan;
import zmaster587.libVulpes.network.INetworkItem;
import zmaster587.libVulpes.network.PacketHandler;
import zmaster587.libVulpes.network.PacketItemModifcation;
import zmaster587.libVulpes.tile.TileSchematic;
import zmaster587.libVulpes.tile.multiblock.DummyTileMultiBlock;
import zmaster587.libVulpes.tile.multiblock.TileMultiBlock;
import zmaster587.libVulpes.tile.multiblock.TilePlaceholder;
import zmaster587.libVulpes.util.BlockPosition;
import zmaster587.libVulpes.util.Vector3F;
import zmaster587.libVulpes.util.ZUtils;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.common.util.ForgeDirection;

public class ItemProjector extends Item implements IModularInventory, IButtonInventory, INetworkItem {

	final ArrayList<TileMultiBlock> machineList;
	final ArrayList<BlockTile> blockList;
	final ArrayList<String> descriptionList;
	private static final String IDNAME = "machineId";
	private static final double MAX_PROJECTION_DISTANCE_SQ = 64D * 64D;
	private static final int WORLD_COORDINATE_LIMIT = 30000000;

	public ItemProjector() {
		machineList = new ArrayList<TileMultiBlock>();
		blockList = new ArrayList<BlockTile>();
		descriptionList = new ArrayList<String>();
	}

	public void registerMachine(TileMultiBlock multiblock, BlockTile mainBlock) {
		if(multiblock == null || mainBlock == null || !isUsableStructure(multiblock.getStructure())) {
			LibVulpes.logger.warning("Ignoring invalid projector machine registration");
			return;
		}
		if(machineList.contains(multiblock))
			return;

		machineList.add(multiblock);
		blockList.add(mainBlock);
		descriptionList.add(buildDescription(multiblock, mainBlock));
	}

	public void registerDummy(DummyTileMultiBlock multiblock) {
		if(multiblock == null || !isUsableStructure(multiblock.getStructure())) {
			LibVulpes.logger.warning("Ignoring invalid dummy projector registration");
			return;
		}
		if(machineList.contains(multiblock))
			return;

		machineList.add(multiblock);
		blockList.add(null);
		descriptionList.add(buildDescription(multiblock, null));
	}

	private String buildDescription(TileMultiBlock multiblock, BlockTile mainBlock) {
		HashMap<String, Integer> requirements = new HashMap<String, Integer>();
		Object[][][] structure = multiblock.getStructure();

		for(Object[][] layer : structure) {
			for(Object[] row : layer) {
				for(Object value : row) {
					if(value instanceof Character && (Character)value == 'c')
						continue;
					String description = describeBlocks(getUsableBlocks(multiblock, value));
					if(description.length() == 0)
						continue;
					Integer count = requirements.get(description);
					requirements.put(description, count == null ? 1 : count + 1);
				}
			}
		}

		StringBuilder description = new StringBuilder();
		if(mainBlock != null) {
			Item item = Item.getItemFromBlock(mainBlock);
			if(item != null)
				description.append(item.getItemStackDisplayName(new ItemStack(mainBlock))).append(" x1\n");
		}

		for(Entry<String, Integer> entry : requirements.entrySet())
			description.append(entry.getKey()).append(" x").append(entry.getValue()).append("\n");
		return description.toString();
	}

	private String describeBlocks(List<BlockMeta> blocks) {
		StringBuilder description = new StringBuilder();
		for(BlockMeta blockMeta : blocks) {
			String blockName = blockMeta.overrideName;
			if(blockName == null || blockName.length() == 0) {
				Item item = Item.getItemFromBlock(blockMeta.getBlock());
				if(item == null)
					continue;
				int metadata = blockMeta.getMeta() == BlockMeta.WILDCARD ? 0 : blockMeta.getMeta();
				blockName = item.getItemStackDisplayName(new ItemStack(blockMeta.getBlock(), 1, metadata));
			}
			if(blockName != null && blockName.length() > 0 && !blockName.contains("tile.")) {
				if(description.length() > 0)
					description.append(" or ");
				description.append(blockName);
			}
		}
		return description.toString();
	}

	private List<BlockMeta> getUsableBlocks(TileMultiBlock multiblock, Object structureEntry) {
		List<BlockMeta> usableBlocks = new ArrayList<BlockMeta>();
		List<BlockMeta> allowableBlocks = multiblock.getAllowableBlocks(structureEntry);
		if(allowableBlocks == null)
			return usableBlocks;

		for(BlockMeta blockMeta : allowableBlocks) {
			if(blockMeta != null && blockMeta.getBlock() != null)
				usableBlocks.add(blockMeta);
		}
		return usableBlocks;
	}

	private boolean isUsableStructure(Object[][][] structure) {
		if(structure == null || structure.length == 0)
			return false;
		for(Object[][] layer : structure) {
			if(layer == null || layer.length == 0)
				return false;
			for(Object[] row : layer) {
				if(row == null || row.length == 0)
					return false;
			}
		}
		return true;
	}

	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void mouseEvent(MouseEvent event) {
		EntityPlayer player = Minecraft.getMinecraft().thePlayer;
		if(player != null && player.isSneaking() && event.dwheel != 0) {
			ItemStack stack = player.getHeldItem();
			int machineId = getMachineId(stack);

			if(stack != null && stack.getItem() == this && isValidMachineId(machineId)
					&& machineList.get(machineId).isVisibleInProjector()) {
				if(event.dwheel < 0) {
					setYLevel(stack, getYLevel(stack) + 1);
				}
				else
					setYLevel(stack, getYLevel(stack) - 1);
				event.setCanceled(true);

				PacketHandler.sendToServer(new PacketItemModifcation(this, Minecraft.getMinecraft().thePlayer, (byte)1));
			}
		}
	}

	private void clearStructure(World world, ItemStack stack) {
		int prevMachineId = getPrevMachineId(stack);
		int directionId = getDirection(stack);
		Vector3F<Integer> basepos = getBasePosition(stack);
		if(world == null || prevMachineId < 0 || prevMachineId >= machineList.size()
				|| !isValidDirection(directionId) || basepos == null)
			return;

		TileMultiBlock previousMachine = machineList.get(prevMachineId);
		if(previousMachine == null || !isUsableStructure(previousMachine.getStructure()))
			return;

		ForgeDirection direction = ForgeDirection.getOrientation(directionId);
		Object[][][] structure = previousMachine.getStructure();
		for(int y = 0; y < structure.length; y++) {
			for(int z = 0; z < structure[y].length; z++) {
				for(int x = 0; x < structure[y][z].length; x++) {
					int globalX = basepos.x - x*direction.offsetZ + z*direction.offsetX;
					int globalZ = basepos.z + x*direction.offsetX + z*direction.offsetZ;
					int globalY = -y + structure.length + basepos.y - 1;

					if(!isValidBlockPosition(world, globalX, globalY, globalZ))
						continue;
					if(world.getBlock(globalX, globalY, globalZ) == LibVulpesBlocks.blockPhantom) {
						world.setBlockToAir(globalX, globalY, globalZ);
						world.removeTileEntity(globalX, globalY, globalZ);
					}
				}
			}
		}
	}

	private void RebuildStructure(World world, ItemStack stack, int posX, int posY, int posZ, ForgeDirection orientation) {
		int id = getMachineId(stack);
		if(world == null || stack == null || id < 0 || id >= machineList.size()
				|| orientation == null || !isValidDirection(orientation.ordinal()))
			return;

		TileMultiBlock multiblock = machineList.get(id);
		if(multiblock == null || !isUsableStructure(multiblock.getStructure()))
			return;

		clearStructure(world, stack);
		Object[][][] structure = multiblock.getStructure();
		int y = getYLevel(stack);
		if(y < -1 || y >= structure.length)
			y = -1;

		int endNumber;
		int startNumber;
		if(y == -1) {
			startNumber = 0;
			endNumber = structure.length;
		}
		else {
			startNumber = y;
			endNumber = y + 1;
		}
		for(y=startNumber; y < endNumber; y++) {
			for(int z = 0; z < structure[y].length; z++) {
				for(int x = 0; x < structure[y][z].length; x++) {
					List<BlockMeta> blocks;
					if(structure[y][z][x] instanceof Character && (Character)structure[y][z][x] == 'c') {
						BlockTile mainBlock = id < blockList.size() ? blockList.get(id) : null;
						if(mainBlock == null)
							continue;
						blocks = new ArrayList<BlockMeta>();
						blocks.add(new BlockMeta(mainBlock, orientation.ordinal()));
					}
					else
						blocks = getUsableBlocks(multiblock, structure[y][z][x]);

					blocks = getProjectionBlocks(blocks);
					if(blocks.isEmpty())
						continue;

					BlockMeta selectedBlock = blocks.get(0);
					int globalX = posX - x*orientation.offsetZ + z*orientation.offsetX;
					int globalZ = posZ + x*orientation.offsetX + z*orientation.offsetZ;
					int globalY = -y + structure.length + posY - 1;

					if(!isValidBlockPosition(world, globalX, globalY, globalZ))
						continue;
					if(world.isAirBlock(globalX, globalY, globalZ)
							|| world.getBlock(globalX, globalY, globalZ).isReplaceable(world, globalX, globalY, globalZ)) {
						if(!world.setBlock(globalX, globalY, globalZ, LibVulpesBlocks.blockPhantom, selectedBlock.getMeta(), 3))
							continue;
						TileEntity newTile = world.getTileEntity(globalX, globalY, globalZ);

						if(newTile instanceof TileSchematic)
							((TileSchematic)newTile).setReplacedBlock(blocks);
						if(newTile instanceof TilePlaceholder)
							((TilePlaceholder)newTile).setReplacedTileEntity(
									selectedBlock.getBlock().createTileEntity(world, selectedBlock.getMeta()));
					}
				}
			}
		}
		this.setPrevMachineId(stack, id);
		this.setBasePosition(stack, posX, posY, posZ);
		this.setDirection(stack, orientation.ordinal());
	}

	private List<BlockMeta> getProjectionBlocks(List<BlockMeta> blocks) {
		List<BlockMeta> projectedBlocks = new ArrayList<BlockMeta>();
		for(BlockMeta blockMeta : blocks) {
			if(blockMeta == null || blockMeta.getBlock() == null
					|| blockMeta.getBlock().getMaterial() == Material.air)
				continue;
			int metadata = blockMeta.getMeta() == BlockMeta.WILDCARD ? 0 : blockMeta.getMeta();
			metadata = Math.max(0, Math.min(15, metadata));
			projectedBlocks.add(new BlockMeta(blockMeta.getBlock(), metadata, blockMeta.overrideName));
		}
		return projectedBlocks;
	}

	private boolean isValidDirection(int direction) {
		return direction >= ForgeDirection.NORTH.ordinal()
				&& direction <= ForgeDirection.EAST.ordinal();
	}

	private boolean isValidBlockPosition(World world, int x, int y, int z) {
		return world != null
				&& y >= 0
				&& y < world.getHeight()
				&& x > -WORLD_COORDINATE_LIMIT
				&& x < WORLD_COORDINATE_LIMIT
				&& z > -WORLD_COORDINATE_LIMIT
				&& z < WORLD_COORDINATE_LIMIT;
	}

	private boolean isProjectionOriginNearPlayer(
			EntityPlayer player, int x, int y, int z) {
		if(player == null || !isValidBlockPosition(player.worldObj, x, y, z))
			return false;
		double deltaX = x + 0.5D - player.posX;
		double deltaY = y + 0.5D - player.posY;
		double deltaZ = z + 0.5D - player.posZ;
		return deltaX*deltaX + deltaY*deltaY + deltaZ*deltaZ
				<= MAX_PROJECTION_DISTANCE_SQ;
	}

	private boolean isValidMachineId(int machineId) {
		return machineId >= 0 && machineId < machineList.size()
				&& machineList.get(machineId) != null
				&& isUsableStructure(machineList.get(machineId).getStructure());
	}

	@Override
	public ItemStack onItemRightClick(ItemStack stack, World world,
			EntityPlayer player) {

		if(!world.isRemote && player.isSneaking()) {
			player.openGui(LibVulpes.instance, GuiHandler.guiId.MODULARNOINV.ordinal(), world, -1, -1, 0);
			return super.onItemRightClick(stack, world, player);
		}

		int id = getMachineId(stack);
		if(!player.isSneaking() && isValidMachineId(id)
				&& machineList.get(id).isVisibleInProjector() && world.isRemote) {
			ForgeDirection dir = ForgeDirection.getOrientation(ZUtils.getDirectionFacing(player.rotationYaw - 180));
			TileMultiBlock tile = machineList.get(id);
			if(tile == null || !isUsableStructure(tile.getStructure()))
				return super.onItemRightClick(stack, world, player);

			int x = tile.getStructure()[0][0].length;
			int z = tile.getStructure()[0].length;

			int globalX = (-x*dir.offsetZ + z*dir.offsetX)/2;
			int globalZ = ((x* dir.offsetX)  + (z*dir.offsetZ))/2;

			MovingObjectPosition pos = Minecraft.getMinecraft().objectMouseOver;
			if(pos == null || pos.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK)
				return super.onItemRightClick(stack, world, player);

			TileEntity tile2;
			if((tile2 = world.getTileEntity(pos.blockX, pos.blockY, pos.blockZ)) instanceof TileMultiBlock) {
				for(TileMultiBlock tiles:  machineList) {
					if(tiles != null && tiles.isVisibleInProjector()
							&& tile2.getClass() == tiles.getClass()
							&& isUsableStructure(tiles.getStructure())) {

						setMachineId(stack, machineList.indexOf(tiles));
						Object[][][] structure = tiles.getStructure();

						BlockPosition controller = getControllerOffset(structure);
						dir = BlockMultiblockMachine.getFront(tile2.getBlockMetadata()).getOpposite();

						int yOffset = 0;
						if(controller != null) {
							controller.y = (short)(structure.length - controller.y);
							yOffset = controller.y;
							globalX = -controller.x*dir.offsetZ + controller.z*dir.offsetX;
							globalZ = controller.x*dir.offsetX + controller.z*dir.offsetZ;
						}
						else {
							int matchedX = structure[0][0].length;
							int matchedZ = structure[0].length;
							globalX = (-matchedX*dir.offsetZ + matchedZ*dir.offsetX)/2;
							globalZ = (matchedX*dir.offsetX + matchedZ*dir.offsetZ)/2;
						}

						setDirection(stack, dir.ordinal());

						setBasePosition(stack, pos.blockX - globalX, pos.blockY - yOffset + 1, pos.blockZ - globalZ);
						PacketHandler.sendToServer(new PacketItemModifcation(this, player, (byte)0));
						PacketHandler.sendToServer(new PacketItemModifcation(this, player, (byte)2));
						return super.onItemRightClick(stack, world, player);
					}
				}
			}

			if(pos.sideHit == 0)
				setBasePosition(stack, pos.blockX - globalX, pos.blockY- tile.getStructure().length, pos.blockZ - globalZ);
			else
				setBasePosition(stack, pos.blockX - globalX, pos.blockY+1, pos.blockZ - globalZ);
			setDirection(stack, dir.ordinal());

			PacketHandler.sendToServer(new PacketItemModifcation(this, player, (byte)2));
		}

		return super.onItemRightClick(stack, world, player);
	}

	protected BlockPosition getControllerOffset(Object[][][] structure) {
		if(!isUsableStructure(structure))
			return null;
		for(int y = 0; y < structure.length; y++) {
			for(int z = 0; z < structure[y].length; z++) {
				for(int x = 0; x < structure[y][z].length; x++) {
					if(structure[y][z][x] instanceof Character && (Character)structure[y][z][x] == 'c')
						return new BlockPosition(x, y, z);
				}
			}
		}
		return null;
	}

	@Override
	public List<ModuleBase> getModules(int ID, EntityPlayer player) {
		List<ModuleBase> modules = new LinkedList<ModuleBase>();
		List<ModuleBase> btns = new LinkedList<ModuleBase>();

		int visibleIndex = 0;
		for(int machineId = 0; machineId < machineList.size(); machineId++) {
			TileMultiBlock multiblock = machineList.get(machineId);
			if(multiblock == null || !multiblock.isVisibleInProjector())
				continue;
			btns.add(new ModuleButton(20 + visibleIndex % 2 * 100,
					4 + visibleIndex / 2 * 24,
					machineId,
					LibVulpes.proxy.getLocalizedString(multiblock.getMachineName()),
					this,
					TextureResources.buttonBuild));
			visibleIndex++;
		}

		ModuleContainerPan panningContainer = new ModuleContainerPan(5, 20, btns, new LinkedList<ModuleBase>(), TextureResources.starryBG, 160, 100, 0, 500);
		modules.add(panningContainer);
		return modules;
	}

	@Override
	public String getModularInventoryName() {
		return "item.holoProjector.name";
	}

	@Override
	public boolean canInteractWithContainer(EntityPlayer entity) {
		return entity != null && !entity.isDead && entity.getHeldItem() != null && entity.getHeldItem().getItem() == this;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void onInventoryButtonPressed(int buttonId) {
		//PacketHandler.sendToServer(new PacketItemModifcation(this, Minecraft.getMinecraft().thePlayer, (byte)buttonId));
		ItemStack stack = Minecraft.getMinecraft().thePlayer.getHeldItem();
		if(stack != null && stack.getItem() == this && isValidMachineId(buttonId)
				&& machineList.get(buttonId).isVisibleInProjector()) {
			setMachineId(stack, buttonId);
			PacketHandler.sendToServer(new PacketItemModifcation(this, Minecraft.getMinecraft().thePlayer, (byte)0));
		}
	}

	private void setMachineId(ItemStack stack, int id) {
		NBTTagCompound nbt;
		if(stack.hasTagCompound()) {
			nbt = stack.getTagCompound();
		}
		else 
			nbt = new NBTTagCompound();

		nbt.setInteger(IDNAME, id);
		stack.setTagCompound(nbt);
	}

	private int getMachineId(ItemStack stack) {
		if(stack != null && stack.hasTagCompound() && stack.getTagCompound().hasKey(IDNAME)) {
			return stack.getTagCompound().getInteger(IDNAME);
		}
		else
			return -1;
	}

	private void setYLevel(ItemStack stack, int level) {
		NBTTagCompound nbt;
		if(stack.hasTagCompound()) {
			nbt = stack.getTagCompound();
		}
		else 
			nbt = new NBTTagCompound();

		int machineId = getMachineId(stack);
		if(machineId < 0 || machineId >= machineList.size())
			return;
		TileMultiBlock machine = machineList.get(machineId);
		if(machine == null || !isUsableStructure(machine.getStructure()))
			return;

		if(level <= -2)
			level = machine.getStructure().length-1;
		else if(level >= machine.getStructure().length)
			level = -1;
		nbt.setInteger("yOffset", level);
		stack.setTagCompound(nbt);
	}

	private int getYLevel(ItemStack stack) {
		if(stack != null && stack.hasTagCompound() && stack.getTagCompound().hasKey("yOffset")) {
			return stack.getTagCompound().getInteger("yOffset");
		}
		else
			return -1;
	}

	private void setPrevMachineId(ItemStack stack, int id) {
		NBTTagCompound nbt;
		if(stack.hasTagCompound()) {
			nbt = stack.getTagCompound();
		}
		else 
			nbt = new NBTTagCompound();

		nbt.setInteger(IDNAME + "Prev", id);
		stack.setTagCompound(nbt);
	}

	private int getPrevMachineId(ItemStack stack) {
		if(stack != null && stack.hasTagCompound() && stack.getTagCompound().hasKey(IDNAME + "Prev")) {
			return stack.getTagCompound().getInteger(IDNAME + "Prev");
		}
		else
			return -1;
	}

	private Vector3F<Integer> getBasePosition(ItemStack stack) {
		if(stack != null && stack.hasTagCompound()) {
			NBTTagCompound nbt = stack.getTagCompound();
			if(nbt.hasKey("x") && nbt.hasKey("y") && nbt.hasKey("z"))
				return new Vector3F<Integer>(nbt.getInteger("x"), nbt.getInteger("y"), nbt.getInteger("z"));
		}
		return null;
	}

	private void setBasePosition(ItemStack stack, int x, int y, int z) {
		NBTTagCompound nbt;
		if(stack.hasTagCompound()) {
			nbt = stack.getTagCompound();
		}
		else
			nbt = new NBTTagCompound();

		nbt.setInteger("x", x);
		nbt.setInteger("y", y);
		nbt.setInteger("z", z);

		stack.setTagCompound(nbt);
	}

	public int getDirection(ItemStack stack) {
		if(stack != null && stack.hasTagCompound() && stack.getTagCompound().hasKey("dir")) {
			return stack.getTagCompound().getInteger("dir");
		}
		else
			return -1;
	}

	public void setDirection(ItemStack stack, int dir) {
		NBTTagCompound nbt;
		if(stack.hasTagCompound()) {
			nbt = stack.getTagCompound();
		}
		else
			nbt = new NBTTagCompound();

		nbt.setInteger("dir", dir);

		stack.setTagCompound(nbt);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack, EntityPlayer player,
			List list, boolean bool) {
		super.addInformation(stack, player, list, bool);

		list.add(LanguageRegistry.instance().getStringLocalization("item.Projector.desc.0"));
		list.add(LanguageRegistry.instance().getStringLocalization("item.Projector.desc.1"));
		list.add(LanguageRegistry.instance().getStringLocalization("item.Projector.desc.2"));

		int id = getMachineId(stack);
		if(isValidMachineId(id) && machineList.get(id).isVisibleInProjector()
				&& id < descriptionList.size()) {
			list.add("");
			list.add(ChatFormatting.GREEN + LibVulpes.proxy.getLocalizedString(machineList.get(id).getMachineName()));
			String str = descriptionList.get(id);

			String strList[] = str.split("\n");

			for(String s : strList)
				list.add(s);
		}
	}

	@Override
	public void writeDataToNetwork(ByteBuf out, byte id, ItemStack stack) {
		if(id == 0) {
			out.writeInt(getMachineId(stack));
		}
		else if(id == 1)
			out.writeInt(getYLevel(stack));
		else if(id == 2) {

			Vector3F<Integer> pos = getBasePosition(stack);
			int direction = getDirection(stack);
			boolean hasPosition = pos != null && isValidDirection(direction);
			out.writeBoolean(hasPosition);
			if(!hasPosition)
				return;
			out.writeInt(pos.x);
			out.writeInt(pos.y);
			out.writeInt(pos.z);
			out.writeInt(direction);
		}
	}

	@Override
	public void readDataFromNetwork(ByteBuf in, byte packetId,
			NBTTagCompound nbt, ItemStack stack) {
		if(nbt == null)
			return;
		if(packetId == 0) {
			nbt.setInteger(IDNAME, in.readInt());
		}
		else if(packetId == 1)
			nbt.setInteger("yLevel", in.readInt());
		else if(packetId == 2) {
			boolean hasPosition = in.readBoolean();
			nbt.setBoolean("hasPosition", hasPosition);
			if(!hasPosition)
				return;
			nbt.setInteger("x", in.readInt());
			nbt.setInteger("y", in.readInt());
			nbt.setInteger("z", in.readInt());
			nbt.setInteger("dir", in.readInt());
		}
	}

	@Override
	public void useNetworkData(EntityPlayer player, Side side, byte id,
			NBTTagCompound nbt, ItemStack stack) {
		if(player == null || nbt == null || stack == null)
			return;
		if(id == 0) {
			int machineId = nbt.getInteger(IDNAME);
			if(!isValidMachineId(machineId) || !machineList.get(machineId).isVisibleInProjector())
				return;
			setMachineId(stack, machineId);
			TileMultiBlock tile = machineList.get(machineId);
			setYLevel(stack, tile.getStructure().length-1);
		}
		else if(id == 1) {
			int machineId = getMachineId(stack);
			if(!isValidMachineId(machineId))
				return;
			setYLevel(stack, nbt.getInteger("yLevel"));
			Vector3F<Integer> vec = getBasePosition(stack);
			int direction = getDirection(stack);
			if(vec != null && isValidDirection(direction)
					&& isProjectionOriginNearPlayer(player, vec.x, vec.y, vec.z))
				RebuildStructure(player.worldObj, stack,
						vec.x, vec.y, vec.z, ForgeDirection.getOrientation(direction));
		}
		else if(id == 2) {
			int machineId = getMachineId(stack);
			if(!isValidMachineId(machineId) || !nbt.getBoolean("hasPosition"))
				return;
			int x = nbt.getInteger("x");
			int y = nbt.getInteger("y");
			int z = nbt.getInteger("z");
			int dir = nbt.getInteger("dir");
			if(isValidDirection(dir)
					&& isProjectionOriginNearPlayer(player, x, y, z))
				RebuildStructure(player.worldObj, stack,
						x, y, z, ForgeDirection.getOrientation(dir));
		}
	}
}
