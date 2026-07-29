package zmaster587.libVulpes.inventory;

import java.util.List;

import zmaster587.libVulpes.inventory.modules.IModularInventory;
import zmaster587.libVulpes.inventory.modules.ModuleBase;
import net.minecraft.entity.player.EntityPlayer;

public class GuiModularFullScreen extends GuiModular {

	public GuiModularFullScreen(EntityPlayer playerInv,
				List<ModuleBase> modules, IModularInventory modularInv,
				boolean includePlayerInv, boolean includeHotBar, String name) {
		super(playerInv, modules, modularInv, includePlayerInv,includeHotBar, name);
	}

	@Override
	public void initGui() {
		// GuiScreen.width/height are already expressed in scaled GUI units.
		// Replacing them with framebuffer pixels breaks buttons and scissoring
		// whenever Minecraft's GUI scale is not exactly two.
		this.xSize = this.width;
		this.ySize = this.height;

		super.initGui();
	}
}
