package org.fatecrafters.plugins.listeners;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.fatecrafters.plugins.RealisticBackpacks;

public class CraftListener implements Listener {

	private final RealisticBackpacks plugin;

	public CraftListener(final RealisticBackpacks plugin) {
		this.plugin = plugin;
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onPrepareCraft(final PrepareItemCraftEvent e) {
		final ItemStack result = e.getInventory().getResult();
		if (result.hasItemMeta()) {
			for (final String backpack : plugin.backpacks) {
				final List<String> key = plugin.backpackData.get(backpack);
				if (!result.hasItemMeta()) {
					continue;
				}
				if (!result.getItemMeta().getDisplayName().equals(ChatColor.translateAlternateColorCodes('&', key.get(3)))) {
					continue;
				}
				final HumanEntity human = e.getView().getPlayer();
				if (!(human instanceof Player)) {
					continue;
				}
				if (!human.hasPermission("rb." + backpack + ".craft")) {
					e.getInventory().setResult(null);
					((Player) human).sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.messageData.get("craftPermError")));
				}
			}
		}
	}

}
