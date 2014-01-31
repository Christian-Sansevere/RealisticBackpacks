package org.fatecrafters.plugins.listeners;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import org.bukkit.ChatColor;
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
import org.fatecrafters.plugins.util.RBUtil;
import org.fatecrafters.plugins.util.Serialization;

public class InventoryListener implements Listener {

	private final RealisticBackpacks plugin;

	public InventoryListener(final RealisticBackpacks plugin) {
		this.plugin = plugin;
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onInventoryClose(final InventoryCloseEvent e) {
		final String name = e.getPlayer().getName();
		final Inventory inv = e.getView().getTopInventory();
		final List<String> invString = Serialization.toString(inv);
		final String backpack = plugin.playerData.get(name);
		plugin.playerData.remove(name);
		final String adminBackpack = plugin.adminFullView.get(name);
		plugin.adminFullView.remove(name);
		if (backpack != null) {
			plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
				@Override
				public void run() {
					if (plugin.isUsingMysql()) {
						try {
							MysqlFunctions.addBackpackData(name, backpack, invString);
						} catch (final SQLException e) {
							e.printStackTrace();
						}
					} else {
						final File file = new File(plugin.getDataFolder() + File.separator + "userdata" + File.separator + name + ".yml");
						final FileConfiguration config = YamlConfiguration.loadConfiguration(file);
						config.set(backpack + ".Inventory", invString);
						try {
							config.save(file);
						} catch (final IOException e) {
							e.printStackTrace();
						}
					}
				}
			});
		} else if (adminBackpack != null) {
			plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
				@Override
				public void run() {
					final String[] split = adminBackpack.split(":");
					if (plugin.isUsingMysql()) {
						try {
							MysqlFunctions.addBackpackData(split[0], split[1], invString);
						} catch (final SQLException e) {
							e.printStackTrace();
						}
					} else {
						final List<String> invString = Serialization.toString(inv);
						final File file = new File(plugin.getDataFolder() + File.separator + "userdata" + File.separator + split[1] + ".yml");
						final FileConfiguration config = YamlConfiguration.loadConfiguration(file);
						config.set(split[0] + ".Inventory", invString);
						try {
							config.save(file);
						} catch (final IOException e) {
							e.printStackTrace();
						}
					}
				}
			});
		} else if (plugin.adminRestrictedView.contains(name)) {
			plugin.adminRestrictedView.remove(name);
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onInventoryClick(final InventoryClickEvent e) {
		if (e.getWhoClicked() instanceof Player) {

			final Player p = (Player) e.getWhoClicked();
			final String name = p.getName();

			if (plugin.adminRestrictedView.contains(name)) {
				e.setCancelled(true);
				return;
			}

			final Inventory otherInv = e.getView().getTopInventory();
			final ItemStack curItem = e.getCurrentItem();
			boolean otherInvPresent = false;

			if (otherInv != null)
				otherInvPresent = true;

			if (curItem != null && curItem.hasItemMeta() && curItem.getItemMeta().hasDisplayName()) {
				for (final String backpack : plugin.backpacks) {
					if (curItem.isSimilar(plugin.backpackItems.get(backpack))) {
						plugin.slowedPlayers.remove(name);
						p.setWalkSpeed(0.2F);
						break;
					}
				}
			}

			if (!e.isCancelled() && curItem != null && otherInvPresent) {

				if (plugin.playerData.containsKey(name)) {

					final String backpack = plugin.playerData.get(name);
					final List<String> key = plugin.backpackData.get(backpack);
					final ItemStack cursor = e.getCursor();
					boolean go = true;

					if (key.get(16) != null && key.get(16).equalsIgnoreCase("true")) {
						for (final String whitelist : plugin.backpackWhitelist.get(backpack)) {
							if (whitelist == null) {
								continue;
							}
							String potentialBackpack = RBUtil.stringToBackpack(whitelist);
							if (potentialBackpack != null && plugin.backpackItems.containsKey(potentialBackpack)) {
								if (curItem.isSimilar(plugin.backpackItems.get(potentialBackpack)) || cursor.isSimilar(plugin.backpackItems.get(potentialBackpack))) {
									go = false;
									break;
								}
							} else {
								if (RBUtil.itemsAreEqual(curItem, whitelist) || RBUtil.itemsAreEqual(cursor, whitelist)) {
									go = false;
									break;
								}
							}
						}
						if (go) {
							e.setCancelled(true);
							p.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.messageData.get("cantPutItemInBackpack")));
							return;
						}
					}

					for (final String blacklist : plugin.backpackBlacklist.get(backpack)) {
						if (blacklist == null) {
							continue;
						}
						String potentialBackpack = RBUtil.stringToBackpack(blacklist);
						if (potentialBackpack != null && plugin.backpackItems.containsKey(potentialBackpack)) {
							if (curItem.isSimilar(plugin.backpackItems.get(potentialBackpack))) {
								e.setCancelled(true);
								p.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.messageData.get("cantPutItemInBackpack")));
								return;
							}
						} else {
							if (RBUtil.itemsAreEqual(curItem, blacklist)) {
								e.setCancelled(true);
								p.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.messageData.get("cantPutItemInBackpack")));
								return;
							}
						}
					}
				}

			}

			/* Dupes
			for (String backpack : plugin.backpackItems.keySet()) {
				if (p.getInventory().contains(plugin.backpackItems.get(backpack))) {
					plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
						@Override
						public void run() {
							Location loc = p.getLocation();
							World world = p.getWorld();
							for (ItemStack item : p.getInventory().getContents()) {
								if (item != null && item.hasItemMeta() && item.getAmount() > 1) {
									for (String backpack : plugin.backpacks) {
										String unstackable = plugin.backpackData.get(backpack).get(18);
										if (unstackable == null || unstackable.equalsIgnoreCase("false")) {
											continue;
										}
										if (item.isSimilar(plugin.backpackItems.get(backpack))) {
											while (item.getAmount() > 1) {
												item.setAmount(item.getAmount() - 1);
												if (p.getInventory().firstEmpty() != -1) {
													p.getInventory().setItem(p.getInventory().firstEmpty(), item);
													p.updateInventory();
												} else {
													world.dropItemNaturally(loc, plugin.backpackItems.get(backpack));
												}
											}
										}
									}
								}
							}
						}
					}, 2L);
					break;
				}
			}
			 */

		}
	}
}
