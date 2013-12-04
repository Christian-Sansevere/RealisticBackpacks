package org.fatecrafters.plugins.versions;

import net.minecraft.server.v1_7_R1.NBTTagCompound;
import net.minecraft.server.v1_7_R1.NBTTagList;

import org.bukkit.craftbukkit.v1_7_R1.inventory.CraftItemStack;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.fatecrafters.plugins.RBInterface;
import org.fatecrafters.plugins.RealisticBackpacks;

public class v1_7_R1 implements RBInterface {

	RealisticBackpacks plugin;

	public v1_7_R1(final RealisticBackpacks rb) {
		this.plugin = rb;
	}

	@Override
	public String inventoryToString(final Inventory inventory) {
		return null;
	}

	@Override
	public Inventory stringToInventory(final String data, final String name) {
		return null;
	}

	@Override
	public ItemStack addGlow(ItemStack item) {
		net.minecraft.server.v1_7_R1.ItemStack nmsStack = CraftItemStack.asNMSCopy(item);
		NBTTagCompound tag = null;
		if (!nmsStack.hasTag()) {
			tag = new NBTTagCompound();
			nmsStack.setTag(tag);
		}
		if (tag == null)
			tag = nmsStack.getTag();
		NBTTagList ench = new NBTTagList();
		tag.set("ench", ench);
		nmsStack.setTag(tag);
		return CraftItemStack.asCraftMirror(nmsStack);
	}

}
