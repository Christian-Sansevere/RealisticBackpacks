package org.fatecrafters.plugins.versions;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.math.BigInteger;

import net.minecraft.server.v1_5_R2.NBTBase;
import net.minecraft.server.v1_5_R2.NBTTagCompound;
import net.minecraft.server.v1_5_R2.NBTTagList;

import org.bukkit.craftbukkit.v1_5_R2.inventory.CraftInventoryCustom;
import org.bukkit.craftbukkit.v1_5_R2.inventory.CraftItemStack;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.fatecrafters.plugins.RBInterface;
import org.fatecrafters.plugins.RealisticBackpacks;

public class v1_5_R2 implements RBInterface {

	RealisticBackpacks plugin;

	public v1_5_R2(final RealisticBackpacks rb) {
		this.plugin = rb;
	}

	@Override
	public String inventoryToString(final Inventory inventory) {
		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		final DataOutputStream dataOutput = new DataOutputStream(outputStream);
		final NBTTagList itemList = new NBTTagList();
		for (int i = 0; i < inventory.getSize(); i++) {
			final NBTTagCompound outputObject = new NBTTagCompound();
			net.minecraft.server.v1_5_R2.ItemStack craft = null;
			final org.bukkit.inventory.ItemStack is = inventory.getItem(i);
			if (is != null) {
				craft = CraftItemStack.asNMSCopy(is);
			} else {
				craft = null;
			}
			if (craft != null) {
				craft.save(outputObject);
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
			if (!inputObject.isEmpty()) {
				inventory.setItem(i, CraftItemStack.asBukkitCopy(net.minecraft.server.v1_5_R2.ItemStack.createStack(inputObject)));
			}
		}
		return inventory;
	}

	@Override
	public ItemStack addGlow(ItemStack item) {
		net.minecraft.server.v1_5_R2.ItemStack handle = CraftItemStack.asNMSCopy(item);

		if (handle == null) {
			return item;
		}

		if (handle.tag == null) {
			handle.tag = new NBTTagCompound();
		}

		NBTTagList tag = handle.getEnchantments();
		if (tag == null) {
			tag = new NBTTagList("ench");
			handle.tag.set("ench", tag);
		}

		return CraftItemStack.asCraftMirror(handle);
	}

}
