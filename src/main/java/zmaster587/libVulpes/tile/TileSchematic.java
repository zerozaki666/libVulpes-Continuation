package zmaster587.libVulpes.tile;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import zmaster587.libVulpes.LibVulpes;
import zmaster587.libVulpes.block.BlockMeta;
import zmaster587.libVulpes.tile.multiblock.TilePlaceholder;

import java.util.ArrayList;
import java.util.List;

public class TileSchematic extends TilePlaceholder {

	private static final int TTL = 6000;
	private int timeAlive = 0;
	List<BlockMeta> possibleBlocks;

	public TileSchematic() {
		possibleBlocks = new ArrayList<>();
	}

	@Override
	public boolean canUpdate() {
		return true;
	}

	public void setReplacedBlock(List<BlockMeta> list) {
		possibleBlocks.clear();
		if(list == null)
			return;
		for(BlockMeta block : list) {
			if(block != null && block.getBlock() != null && block.getBlock() != Blocks.air)
				possibleBlocks.add(block);
		}
	}

	@Override
	public void setReplacedBlock(Block block) {
		super.setReplacedBlock(block);
		possibleBlocks.clear();
	}

	@Override
	public void setReplacedBlockMeta(int meta) {
		super.setReplacedBlockMeta(meta);
		possibleBlocks.clear();
	}

	@Override
	public Block getReplacedBlock() {
		if(possibleBlocks.isEmpty())
			return super.getReplacedBlock();
		else {
			return possibleBlocks.get((timeAlive/20) % possibleBlocks.size()).getBlock();
		}
	}

	@Override
	public int getReplacedBlockMeta() {
		if(possibleBlocks.isEmpty())
			return super.getReplacedBlockMeta();
		else
			return possibleBlocks.get((timeAlive/20) % possibleBlocks.size()).getMeta();
	}
	public String getReplacedBlockOverrideName() {
		if(possibleBlocks.isEmpty())
			return "";
		else
			return possibleBlocks.get((timeAlive/20) % possibleBlocks.size()).overrideName;
	}
	@Override
	public void updateEntity() {
		super.updateEntity();

		if(!worldObj.isRemote) {
			if(timeAlive >= TTL) {
				worldObj.setBlockToAir(xCoord, yCoord, zCoord);
			}
		}
		timeAlive++;
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.setInteger("timeAlive", timeAlive);

		int written = 0;
		for(BlockMeta block : possibleBlocks) {
			if(block == null || block.getBlock() == null || block.getBlock() == Blocks.air)
				continue;
			NBTTagCompound blockTag = new NBTTagCompound();
			blockTag.setInteger("id", Block.getIdFromBlock(block.getBlock()));
			blockTag.setInteger("meta", block.getMeta());
			if(block.overrideName != null && !block.overrideName.isEmpty())
				blockTag.setString("name", block.overrideName);
			nbt.setTag("block." + written++, blockTag);
		}
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		timeAlive = nbt.getInteger("timeAlive");
		possibleBlocks.clear();

		if(nbt.hasKey("blockIds")) {
			int[] blockIds = nbt.getIntArray("blockIds");
			int[] blockMetas = nbt.getIntArray("blockMetas");
			for(int i = 0; i < blockIds.length && i < blockMetas.length; i++) {
				Block block = Block.getBlockById(blockIds[i]);
				if(block != null && block != Blocks.air)
					possibleBlocks.add(new BlockMeta(block, normalizeMetadata(blockMetas[i])));
			}
			return;
		}

		for(int i = 0; nbt.hasKey("block." + i); i++) {
			try {
				NBTTagCompound blockTag = nbt.getCompoundTag("block." + i);
				Block target = Block.getBlockById(blockTag.getInteger("id"));
				if(target == null || target == Blocks.air)
					continue;
				String overrideName = blockTag.hasKey("name") ? blockTag.getString("name") : "";
				possibleBlocks.add(new BlockMeta(target, normalizeMetadata(blockTag.getInteger("meta")), overrideName));
			}catch (Exception e){
				LibVulpes.logger.warning("Skipping invalid projector block entry block." + i + ": " + e.getMessage());
			}
		}
	}

	private int normalizeMetadata(int metadata) {
		return Math.max(0, Math.min(15, metadata));
	}

	@Override
	public boolean shouldRenderInPass(int pass) {
		return true;
	}

}
