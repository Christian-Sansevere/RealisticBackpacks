package org.fatecrafters.plugins.versions;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.math.BigInteger;

import net.minecraft.server.NBTBase;
import net.minecraft.server.NBTTagCompound;
import net.minecraft.server.NBTTagList;

import org.bukkit.craftbukkit.inventory.CraftInventoryCustom;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.inventory.Inventory;
import org.fatecrafters.plugins.RBInterface;
import org.fatecrafters.plugins.RealisticBackpacks;

public class preVersioning implements RBInterface {

	RealisticBackpacks plugin;

	public preVersioning(final RealisticBackpacks rb) {
		this.plugin = rb;
	}

	@Override
	public String inventoryToString(final Inventory inventory) {
		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		final DataOutputStream dataOutput = new DataOutputStream(outputStream);
		final NBTTagList itemList = new NBTTagList();
		for (int i = 0; i < inventory.getSize(); i++) {
			final NBTTagCompound outputObject = new NBTTagCompound();
			CraftItemStack craft = null;
			final org.bukkit.inventory.ItemStack is = inventory.getItem(i);
			if (is instanceof CraftItemStack) {
				craft = (CraftItemStack) is;
			} else if (is != null) {
				craft = new CraftItemStack(is);
			} else {
				craft = null;
			}
			if (craft != null) {
				craft.getHandle().save(outputObject);
			}
			itemList.add(outputObject);
		}
		NBTBase.a(itemList, dataOutput);
		return new BigInteger(1, outputStream.toByteArray()).toString(32);
	}

	@Override
	public Inventory stringToInventory(final String data, final String name) {
		final ByteArrayInputStream inputStream = new ByteArrayInputStream(new BigInteger(data, 32).toByteArray());
		final NBTTagList itemList = (NBTTagList) NBTBase.b(new DataInputStream(inputStream));
		final Inventory inventory = new CraftInventoryCustom(null, itemList.size());

		for (int i = 0; i < itemList.size(); i++) {
			final NBTTagCompound inputObject = (NBTTagCompound) itemList.get(i);
			if (!inputObject.d()) {
				inventory.setItem(i, new CraftItemStack(net.minecraft.server.ItemStack.a(inputObject)));
			}
		}
		return inventory;
	}

}
