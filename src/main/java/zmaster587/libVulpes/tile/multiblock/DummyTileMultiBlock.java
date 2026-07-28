package zmaster587.libVulpes.tile.multiblock;

import java.util.List;

import zmaster587.libVulpes.api.LibVulpesBlocks;
import zmaster587.libVulpes.block.BlockMeta;

public class DummyTileMultiBlock extends TileMultiBlock {
	public final Object[][][] structure;
	public final String name;

	public DummyTileMultiBlock(Object[][][] structure, String name) {
		this.structure = structure;
		this.name = name == null ? "" : name;
	}

	@Override
	public List<BlockMeta> getAllowableWildCardBlocks(Character wildCard) {
		List<BlockMeta> list = super.getAllowableWildCardBlocks(wildCard);
		if(LibVulpesBlocks.blockHatch != null) {
			list.add(new BlockMeta(LibVulpesBlocks.blockHatch, 0));
			list.add(new BlockMeta(LibVulpesBlocks.blockHatch, 1));
		}
		return list;
	}

	@Override
	public Object[][][] getStructure() {
		return structure;
	}

	@Override
	public String getMachineName() {
		return name;
	}
}
