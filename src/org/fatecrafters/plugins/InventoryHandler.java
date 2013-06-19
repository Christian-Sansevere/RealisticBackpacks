package org.fatecrafters.plugins;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.math.BigInteger;

import net.minecraft.server.v1_5_R3.NBTBase;
import net.minecraft.server.v1_5_R3.NBTTagCompound;
import net.minecraft.server.v1_5_R3.NBTTagList;

import org.bukkit.ChatColor;
import org.bukkit.craftbukkit.v1_5_R3.inventory.CraftInventoryCustom;
import org.bukkit.craftbukkit.v1_5_R3.inventory.CraftItemStack;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class InventoryHandler {
	
	// Pulled from resources section, minor edits made

	public static String inventoryToString(Inventory inventory) {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		DataOutputStream dataOutput = new DataOutputStream(outputStream);
		NBTTagList itemList = new NBTTagList();
		// Save every element in the list
		for (int i = 0; i < inventory.getSize(); i++) {
			NBTTagCompound outputObject = new NBTTagCompound();
			net.minecraft.server.v1_5_R3.ItemStack craft = getCraftVersion(inventory.getItem(i));
			// Convert the item stack to a NBT compound
			if (craft != null)
				craft.save(outputObject);
			itemList.add(outputObject);
		}
		// Now save the list
		NBTBase.a(itemList, dataOutput);

		// Serialize that array
		return new BigInteger(1, outputStream.toByteArray()).toString(32);
		// return encodeBase64(outputStream.toByteArray());
	}

	public static Inventory stringToInventory(String data, String name)	{
		ByteArrayInputStream inputStream = new ByteArrayInputStream(new BigInteger(data, 32).toByteArray());
		// ByteArrayInputStream inputStream = new
		// ByteArrayInputStream(decodeBase64(data));
		NBTTagList itemList = (NBTTagList) NBTBase.b(new DataInputStream(inputStream));
		Inventory inventory = new CraftInventoryCustom(null, itemList.size(), ChatColor.translateAlternateColorCodes('&', name));

		for (int i = 0; i < itemList.size(); i++) {
			NBTTagCompound inputObject = (NBTTagCompound) itemList.get(i);
			// IsEmpty
			if (!inputObject.isEmpty()) {
				inventory.setItem(i, CraftItemStack.asBukkitCopy(net.minecraft.server.v1_5_R3.ItemStack.createStack(inputObject)));
			}
		}
		// Serialize that array
		return inventory;
	}

	private static net.minecraft.server.v1_5_R3.ItemStack getCraftVersion(ItemStack stack) {
		if (stack != null)
			return CraftItemStack.asNMSCopy(stack);

		return null;
	}
}
