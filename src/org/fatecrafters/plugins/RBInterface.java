package org.fatecrafters.plugins;

import org.bukkit.inventory.Inventory;

public interface RBInterface {

	public String inventoryToString(Inventory inventory);

	public Inventory stringToInventory(String data, String name);

}
