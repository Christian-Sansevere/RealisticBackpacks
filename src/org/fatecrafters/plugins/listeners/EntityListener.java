package org.fatecrafters.plugins.listeners;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.inventory.Inventory;
import org.fatecrafters.plugins.RealisticBackpacks;
import org.fatecrafters.plugins.util.RBUtil;

public class EntityListener implements Listener {

	private final RealisticBackpacks plugin;

	private int setlevel = 0;

	public EntityListener(final RealisticBackpacks plugin) {
		this.plugin = plugin;
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onFoodChange(final FoodLevelChangeEvent e) {
		if (e.getEntity() instanceof Player) {
			final int foodlevel = e.getFoodLevel();
			final Player p = (Player) e.getEntity();
			final int pLevel = p.getFoodLevel();
			final Inventory inv = p.getInventory();
			plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
				@Override
				public void run() {
					final List<String> backpackList = new ArrayList<String>();
					for (final String backpack : plugin.backpacks) {
						final List<String> key = plugin.backpackData.get(backpack);
						if (!key.get(10).equals("true")) {
							continue;
						}
						if (!inv.contains(plugin.backpackItems.get(backpack))) {
							continue;
						}
						backpackList.add(backpack);
					}
					final int listsize = backpackList.size();
					if (listsize > 0) {
						if (pLevel > foodlevel) {
							//Starving
							setlevel = RBUtil.getFoodLevel(foodlevel, pLevel, listsize, 11, backpackList);
							if (setlevel < 0) {
								setlevel = 0;
							}
						} else {
							//Ate food
							setlevel = RBUtil.getFoodLevel(foodlevel, pLevel, listsize, 12, backpackList);
							if (setlevel < pLevel) {
								setlevel = pLevel;
							}
						}
						plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
							@Override
							public void run() {
								e.setCancelled(true);
								p.setFoodLevel(setlevel);
							}
						});
					}
				}
			});
		}
	}

}
