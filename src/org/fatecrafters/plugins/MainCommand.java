package org.fatecrafters.plugins;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.fatecrafters.plugins.util.MysqlFunctions;
import org.fatecrafters.plugins.util.RBUtil;
import org.fatecrafters.plugins.util.Serialization;

public class MainCommand implements CommandExecutor {

	private final RealisticBackpacks plugin;

	private boolean exist = false;

	public MainCommand(final RealisticBackpacks plugin) {
		this.plugin = plugin;
	}

	@Override
	@SuppressWarnings("deprecation")
	public boolean onCommand(final CommandSender sender, final Command cmd, final String label, final String[] args) {
		if (cmd.getName().equalsIgnoreCase("rb")) {
			if (args.length >= 1) {
				final String command = args[0];
				if (command.equalsIgnoreCase("reload")) {
					if (plugin.isUsingPerms() && !sender.hasPermission("rb.reload")) {
						sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.messageData.get("noPermission")));
						return false;
					}
					final Long first = System.currentTimeMillis();
					plugin.reloadConfig();
					plugin.setupLists();
					plugin.getServer().resetRecipes();
					plugin.setup();
					sender.sendMessage(ChatColor.GRAY + "Config reloaded.");
					sender.sendMessage(ChatColor.GRAY + "Took " + ChatColor.YELLOW + (System.currentTimeMillis() - first) + "ms" + ChatColor.GRAY + ".");
					return true;
				} else if (command.equalsIgnoreCase("buy") || command.equalsIgnoreCase("purchase")) {
					if (!plugin.isUsingVault()) {
						sender.sendMessage(ChatColor.RED + "This command is disabled due to Vault not being installed.");
						return false;
					}
					if (!(sender instanceof Player)) {
						sender.sendMessage(ChatColor.RED + "This command can only be used by a player.");
						return false;
					}
					if (!(args.length == 2)) {
						sender.sendMessage(ChatColor.RED + "Incorrect syntax. Please use:" + ChatColor.GRAY + " /rb buy <backpack>");
						return false;
					}
					String backpack = null;
					backpack = RBUtil.stringToBackpack(args[1]);
					if (backpack == null) {
						sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.messageData.get("backpackDoesNotExist")));
						return false;
					}
					if (plugin.isUsingPerms() && !sender.hasPermission("rb." + backpack + ".buy")) {
						sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.messageData.get("noPermission")));
						return false;
					}
					if (plugin.backpackData.get(backpack).get(13) != null && !plugin.backpackData.get(backpack).get(13).equals("true")) {
						sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.messageData.get("notPurchasable")));
						return false;
					}
					final double price = Double.parseDouble(plugin.backpackData.get(backpack).get(14));
					if (RealisticBackpacks.econ.getBalance(sender.getName()) < price) {
						sender.sendMessage(ChatColor.RED + "You can not afford " + ChatColor.GOLD + price + ChatColor.RED + " to purchase this backpack.");
						return false;
					}
					final Player p = (Player) sender;
					final Inventory inv = p.getInventory();
					final ItemStack backpackItem = plugin.backpackItems.get(backpack);
					if (inv.firstEmpty() != -1) {
						RealisticBackpacks.econ.withdrawPlayer(p.getName(), price);
						if (plugin.backpackData.get(backpack).get(18) != null && plugin.backpackData.get(backpack).get(18).equalsIgnoreCase("true")) {
							if (RealisticBackpacks.globalGlow && plugin.backpackData.get(backpack).get(17) != null && plugin.backpackData.get(backpack).get(17).equalsIgnoreCase("true")) {
								inv.setItem(inv.firstEmpty(), RealisticBackpacks.NMS.addGlow(backpackItem));
							} else {
								inv.setItem(inv.firstEmpty(), backpackItem);
							}
						} else {
							if (RealisticBackpacks.globalGlow && plugin.backpackData.get(backpack).get(17) != null && plugin.backpackData.get(backpack).get(17).equalsIgnoreCase("true")) {
								inv.addItem(RealisticBackpacks.NMS.addGlow(backpackItem));
							} else {
								inv.addItem(backpackItem);
							}
						}
						p.updateInventory();
						sender.sendMessage(ChatColor.GREEN + "You have purchased the " + ChatColor.GOLD + backpack + ChatColor.GREEN + " backpack for " + ChatColor.GOLD + price);
						return true;
					} else {
						sender.sendMessage(ChatColor.RED + "Your inventory is full.");
						return false;
					}
				} else if (command.equalsIgnoreCase("list")) {
					if (plugin.isUsingPerms() && !sender.hasPermission("rb.list")) {
						sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.messageData.get("noPermission")));
						return false;
					}
					sender.sendMessage(ChatColor.LIGHT_PURPLE + "  Name  " + ChatColor.GOLD + "|" + ChatColor.AQUA + "  Size  " + ChatColor.GOLD + "|" + ChatColor.GREEN + "  Price  ");
					sender.sendMessage(ChatColor.GOLD + "-----------------------------------");
					if (plugin.isUsingPerms()) {
						for (final String backpack : plugin.backpacks) {
							final boolean hasPerm = sender.hasPermission("rb." + backpack + ".buy");
							final List<String> key = plugin.backpackData.get(backpack);
							if (plugin.backpackData.get(backpack).get(13).equalsIgnoreCase("true") && hasPerm) {
								sender.sendMessage(ChatColor.LIGHT_PURPLE + backpack + ChatColor.GOLD + " | " + ChatColor.AQUA + key.get(0) + ChatColor.GOLD + " | " + ChatColor.GREEN + Double.parseDouble(key.get(14)));
							} else if (plugin.backpackData.get(backpack).get(13) != null && !plugin.backpackData.get(backpack).get(13).equalsIgnoreCase("true") && hasPerm) {
								sender.sendMessage(ChatColor.LIGHT_PURPLE + backpack + ChatColor.GOLD + " | " + ChatColor.AQUA + key.get(0) + ChatColor.GOLD + " | " + ChatColor.translateAlternateColorCodes('&', plugin.messageData.get("listCommandNotBuyable")));
							} else {
								sender.sendMessage(ChatColor.LIGHT_PURPLE + backpack + ChatColor.GOLD + " | " + ChatColor.AQUA + key.get(0) + ChatColor.GOLD + " | " + ChatColor.translateAlternateColorCodes('&', plugin.messageData.get("listCommandNoPermission")));
							}
						}
					} else {
						for (final String backpack : plugin.backpacks) {
							final List<String> key = plugin.backpackData.get(backpack);
							if (plugin.backpackData.get(backpack).get(13) != null && plugin.backpackData.get(backpack).get(13).equalsIgnoreCase("true")) {
								sender.sendMessage(ChatColor.LIGHT_PURPLE + backpack + ChatColor.GOLD + " | " + ChatColor.AQUA + key.get(0) + ChatColor.GOLD + " | " + ChatColor.GREEN + Double.parseDouble(key.get(14)));
							} else {
								sender.sendMessage(ChatColor.LIGHT_PURPLE + backpack + ChatColor.GOLD + " | " + ChatColor.AQUA + key.get(0) + ChatColor.GOLD + " | " + ChatColor.translateAlternateColorCodes('&', plugin.messageData.get("listCommandNotBuyable")));
							}
						}
					}
				} else if (command.equalsIgnoreCase("give")) {
					if (!(args.length == 3)) {
						sender.sendMessage(ChatColor.RED + "Incorrect syntax. Please use:" + ChatColor.GRAY + " /rb give <player> <backpack>");
						return false;
					}
					String backpack = null;
					backpack = RBUtil.stringToBackpack(args[2]);
					if (plugin.isUsingPerms() && !sender.hasPermission("rb." + backpack + ".give")) {
						sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.messageData.get("noPermission")));
						return false;
					}
					if (backpack == null) {
						sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.messageData.get("backpackDoesNotExist")));
						return false;
					}
					final Player other = plugin.getServer().getPlayer(args[1]);
					if (other == null) {
						sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.messageData.get("playerDoesNotExist")));
						return false;
					}
					final Inventory inv = other.getInventory();
					final ItemStack backpackItem = plugin.backpackItems.get(backpack);
					if (inv.firstEmpty() != -1) {
						if (plugin.backpackData.get(backpack).get(18) != null && plugin.backpackData.get(backpack).get(18).equalsIgnoreCase("true")) {
							if (RealisticBackpacks.globalGlow && plugin.backpackData.get(backpack).get(17) != null && plugin.backpackData.get(backpack).get(17).equalsIgnoreCase("true")) {
								inv.setItem(inv.firstEmpty(), RealisticBackpacks.NMS.addGlow(backpackItem));
							} else {
								inv.setItem(inv.firstEmpty(), backpackItem);
							}
						} else {
							if (RealisticBackpacks.globalGlow && plugin.backpackData.get(backpack).get(17) != null && plugin.backpackData.get(backpack).get(17).equalsIgnoreCase("true")) {
								inv.addItem(RealisticBackpacks.NMS.addGlow(backpackItem));
							} else {
								inv.addItem(backpackItem);
							}
						}
						other.updateInventory();
						sender.sendMessage(ChatColor.GREEN + "You have given the " + ChatColor.GOLD + backpack + ChatColor.GREEN + " backpack to " + ChatColor.GOLD + other.getName());
						return true;
					} else {
						sender.sendMessage(ChatColor.RED + other.getName() + "'s inventory is full.");
						return false;
					}
				} else if (command.equalsIgnoreCase("filetomysql")) {
					if (plugin.isUsingPerms() && !sender.hasPermission("rb.filetomysql")) {
						sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.messageData.get("noPermission")));
						return false;
					}
					plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
						@Override
						public void run() {
							if (!MysqlFunctions.checkIfTableExists("rb_data")) {
								MysqlFunctions.createTables();
								exist = false;
							} else {
								exist = true;
							}
							try {
								final Connection conn = DriverManager.getConnection(plugin.getUrl(), plugin.getUser(), plugin.getPass());
								final File dir = new File(plugin.getDataFolder() + File.separator + "userdata");
								int i = 0, times = 0;
								final int files = dir.listFiles().length;
								for (final File child : dir.listFiles()) {
									final FileConfiguration config = YamlConfiguration.loadConfiguration(child);
									final String player = child.getName().replace(".yml", "");
									i++;
									PreparedStatement statement = null;
									PreparedStatement state = null;
									for (final String backpack : config.getConfigurationSection("").getKeys(false)) {
										for (final String number : config.getConfigurationSection(backpack).getKeys(false)) {
											int pnumber = Integer.parseInt(number);
											if (exist) {
												statement = conn.prepareStatement("SELECT EXISTS(SELECT 1 FROM rb_data WHERE player = ? AND backpack = ? AND number = ? LIMIT 1);");
												statement.setString(1, player);
												statement.setString(2, backpack);
												statement.setInt(3, Integer.parseInt(number));
												final ResultSet res = statement.executeQuery();
												if (res.next()) {
													if (res.getInt(1) == 1) {
														state = conn.prepareStatement("UPDATE rb_data SET player=?, backpack=?, inventory=? number=? WHERE player=? AND backpack=? AND number=?;");
														state.setString(1, player);
														state.setString(2, backpack);
														state.setString(3, Serialization.listToString(config.getStringList(backpack + "." + pnumber + ".Inventory")));
														state.setInt(4, pnumber);
														state.setString(5, player);
														state.setString(6, backpack);
														state.setInt(7, pnumber);
													} else {
														state = conn.prepareStatement("INSERT INTO rb_data (player, backpack, inventory, number) VALUES(?, ?, ?, ?);");
														state.setString(1, player);
														state.setString(2, backpack);
														state.setString(3, Serialization.listToString(config.getStringList(backpack + "." + pnumber + ".Inventory")));
														state.setInt(4, pnumber);
													}
												}
											} else {
												state = conn.prepareStatement("INSERT INTO rb_data (player, backpack, inventory, number) VALUES(?, ?, ?, ?);");
												state.setString(1, player);
												state.setString(2, backpack);
												state.setString(3, Serialization.listToString(config.getStringList(backpack + "." + pnumber + ".Inventory")));
												state.setInt(4, pnumber);
											}
											state.executeUpdate();
											state.close();
										}
									}
									if (i == 50) {
										i = 0;
										times++;
										sender.sendMessage(ChatColor.LIGHT_PURPLE + "" + times * 50 + "/" + files + " files have been transferred.");
									}
								}
								conn.close();
								sender.sendMessage(ChatColor.LIGHT_PURPLE + "File transfer complete.");
							} catch (final SQLException e) {
								e.printStackTrace();
							}
						}
					});
				} else if (command.equalsIgnoreCase("view")) {
					if (!(args.length == 4)) {
						sender.sendMessage(ChatColor.RED + "Incorrect syntax. Please use:" + ChatColor.GRAY + " /rb view <player> <backpack> <number>");
						return false;
					}
					String backpack = null;
					backpack = RBUtil.stringToBackpack(args[2]);
					boolean fullview = false;
					boolean restrictedview = false;
					if (plugin.isUsingPerms() && sender.hasPermission("rb.fullview")) {
						fullview = true;
					} else if (plugin.isUsingPerms() && sender.hasPermission("rb.restrictedview")) {
						restrictedview = true;
					}
					if (plugin.isUsingPerms() && !fullview && !restrictedview) {
						sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.messageData.get("noPermission")));
						return false;
					}
					if (backpack == null) {
						sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.messageData.get("backpackDoesNotExist")));
						return false;
					}
					if (!RBUtil.stringIsInteger(args[3])) {
						sender.sendMessage(ChatColor.RED + "Number of backpack is not a integer in your syntax.");
						return false;
					}
					int number = Integer.parseInt(args[3]);
					Inventory inv = null;
					String name = args[1];
					final Player p = (Player) sender;
					final List<String> key = plugin.backpackData.get(backpack);
					if (!plugin.isUsingMysql()) {
						boolean fileExists = false;
						String fullName = null;
						File file = null;
						final File dir = new File(plugin.getDataFolder() + File.separator + "userdata");
						for (final File f : dir.listFiles()) {
							final String fileName = f.getName();
							fullName = fileName.replace(".yml", "");
							if (fullName.equalsIgnoreCase(name)) {
								name = fullName;
								file = f;
								fileExists = true;
								break;
							}
						}
						if (!fileExists) {
							sender.sendMessage(ChatColor.RED + "This player has never opened this backpack with flatfile data.");
							return false;
						}
						final FileConfiguration config = YamlConfiguration.loadConfiguration(file);
						if (!config.isSet(backpack + "." + number + ".Inventory")) {
							sender.sendMessage(ChatColor.RED + "This player has never opened this backpack with flatfile data.");
							return false;
						} else {
							inv = Serialization.toInventory(config.getStringList(backpack + "." + number + ".Inventory"), fullName + "'s " + backpack + " data", Integer.parseInt(key.get(0)));
						}
					} else {
						try {
							inv = MysqlFunctions.getBackpackInv(name, backpack, number);
						} catch (final SQLException e1) {
							e1.printStackTrace();
						}
						if (inv == null) {
							sender.sendMessage(ChatColor.RED + "This player has never opened this backpack with MySQL data.");
							return false;
						}
					}
					if (plugin.playerData.containsKey(name)) {
						sender.sendMessage(ChatColor.RED + "This player is in his backpack, wait for him to close it.");
						return false;
					}
					if (fullview || !plugin.isUsingPerms()) {
						plugin.adminFullView.put(sender.getName(), backpack + ":" + name + ":" + number);
					} else {
						plugin.adminRestrictedView.add(sender.getName());
					}
					p.openInventory(inv);
				} else if (command.equalsIgnoreCase("check")) {
					if (!(args.length == 3)) {
						sender.sendMessage(ChatColor.RED + "Incorrect syntax. Please use:" + ChatColor.GRAY + " /rb check <player> <backpack>");
						return false;
					}
					String backpack = null, name = args[1];
					backpack = RBUtil.stringToBackpack(args[2]);
					if (plugin.isUsingPerms() && !sender.hasPermission("rb.check")) {
						sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.messageData.get("noPermission")));
						return false;
					}
					if (backpack == null) {
						sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.messageData.get("backpackDoesNotExist")));
						return false;
					}
					Inventory inv = null;
					sender.sendMessage(ChatColor.GRAY + "" + backpack + "'s information for " + name);
					sender.sendMessage(ChatColor.BLUE + "BP-Number" + ChatColor.RESET + "  |  " + ChatColor.RED + "Number of items it contains");
					sender.sendMessage(ChatColor.GRAY + "-----------------------------------");
					if (!plugin.isUsingMysql()) {
						final File file = new File(plugin.getDataFolder() + File.separator + "userdata" + File.separator + name + ".yml");
						final FileConfiguration config = YamlConfiguration.loadConfiguration(file);
						for (int number : RBUtil.getNumbers(config, backpack)) {
							inv = Serialization.toInventory(config.getStringList(backpack + "." + number + ".Inventory"), "Kappa", Integer.parseInt(plugin.backpackData.get(backpack).get(0)));
						}
					} else {
						for (int i : MysqlFunctions.getNumbers(name, backpack)) {
							try {
								inv = MysqlFunctions.getBackpackInv(name, backpack, i);
							} catch (SQLException e) {
								e.printStackTrace();
							}
							sender.sendMessage(ChatColor.BLUE + "" + i + ChatColor.RESET + "  |  " + ChatColor.RED + getItemAmount(inv.getContents()));
						}
					}
				} else {
					sender.sendMessage(ChatColor.RED + "Command not found.");
				}
			}
		}
		return false;
	}

	private int getItemAmount(ItemStack[] items) {
		int i = 0;
		for (ItemStack item : items) {
			if (!(item == null)) {
				i++;
			}
		}
		return i;
	}

}
