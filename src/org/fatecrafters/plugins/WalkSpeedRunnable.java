package org.fatecrafters.plugins;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;


public class WalkSpeedRunnable implements Runnable {
	private RealisticBackpacks plugin;

	public WalkSpeedRunnable(RealisticBackpacks plugin) {
		this.plugin = plugin;
	}

	@Override
	public void run() {
		for (Player p : plugin.getServer().getOnlinePlayers()) {
			final String name = p.getName();
			if (plugin.slowedPlayers.contains(name)) {
				return;
			}

			final Inventory inv = p.getInventory();
			final List<String> backpackList = new ArrayList<String>();
			for (final String backpack : plugin.backpacks) {
				final List<String> key = plugin.backpackData.get(backpack);
				if (key.get(8) != null && key.get(8).equalsIgnoreCase("true") && inv != null && inv.contains(plugin.backpackItems.get(backpack))) {
					backpackList.add(backpack);
				}
			}
			final int listsize = backpackList.size();
			if (listsize > 0) {
				float walkSpeedMultiplier = 0.0F;
				if (listsize > 1) {
					if (plugin.isAveraging()) {
						float average = 0;
						for (final String backpack : backpackList) {
							average += Float.parseFloat(plugin.backpackData.get(backpack).get(9));
						}
						walkSpeedMultiplier = average / listsize;
					} else {
						if (plugin.isAdding()) {
							float sum = 0;
							for (final String backpack : backpackList) {
								sum += 0.2F - Float.parseFloat(plugin.backpackData.get(backpack).get(9));
							}
							walkSpeedMultiplier = 0.2F - sum;
						} else {
							final List<Float> floatList = new ArrayList<Float>();
							for (final String backpack : backpackList) {
								floatList.add(Float.parseFloat(plugin.backpackData.get(backpack).get(9)));
							}
							walkSpeedMultiplier = Collections.max(floatList);
						}
					}
				} else {
					if (listsize == 1) {
						walkSpeedMultiplier = Float.parseFloat(plugin.backpackData.get(backpackList.get(0)).get(9));
					}
				}
				plugin.slowedPlayers.add(name);
				p.setWalkSpeed(walkSpeedMultiplier);
			}
		}
	}
}
