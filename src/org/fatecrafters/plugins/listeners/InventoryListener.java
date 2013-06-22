package org.fatecrafters.plugins.listeners;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.fatecrafters.plugins.RealisticBackpacks;
import org.fatecrafters.plugins.util.MysqlFunctions;

public class InventoryListener implements Listener {

	private final RealisticBackpacks plugin;

	public InventoryListener(final RealisticBackpacks plugin) {
		this.plugin = plugin;
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onInventoryClose(final InventoryCloseEvent e) {
		final String name = e.getPlayer().getName();
		if (plugin.playerData.containsKey(name)) {
			final Inventory inv = e.getView().getTopInventory();
			plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
				@Override
				public void run() {
					if (plugin.isUsingMysql()) {
						try {
							MysqlFunctions.addBackpackData(name, plugin.playerData.get(name), inv);
						} catch (final SQLException e) {
							e.printStackTrace();
						}
					} else {
						final String invString = RealisticBackpacks.inter.inventoryToString(inv);
						final File file = new File(plugin.getDataFolder() + File.separator + "userdata" + File.separator + name + ".yml");
						final FileConfiguration config = YamlConfiguration.loadConfiguration(file);
						config.set(plugin.playerData.get(name) + ".Inventory", invString);
						try {
							config.save(file);
						} catch (final IOException e) {
							e.printStackTrace();
						}
					}
					plugin.playerData.remove(name);
				}
			});
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onInventoryClick(final InventoryClickEvent e) {
		if (e.getWhoClicked() instanceof Player) {
			final Player p = (Player) e.getWhoClicked();
			final ItemStack curItem = e.getCurrentItem();
			final Inventory inv = p.getInventory();
			final Inventory otherInv = e.getView().getTopInventory();
			if (curItem != null && otherInv != null) {
				plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
					@Override
					public void run() {
						if (plugin.slowedPlayers.contains(p.getName())) {
							for (final String backpack : plugin.backpacks) {
								final ItemStack backpackItem = plugin.backpackItems.get(backpack);
								if (!curItem.equals(backpackItem)) {
									continue;
								}
								if (!inv.contains(backpackItem)) {
									plugin.slowedPlayers.remove(p.getName());
									plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
										@Override
										public void run() {
											p.setWalkSpeed(0.2F);
										}
									});
								}
							}
						}
					}
				});
			}
		}
	}

}
