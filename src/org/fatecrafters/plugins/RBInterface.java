package org.fatecrafters.plugins;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public interface RBInterface {

	public String inventoryToString(Inventory inventory);

	public Inventory stringToInventory(String data, String name);

	public ItemStack addGlow(ItemStack item);

}
