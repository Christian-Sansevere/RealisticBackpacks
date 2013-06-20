package org.fatecrafters.plugins;

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
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class RBListener implements Listener {

	private RealisticBackpacks plugin;

	private HashMap<String,String> playerData = new HashMap<String,String>();
	private float walkSpeedMultiplier = 0.0F;
	private int setlevel = 0;
	private List<String> slowedPlayers = new ArrayList<String>();
	private HashMap<String, String> deadPlayers = new HashMap<String, String>();

	public RBListener(RealisticBackpacks plugin) {
		this.plugin = plugin;
	}

	/*List key ---------
	 * 0 = Size
	 * 1 = UseRecipe
	 * 2 = id
	 * 3 = name
	 * 4 = destroyContents
	 * 5 = dropContents
	 * 6 = dropBackpack
	 * 7 = keepBackpack
	 * 8 = walkSpeedEnabled
	 * 9 = walkSpeedMultiplier
	 * 10 = increasedHungerEnabled
	 * 11 = hungerBarsToDeplete
	 * 12 = hungerBarsToSubtractWhenEating
	 * 13 = Purchasable
	 * 14 = Price
	 * 15 = OpenWith
	 */

	@SuppressWarnings("deprecation")
	@EventHandler(priority = EventPriority.NORMAL)
	public void onRightClick(PlayerInteractEvent e) {
		Action act = e.getAction();
		Player p = e.getPlayer();
		ItemStack item = p.getItemInHand();
		String name = p.getName();
		if (item.hasItemMeta()) {
			for (String backpack : plugin.backpacks) {
				List<String> key = plugin.backpackData.get(backpack);
				if (item.getItemMeta().hasDisplayName() && item.getItemMeta().getDisplayName().equals(ChatColor.translateAlternateColorCodes('&', key.get(3)))) {
					String openWith = key.get(15);
					if (openWith != null) {
						if (openWith.equalsIgnoreCase("left_click")) {
							if (act.equals(Action.RIGHT_CLICK_AIR)) continue;
							if (act.equals(Action.RIGHT_CLICK_BLOCK)) continue;
						} else if (openWith.equalsIgnoreCase("right_click")) {
							if (act.equals(Action.LEFT_CLICK_AIR)) continue;
							if (act.equals(Action.LEFT_CLICK_BLOCK)) continue;
						}
					} else {
						if (act.equals(Action.LEFT_CLICK_AIR)) continue;
						if (act.equals(Action.LEFT_CLICK_BLOCK)) continue;
					}
					if (!p.hasPermission("rb."+backpack+".use")) {
						p.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.messageData.get("openBackpackPermError")));
						continue;
					}
					if (act.equals(Action.RIGHT_CLICK_BLOCK)) {
						e.setCancelled(true);
						p.updateInventory();
					}
					Inventory inv = null;
					if (plugin.isUsingMysql()) {
						try {
							inv = MysqlFunctions.getBackpackInv(name, backpack);
						} catch (SQLException e1) {
							e1.printStackTrace();
						}
						if (inv == null) {
							inv = plugin.getServer().createInventory(p, Integer.parseInt(key.get(0)), ChatColor.translateAlternateColorCodes('&', key.get(3)));
						}
					} else {
						File file = new File(plugin.getDataFolder()+File.separator+"userdata"+File.separator+name+".yml");
						if (!file.exists()) {
							try {
								file.createNewFile();
							} catch (IOException e1) {
								e1.printStackTrace();
							}
						}
						FileConfiguration config = YamlConfiguration.loadConfiguration(file);
						if (config.getString(backpack+".Inventory") == null) {
							inv = plugin.getServer().createInventory(p, Integer.parseInt(key.get(0)), ChatColor.translateAlternateColorCodes('&', key.get(3)));
						} else {
							inv = RealisticBackpacks.inter.stringToInventory(config.getString(backpack+".Inventory"), key.get(3));
						}
					}
					playerData.put(name, backpack);
					p.openInventory(inv);
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onInventoryClose(InventoryCloseEvent e) {
		final String name = e.getPlayer().getName();
		if (playerData.containsKey(name)) {
			final Inventory inv = e.getView().getTopInventory();
			plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
				public void run() {
					if (plugin.isUsingMysql()) {
						try {
							MysqlFunctions.addBackpackData(name, playerData.get(name), inv);
						} catch (SQLException e) {
							e.printStackTrace();
						}
					} else {
						String invString = RealisticBackpacks.inter.inventoryToString(inv);
						File file = new File(plugin.getDataFolder()+File.separator+"userdata"+File.separator+name+".yml");
						FileConfiguration config = YamlConfiguration.loadConfiguration(file);
						config.set(playerData.get(name)+".Inventory", invString);
						try {
							config.save(file);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					playerData.remove(name);
				}
			});
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onCraft(PrepareItemCraftEvent e) {
		ItemStack result = e.getInventory().getResult();
		if (result.hasItemMeta()) {
			for (String backpack : plugin.backpacks) {
				List<String> key = plugin.backpackData.get(backpack);
				if (!result.hasItemMeta()) 
					continue;
				if (!result.getItemMeta().getDisplayName().equals(ChatColor.translateAlternateColorCodes('&', key.get(3)))) 
					continue;
				HumanEntity human = e.getView().getPlayer();
				if (!(human instanceof Player)) 
					continue;
				if (!human.hasPermission("rb."+backpack+".craft")) { 
					e.getInventory().setResult(null);
					((Player)human).sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.messageData.get("craftPermError")));
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onDeath(PlayerDeathEvent e) {
		Player p = e.getEntity();
		String name = p.getName();
		for (String backpack : plugin.backpacks) {
			if (p.hasPermission("rb."+backpack+".deathbypass"))
				continue;
			if (!p.getInventory().contains(plugin.backpackItems.get(backpack)))
				continue;
			p.setWalkSpeed(0.2F);
			List<String> key = plugin.backpackData.get(backpack);
			if (key.get(5) != null && key.get(5).equals("true")) {
				//Drop contents
				Inventory binv = null;
				if (plugin.isUsingMysql()) {
					try {
						binv = MysqlFunctions.getBackpackInv(name, backpack);
					} catch (SQLException e1) {
						e1.printStackTrace();
					}
				} else {
					File file = new File(plugin.getDataFolder()+File.separator+"userdata"+File.separator+name+".yml");
					if (!file.exists()) {
						continue;
					}
					FileConfiguration config = YamlConfiguration.loadConfiguration(file);
					if (config.getString(backpack+".Inventory") == null) {
						continue;
					}
					binv = RealisticBackpacks.inter.stringToInventory(config.getString(backpack+".Inventory"), key.get(3));
				}
				if (binv != null) {
					for (ItemStack item : binv.getContents()) {
						if (item != null) {
							p.getWorld().dropItemNaturally(p.getLocation(), item);
						}
					}
				}
			}
			if (key.get(4) != null && key.get(4).equals("true")) {
				//Destroy contents
				destroyContents(backpack, name);
				p.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.messageData.get("contentsDestroyed")));
			}
			if (key.get(6) != null && key.get(6).equals("false")) {
				//Drop backpack
				e.getDrops().remove(plugin.backpackItems.get(backpack));
			}
			if (key.get(7) != null && key.get(7).equals("true")) {
				deadPlayers.put(name, backpack);
			}
		}
	}

	@SuppressWarnings("deprecation")
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onRespawn(PlayerRespawnEvent e) {
		Player p = e.getPlayer();
		String name = p.getName();
		for (String backpack : plugin.backpacks) {
			List<String> key = plugin.backpackData.get(backpack);
			if (key.get(7) != null && key.get(7).equals("true") && deadPlayers.get(name) != null && deadPlayers.get(name).equals(backpack)) {
				//Keep backpack
				p.getInventory().addItem(plugin.backpackItems.get(backpack));
				p.updateInventory();
				deadPlayers.remove(name);
			}
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onMove(final PlayerMoveEvent e) {
		final Player p = e.getPlayer();
		final String name = p.getName();
		final Inventory inv = p.getInventory();
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
			public void run() {
				List<String> backpackList = new ArrayList<String>();
				for (String backpack : plugin.backpacks) {
					List<String> key = plugin.backpackData.get(backpack);
					if (key.get(8).equals("true") && inv.contains(plugin.backpackItems.get(backpack))) {
						backpackList.add(backpack);
					}
				}
				int listsize = backpackList.size();
				if (listsize > 0) {
					if (listsize > 1) {
						if (plugin.isAveraging()) {
							float average = 0;
							for (String backpack : backpackList) {
								average += Float.parseFloat(plugin.backpackData.get(backpack).get(9));
							}
							walkSpeedMultiplier = average / listsize;
						} else if (plugin.isAdding()) {
							float sum = 0;
							for (String backpack : backpackList) {
								sum += 0.2F - Float.parseFloat(plugin.backpackData.get(backpack).get(9));
							}
							walkSpeedMultiplier = 0.2F - sum;
						} else {
							List<Float> floatList = new ArrayList<Float>();
							for (String backpack : backpackList) {
								floatList.add(Float.parseFloat(plugin.backpackData.get(backpack).get(9)));
							}
							walkSpeedMultiplier = Collections.max(floatList);
						}
					} else if (listsize == 1) {
						for (String backpack : backpackList) {
							walkSpeedMultiplier = Float.parseFloat(plugin.backpackData.get(backpack).get(9));
						}
					}
					if (!slowedPlayers.contains(name)) {
						slowedPlayers.add(name);
					}
					plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
						public void run() {
							p.setWalkSpeed(walkSpeedMultiplier);
						}
					});
				}
			}
		});
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onDrop(PlayerDropItemEvent e) {
		final Player p = e.getPlayer();
		final String name = p.getName();
		final ItemStack item = e.getItemDrop().getItemStack();
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
			public void run() {
				if (slowedPlayers.contains(name)) {
					for (String backpack : plugin.backpacks) {
						if (plugin.backpackItems.get(backpack).equals(item)) {
							slowedPlayers.remove(name);
							plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
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

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onPickup(PlayerPickupItemEvent e) {
		final ItemStack item = e.getItem().getItemStack();
		final Player p = e.getPlayer();
		final String name = p.getName();
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
			public void run() {
				for (String backpack : plugin.backpacks) {
					if (!item.equals(plugin.backpackItems.get(backpack))) continue;
					final List<String> key = plugin.backpackData.get(backpack);
					if (!slowedPlayers.contains(name)) {
						slowedPlayers.add(name);
					}
					plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
						public void run() {
							p.setWalkSpeed(Float.parseFloat(key.get(9)));
						}
					});
				}
			}
		});
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onInventoryClick(InventoryClickEvent e) {
		if (e.getWhoClicked() instanceof Player) {
			final Player p = (Player) e.getWhoClicked();
			final ItemStack curItem = e.getCurrentItem();
			final Inventory inv = p.getInventory();
			final Inventory otherInv = e.getView().getTopInventory();
			if (curItem != null && otherInv != null) {
				plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
					public void run() {
						if (slowedPlayers.contains(p.getName())) {
							for (String backpack : plugin.backpacks) {
								ItemStack backpackItem = plugin.backpackItems.get(backpack);
								if (!curItem.equals(backpackItem)) continue;
								if (!inv.contains(backpackItem)) {
									slowedPlayers.remove(p.getName());
									plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
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

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onFoodChange(final FoodLevelChangeEvent e) {
		if (e.getEntity() instanceof Player) {
			final int foodlevel = e.getFoodLevel();
			final Player p = (Player) e.getEntity();
			final int pLevel = p.getFoodLevel();
			final Inventory inv = p.getInventory();
			plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
				public void run() {
					List<String> backpackList = new ArrayList<String>();
					for (String backpack : plugin.backpacks) {
						List<String> key = plugin.backpackData.get(backpack);
						if (!key.get(10).equals("true")) continue;
						if (!inv.contains(plugin.backpackItems.get(backpack))) continue;
						backpackList.add(backpack);
					}
					int listsize = backpackList.size();
					if (listsize > 0) {
						if (pLevel > foodlevel) {
							//Starving
							setlevel = getFoodLevel(foodlevel, pLevel, listsize, 11, backpackList);
							if (setlevel < 0) {
								setlevel = 0;
							}
						} else {
							//Ate food
							setlevel = getFoodLevel(foodlevel, pLevel, listsize, 12, backpackList);
							if (setlevel < pLevel) {
								setlevel = pLevel;
							}
						}
						plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
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

	private int getFoodLevel(int foodlevel, int pLevel, int listsize, int key, List<String> backpackList) {
		int i = 0;
		//Starving
		if (plugin.isAveraging()) {
			int average = 0;
			for (String backpack : backpackList) {
				average += Integer.parseInt(plugin.backpackData.get(backpack).get(key));
			}
			i = foodlevel - (average / listsize);
		} else if (plugin.isAdding()) {
			int sum = 0;
			for (String backpack : backpackList) {
				sum += Integer.parseInt(plugin.backpackData.get(backpack).get(key));
			}
			i = foodlevel - sum;
		} else {
			List<Integer> list = new ArrayList<Integer>();
			for (String backpack : backpackList) {
				list.add(Integer.parseInt(plugin.backpackData.get(backpack).get(key)));
			}
			i = foodlevel - Collections.max(list);
		}
		return i;
	}

	private void destroyContents(final String backpack, final String name) {
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
			public void run() {
				if (plugin.isUsingMysql()) {
					MysqlFunctions.delete(name, backpack);
				} else {
					File file = new File(plugin.getDataFolder()+File.separator+"userdata"+File.separator+name+".yml");
					FileConfiguration config = YamlConfiguration.loadConfiguration(file);
					config.set(backpack+".Inventory", null);
					try {
						config.save(file);
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
			}
		});
	}

}
