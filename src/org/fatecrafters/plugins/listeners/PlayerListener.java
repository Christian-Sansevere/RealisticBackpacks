package org.fatecrafters.plugins.listeners;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.fatecrafters.plugins.RealisticBackpacks;
import org.fatecrafters.plugins.util.MysqlFunctions;
import org.fatecrafters.plugins.util.RBUtil;
import org.fatecrafters.plugins.util.Serialization;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

public class PlayerListener implements Listener {

	private final RealisticBackpacks plugin;

	private final HashMap<String, String> deadPlayers = new HashMap<String, String>();
	private float walkSpeedMultiplier = 0.0F;

	public PlayerListener(final RealisticBackpacks plugin) {
		this.plugin = plugin;
	}

	@SuppressWarnings("deprecation")
	@EventHandler(priority = EventPriority.HIGH)
	public void onInteract(final PlayerInteractEvent e) {
		if (e.getAction().equals(Action.PHYSICAL)) {
			return;
		}

		final Action act = e.getAction();
		final Player p = e.getPlayer();
		final ItemStack item = p.getItemInHand();
		if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
			for (final String backpack : plugin.backpacks) {
				final List<String> key = plugin.backpackData.get(backpack);
				if (item.getItemMeta().getDisplayName().equals(ChatColor.translateAlternateColorCodes('&', key.get(3)))) {
					if (plugin.isUsingPerms() && !p.hasPermission("rb." + backpack + ".use")) {
						p.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.messageData.get("openBackpackPermError")));
						continue;
					}
					final String openWith = key.get(15);
					if (openWith != null) {
						if (openWith.equalsIgnoreCase("left_click")) {
							if (act.equals(Action.RIGHT_CLICK_AIR)) {
								continue;
							}
							if (act.equals(Action.RIGHT_CLICK_BLOCK)) {
								continue;
							}
						} else if (openWith.equalsIgnoreCase("right_click")) {
							if (act.equals(Action.LEFT_CLICK_AIR)) {
								continue;
							}
							if (act.equals(Action.LEFT_CLICK_BLOCK)) {
								continue;
							}
						}
					} else {
						if (act.equals(Action.LEFT_CLICK_AIR)) {
							continue;
						}
						if (act.equals(Action.LEFT_CLICK_BLOCK)) {
							continue;
						}
					}
					if (act.equals(Action.RIGHT_CLICK_BLOCK) && item.getType().isBlock()) {
						e.setCancelled(true);
						p.updateInventory();
					}
					Inventory inv = null;
					int number = 0;
					String name = null;
					boolean newBackpack = false, firstNewBackpack = false;
					if (item.getItemMeta().hasLore()) {
						number = RBUtil.getItemNumber(item.getItemMeta().getLore());
						name = RBUtil.getItemOwner(item.getItemMeta().getLore());
					}
					if (number == 0 || name == null) {
						ItemStack newItem = item;
						final ItemMeta meta = item.getItemMeta();
						final List<String> lore = meta.getLore();
						int nextNumber = 0;
						if (plugin.isUsingMysql()) {
							nextNumber = MysqlFunctions.getNextNumber(p.getName(), backpack);
							lore.add(ChatColor.GRAY + "" + ChatColor.ITALIC + "BP#: " + nextNumber);
							lore.add(ChatColor.GRAY + "" + ChatColor.ITALIC + "BP-Owner: " + p.getName());
						} else {
							final File file = new File(plugin.getDataFolder() + File.separator + "userdata" + File.separator + p.getName() + ".yml");
							final FileConfiguration fig = YamlConfiguration.loadConfiguration(file);
							nextNumber = RBUtil.getNextNumber(fig, backpack);
							lore.add(ChatColor.GRAY + "" + ChatColor.ITALIC + "BP#: " + nextNumber);
							lore.add(ChatColor.GRAY + "" + ChatColor.ITALIC + "BP-Owner: " + p.getName());
						}
						if (nextNumber == 1) {
							firstNewBackpack = true;
						}
						meta.setLore(lore);
						newItem.setItemMeta(meta);
						if (item.getAmount() > 1) {
							newItem.setAmount(1);
							item.setAmount(item.getAmount() - 1);
							if (p.getInventory().firstEmpty() == -1) {
								p.getWorld().dropItemNaturally(p.getLocation(), item);
							} else {
								p.getInventory().setItem(p.getInventory().firstEmpty(), item);
							}
						} else {
							p.setItemInHand(newItem);
						}
						number = nextNumber;
						newBackpack = true;
					}
					if (plugin.isUsingMysql()) {
						try {
							if (!newBackpack) {
								inv = MysqlFunctions.getBackpackInv(name, backpack, number);
							} else if (firstNewBackpack) {
								inv = MysqlFunctions.getBackpackInv(name, backpack, 1);
							}
						} catch (final SQLException e1) {
							e1.printStackTrace();
						}
					} else {
						final File file = new File(plugin.getDataFolder() + File.separator + "userdata" + File.separator + name + ".yml");
						if (!file.exists()) {
							try {
								file.createNewFile();
							} catch (final IOException e1) {
								e1.printStackTrace();
							}
						}
						final FileConfiguration config = YamlConfiguration.loadConfiguration(file);
						if (config.isSet(backpack + "." + number + ".Inventory") && !newBackpack) {
							inv = Serialization.toInventory(config.getStringList(backpack + "." + number + ".Inventory"), key.get(3), Integer.parseInt(key.get(0)));
						} else if (firstNewBackpack) {
							inv = Serialization.toInventory(config.getStringList(backpack + ".1.Inventory"), key.get(3), Integer.parseInt(key.get(0)));
						}
					}
					if (inv == null) {
						inv = plugin.getServer().createInventory(p, Integer.parseInt(key.get(0)), ChatColor.translateAlternateColorCodes('&', key.get(3)));
					}
					if (p.getOpenInventory() != null) {
						p.closeInventory();
					}
					p.openInventory(inv);
					plugin.playerData.put(name, backpack);
					plugin.playerData2.put(name, number);
					break;
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onDrop(final PlayerDropItemEvent e) {
		final Player p = e.getPlayer();
		final String name = p.getName();
		final ItemStack item = e.getItemDrop().getItemStack();
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
			@Override
			public void run() {
				if (plugin.slowedPlayers.contains(name)) {
					for (final String backpack : plugin.backpacks) {
						if (plugin.backpackItems.get(backpack).equals(item)) {
							plugin.slowedPlayers.remove(name);
							plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
								@Override
								public void run() {
									p.setWalkSpeed(0.2F);
								}
							});
							break;
						}
					}
				}
			}
		});
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onPickup(final PlayerPickupItemEvent e) {
		final ItemStack item = e.getItem().getItemStack();
		final Player p = e.getPlayer();
		final String name = p.getName();
		for (final String backpack : plugin.backpacks) {
			if (!item.isSimilar(plugin.backpackItems.get(backpack))) {
				continue;
			}
			final List<String> key = plugin.backpackData.get(backpack);
			if (!plugin.slowedPlayers.contains(name)) {
				plugin.slowedPlayers.add(name);
			}
			p.setWalkSpeed(Float.parseFloat(key.get(9)));
			/*if (key.get(18) != null && key.get(18).equalsIgnoreCase("true")) {
				final Inventory inv = p.getInventory();
				final Location loc = e.getItem().getLocation();
				final ItemStack backpackItem = plugin.backpackItems.get(backpack);
				int emptySlots = 0, itemAmount = item.getAmount();
				for (final ItemStack invItem : inv.getContents()) {
					if (invItem == null) {
						emptySlots++;
					}
				}
				if (emptySlots == 0) {
					e.setCancelled(true);
				} else {
					e.getItem().remove();
					e.setCancelled(true);
					if (itemAmount > emptySlots) {
						final ItemStack dropItem = backpackItem;
						dropItem.setAmount(itemAmount - emptySlots);
						p.getWorld().dropItem(loc, dropItem);
						itemAmount = emptySlots;
					}
					if (itemAmount == 1) {
						backpackItem.setAmount(1);
						inv.setItem(inv.firstEmpty(), backpackItem);
					} else if (itemAmount > 1) {
						int x = itemAmount;
						backpackItem.setAmount(1);
						while (x > 0) {
							x--;
							inv.setItem(inv.firstEmpty(), backpackItem);
						}
					}
				}
			}*/// Unstackable backpacks
			break;
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onMove(final PlayerMoveEvent e) {
		final Player p = e.getPlayer();
		final String name = p.getName();
		if (plugin.slowedPlayers.contains(name)) {
			return;
		}

		final Inventory inv = p.getInventory();
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
			@Override
			public void run() {
				final List<String> backpackList = new ArrayList<String>();
				for (final String backpack : plugin.backpacks) {
					final List<String> key = plugin.backpackData.get(backpack);
					if (key.get(8) != null && key.get(8).equalsIgnoreCase("true") && inv != null && inv.contains(plugin.backpackItems.get(backpack))) {
						backpackList.add(backpack);
					}
				}
				final int listsize = backpackList.size();
				if (listsize > 0) {
					if (listsize > 1) {
						if (plugin.isAveraging()) {
							float average = 0;
							for (final String backpack : backpackList) {
								average += Float.parseFloat(plugin.backpackData.get(backpack).get(9));
							}
							walkSpeedMultiplier = average / listsize;
						} else if (plugin.isAdding()) {
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
					} else if (listsize == 1) {
						walkSpeedMultiplier = Float.parseFloat(plugin.backpackData.get(backpackList.get(0)).get(9));
					}
					plugin.slowedPlayers.add(name);
					plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
						@Override
						public void run() {
							p.setWalkSpeed(walkSpeedMultiplier);
						}
					});
				}
			}
		});
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onDeath(final PlayerDeathEvent e) {
		final Player p = e.getEntity();
		Multimap<String, ItemStack> backpackItemList = ArrayListMultimap.create();
		for (ItemStack item : p.getInventory().getContents()) {
			if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
				for (final String backpack : plugin.backpacks) {
					if (item.getItemMeta().getDisplayName().equals(ChatColor.translateAlternateColorCodes('&', plugin.backpackData.get(backpack).get(3)))) {
						backpackItemList.put(backpack, item);
					}
				}
			}
		}
		if (backpackItemList.isEmpty()) {
			return;
		}
		p.setWalkSpeed(0.2F);
		for (final String backpack : backpackItemList.keySet()) {
			final List<String> key = plugin.backpackData.get(backpack);
			for (ItemStack item : backpackItemList.get(backpack)) {
				int number = 0;
				String name = null;
				if (item.getItemMeta().hasLore()) {
					number = RBUtil.getItemNumber(item.getItemMeta().getLore());
					name = RBUtil.getItemOwner(item.getItemMeta().getLore());
				}
				if (number == 0 || name == null) {
					continue;
				}
				if (key.get(5) != null && key.get(5).equalsIgnoreCase("true")) {
					//Drop contents
					Inventory binv = null;
					if (plugin.isUsingMysql()) {
						try {
							binv = MysqlFunctions.getBackpackInv(name, backpack, number);
						} catch (final SQLException e1) {
							e1.printStackTrace();
						}
					} else {
						final File file = new File(plugin.getDataFolder() + File.separator + "userdata" + File.separator + name + ".yml");
						if (!file.exists()) {
							continue;
						}
						final FileConfiguration config = YamlConfiguration.loadConfiguration(file);
						if (config.getStringList(backpack + "." + number + ".Inventory") == null) {
							continue;
						}
						binv = Serialization.toInventory(config.getStringList(backpack + "." + number + ".Inventory"), key.get(3), Integer.parseInt(key.get(0)));
					}
					if (plugin.playerData.containsKey(name)) {
						if (p.getItemOnCursor() != null) {
							p.setItemOnCursor(null);
						}
					}
					if (binv != null) {
						for (final ItemStack items : binv.getContents()) {
							if (items != null) {
								p.getWorld().dropItemNaturally(p.getLocation(), item);
							}
						}
					}
					RBUtil.destroyContents(name, backpack, number);
				}
				if (key.get(4) != null && key.get(4).equalsIgnoreCase("true")) {
					//Destroy contents
					RBUtil.destroyContents(name, backpack, number);
					p.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.messageData.get("contentsDestroyed")));
				}
				if (key.get(6) != null && key.get(6).equalsIgnoreCase("false")) {
					//Do not drop backpack
					e.getDrops().remove(plugin.backpackItems.get(backpack));
				}
				if (key.get(7) != null && key.get(7).equalsIgnoreCase("true")) {
					deadPlayers.put(name, backpack);
				}
			}
		}
	}

	@SuppressWarnings("deprecation")
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onRespawn(final PlayerRespawnEvent e) {
		final Player p = e.getPlayer();
		final String name = p.getName();
		for (final String backpack : plugin.backpacks) {
			final List<String> key = plugin.backpackData.get(backpack);
			if (key.get(7) != null && key.get(7).equalsIgnoreCase("true") && deadPlayers.get(name) != null && deadPlayers.get(name).equals(backpack)) {
				//Keep backpack
				p.getInventory().addItem(plugin.backpackItems.get(backpack));
				p.updateInventory();
				deadPlayers.remove(name);
			}
		}
	}

}
