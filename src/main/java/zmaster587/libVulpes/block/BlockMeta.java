package zmaster587.libVulpes.block;

import net.minecraft.block.Block;

public class BlockMeta {
	final Block block;
	final int meta;
	public String overrideName = "";
	public static final int WILDCARD = -1;

	public BlockMeta(Block block, int meta) {
		this.block = block;
		this.meta = meta;
	}


	public BlockMeta(Block block, int meta, String overrideName) {
		this.block = block;
		this.meta = meta;
		this.overrideName = overrideName == null ? "" : overrideName;
	}

	public BlockMeta(Block block) {
		this.block = block;
		this.meta = WILDCARD;
	}

	@Override
	public boolean equals(Object obj) {

		if(obj instanceof BlockMeta) {
			return ((BlockMeta)obj).block == block && (meta == WILDCARD || ((BlockMeta)obj).meta == WILDCARD || ((BlockMeta)obj).meta == meta);
		}
		return super.equals(obj);
	}

	public Block getBlock() {
		return block;
	}

	public int getMeta() {
		if(meta != WILDCARD)
			return meta;
		return 0;
	}
}
