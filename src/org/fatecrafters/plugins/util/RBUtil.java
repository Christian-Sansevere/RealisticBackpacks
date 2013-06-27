package org.fatecrafters.plugins.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.fatecrafters.plugins.RealisticBackpacks;

public class RBUtil {

	private static RealisticBackpacks plugin;

	public static void setRBUtil(final RealisticBackpacks plugin) {
		RBUtil.plugin = plugin;
	}

	public static int getFoodLevel(final int foodlevel, final int pLevel, final int listsize, final int key, final List<String> backpackList) {
		int i = 0;
		if (plugin.isAveraging()) {
			int average = 0;
			for (final String backpack : backpackList) {
				average += Integer.parseInt(plugin.backpackData.get(backpack).get(key));
			}
			i = foodlevel - (average / listsize);
		} else if (plugin.isAdding()) {
			int sum = 0;
			for (final String backpack : backpackList) {
				sum += Integer.parseInt(plugin.backpackData.get(backpack).get(key));
			}
			i = foodlevel - sum;
		} else {
			final List<Integer> list = new ArrayList<Integer>();
			for (final String backpack : backpackList) {
				list.add(Integer.parseInt(plugin.backpackData.get(backpack).get(key)));
			}
			i = foodlevel - Collections.max(list);
		}
		return i;
	}

	public static ItemStack getItemstackFromString(final String s) {
		ItemStack item = null;
		final String[] split = s.split(":");
		if (split.length == 1) {
			item = new ItemStack(Material.getMaterial(Integer.parseInt(split[0])));
		} else {
			item = new ItemStack(Material.getMaterial(Integer.parseInt(split[0])), 1, (byte) Integer.parseInt(split[1]));
		}
		return item;
	}

	public static void destroyContents(final String name, final String backpack) {
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
			@Override
			public void run() {
				if (plugin.isUsingMysql()) {
					MysqlFunctions.delete(name, backpack);
				} else {
					final File file = new File(plugin.getDataFolder() + File.separator + "userdata" + File.separator + name + ".yml");
					final FileConfiguration config = YamlConfiguration.loadConfiguration(file);
					config.set(backpack + ".Inventory", null);
					try {
						config.save(file);
					} catch (final IOException e1) {
						e1.printStackTrace();
					}
				}
			}
		});
	}
}
