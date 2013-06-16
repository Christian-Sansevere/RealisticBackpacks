package org.fatecrafters.plugins;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.MaterialData;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class RealisticBackpacks extends JavaPlugin {

	//ToDo:
	//Pay for backpacks via command, give backpacks via command
	
	public static Economy econ = null;
	private boolean vault = true;
	private static boolean average = false;
	private static boolean add = false;
	public static List<String> backpacks = new ArrayList<String>();
	public static HashMap<String, String> messageData = new HashMap<String, String>();
	public static HashMap<String,List<String>> backpackData = new HashMap<String,List<String>>();
	public static HashMap<String,List<String>> backpackLore = new HashMap<String,List<String>>();
	public static HashMap<String,List<String>> backpackRecipe = new HashMap<String,List<String>>();
	public static HashMap<String,ItemStack> backpackItems = new HashMap<String,ItemStack>();

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
	 */

	@Override
	public void onEnable() { 
		saveDefaultConfig();
		getServer().getPluginManager().registerEvents(new RBListener(this), this);
		if (!setupEconomy()) {
			getLogger().info("Vault not found, economy features disabled.");
			vault = false;
		} else {
			getLogger().info("Vault found, economy features enabled.");
		}
		File f = new File(getDataFolder()+File.separator+"messages.yml");
		if (!f.exists()) {
			try {
				f.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		setMessage("openBackpackPermError", "&cYou do not have permission to open this backpack.");
		setMessage("craftPermError", "&cYou do not have permission to craft this backpack.");
		setMessage("contentsDestroyed", "&cYour backpack contents were destroyed in your death.");
		setMessage("backpackDoesNotExist", "&cThis backpack does not exist.");
		setMessage("playerDoesNotExist", "&cThe player does not exist or is offline.");
		setMessage("noPermission", "&cYou do not have permission.");
		setMessage("notPurchasable", "&cYou can not purchase this backpack.");
		setupLists();
		File userdata = new File(getDataFolder()+File.separator+"userdata");
		if (!userdata.exists()) {
			userdata.mkdirs();
		}
		setup();
		getLogger().info("Realistic Backpacks has been enabled.");
	}

	@Override
	public void onDisable() {
		HandlerList.unregisterAll(this);
		getServer().getPluginManager().disablePlugin(this);
		getLogger().info("Realistic Backpacks has been disabled.");
	}

	@SuppressWarnings("deprecation")
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if(cmd.getName().equalsIgnoreCase("rb")) {
			if (args.length >= 1) {
				if (args[0].equalsIgnoreCase("reload")) {
					if (!sender.hasPermission("rb.reload")) {
						sender.sendMessage(ChatColor.translateAlternateColorCodes('&', messageData.get("noPermission")));
						return false;
					}
					Long first = System.currentTimeMillis();
					reloadConfig();
					setupLists();
					getServer().resetRecipes();
					setup();
					sender.sendMessage(ChatColor.GRAY + "Config reloaded.");
					sender.sendMessage(ChatColor.GRAY + "Took " + ChatColor.YELLOW + (System.currentTimeMillis() - first) + "ms" + ChatColor.GRAY + ".");
					return true;
				} else if (args[0].equalsIgnoreCase("buy") || args[0].equalsIgnoreCase("purchase")) {
					if (!vault) {
						sender.sendMessage(ChatColor.RED + "This command is disabled due to Vault not being installed.");
						return false;
					}
					if (!(sender instanceof Player)) {
						sender.sendMessage(ChatColor.RED + "This command can only be used by a player.");
						return false;
					}
					String backpack = args[1];
					if (!backpacks.contains(backpack)) {
						sender.sendMessage(ChatColor.translateAlternateColorCodes('&', messageData.get("backpackDoesNotExist")));
						return false;
					}
					if (!backpackData.get(backpack).get(13).equalsIgnoreCase("true")) {
						sender.sendMessage(ChatColor.translateAlternateColorCodes('&', messageData.get("notPurchasable")));
						return false;
					}
					if (!sender.hasPermission("rb."+backpack+".buy")) {
						sender.sendMessage(ChatColor.translateAlternateColorCodes('&', messageData.get("noPermission")));
						return false;
					}
					if (!(args.length == 2)) {
						sender.sendMessage(ChatColor.RED + "Incorrect syntax. Please use:"+ ChatColor.GRAY +" /rb buy <backpack>");
						return false;
					}
					double price = Double.parseDouble(backpackData.get(backpack).get(14));
					if (econ.getBalance(sender.getName()) < price) {
						sender.sendMessage(ChatColor.RED + "You can not afford "+ChatColor.GOLD+price+ChatColor.RED+" to purchase this backpack.");
						return false;
					}
					Player p = (Player) sender;
					Inventory inv = p.getInventory();
					if (inv.firstEmpty() != -1) {
						econ.withdrawPlayer(p.getName(), price);
						inv.addItem(backpackItems.get(backpack));
						p.updateInventory();
						sender.sendMessage(ChatColor.GREEN+"You have purchased the "+ChatColor.GOLD+backpack+ChatColor.GREEN+" backpack for "+ChatColor.GOLD+price);
						return true;
					} else {
						sender.sendMessage(ChatColor.RED + "Your inventory is full.");
						return false;
					}
				} else if (args[0].equalsIgnoreCase("list")) {
					if (!sender.hasPermission("rb.list")) {
						sender.sendMessage(ChatColor.translateAlternateColorCodes('&', messageData.get("noPermission")));
						return false;
					}
					sender.sendMessage(ChatColor.LIGHT_PURPLE+"  Name  "+ChatColor.GOLD+"|"+ChatColor.AQUA+"  Size  "+ChatColor.GOLD+"|"+ChatColor.GREEN+"  Price  ");
					sender.sendMessage(ChatColor.GOLD+"-----------------------------------");
					for (String backpack : backpacks) {
						List<String> key = backpackData.get(backpack);
						if (backpackData.get(backpack).get(13).equalsIgnoreCase("true")) {
							sender.sendMessage(ChatColor.LIGHT_PURPLE+backpack+ChatColor.GOLD+" | "+ChatColor.AQUA+key.get(0)+ChatColor.GOLD+" | "+ChatColor.GREEN+Double.parseDouble(key.get(14)));	
						} else {
							sender.sendMessage(ChatColor.LIGHT_PURPLE+backpack+ChatColor.GOLD+" | "+ChatColor.AQUA+key.get(0)+ChatColor.GOLD+" | "+ChatColor.GREEN+"Not Purchasable");	
						}
					}
				} else if (args[0].equalsIgnoreCase("give")) {
					String backpack = args[2];
					if (!backpacks.contains(backpack)) {
						sender.sendMessage(ChatColor.translateAlternateColorCodes('&', messageData.get("backpackDoesNotExist")));
						return false;
					}
					if (!sender.hasPermission("rb."+backpack+".give")) {
						sender.sendMessage(ChatColor.translateAlternateColorCodes('&', messageData.get("noPermission")));
						return false;
					}
					if (!(args.length == 3)) {
						sender.sendMessage(ChatColor.RED + "Incorrect syntax. Please use:"+ ChatColor.GRAY +" /rb give <player> <backpack>");
						return false;
					}
					Player other = getServer().getPlayer(args[1]);
					if (other == null) {
						sender.sendMessage(ChatColor.translateAlternateColorCodes('&', messageData.get("playerDoesNotExist")));
						return false;
					}
					Inventory inv = other.getInventory();
					if (inv.firstEmpty() != -1) {
						inv.addItem(backpackItems.get(backpack));
						other.updateInventory();
						sender.sendMessage(ChatColor.GREEN+"You have given the "+ChatColor.GOLD+backpack+ChatColor.GREEN+" backpack to "+ChatColor.GOLD+other.getName());
						return true;
					} else {
						sender.sendMessage(ChatColor.RED + "Your inventory is full.");
						return false;
					}
				}
			}
		}
		return false;
	}
	
	private boolean setupEconomy() {
		if (getServer().getPluginManager().getPlugin("Vault") == null) {
			return false;
		}
		RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
		if (rsp == null) {
			return false;
		}
		econ = rsp.getProvider();
		return econ != null;
	}
	
	private void setMessage(String name, String message) {
		File f = new File(getDataFolder()+File.separator+"messages.yml");
		FileConfiguration config = YamlConfiguration.loadConfiguration(f);
		if (!config.isSet(name)) {
			config.set(name, message);
			try {
				config.save(f);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void setupLists() {
		backpacks.clear();
		backpackRecipe.clear();
		backpackData.clear();
		backpackLore.clear();
		for (String backpack : getConfig().getConfigurationSection("Backpacks").getKeys(false)) {
			List<String> list = new ArrayList<String>();
			backpacks.add(backpack);
			list.add(0, getConfig().getString("Backpacks."+backpack+".Size"));
			list.add(1, getConfig().getString("Backpacks."+backpack+".UseRecipe"));
			if (getConfig().getStringList("Backpacks."+backpack+".Recipe") != null) {
				backpackRecipe.put(backpack, getConfig().getStringList("Backpacks."+backpack+".Recipe"));
			} else {
				backpackRecipe.put(backpack, null);
			}
			list.add(2, getConfig().getString("Backpacks."+backpack+".BackpackItem.id"));
			list.add(3, getConfig().getString("Backpacks."+backpack+".BackpackItem.name"));
			if (getConfig().getStringList("Backpacks."+backpack+".BackpackItem.lore") != null) {
				backpackLore.put(backpack, getConfig().getStringList("Backpacks."+backpack+".BackpackItem.lore"));
			} else {
				backpackLore.put(backpack, null);
			}
			list.add(4, getConfig().getString("Backpacks."+backpack+".onDeath.destroyContents"));
			list.add(5, getConfig().getString("Backpacks."+backpack+".onDeath.dropContents"));
			list.add(6, getConfig().getString("Backpacks."+backpack+".onDeath.dropBackpack"));
			list.add(7, getConfig().getString("Backpacks."+backpack+".onDeath.keepBackpack"));
			list.add(8, getConfig().getString("Backpacks."+backpack+".WalkSpeedFeature.enabled"));
			list.add(9, getConfig().getString("Backpacks."+backpack+".WalkSpeedFeature.walkingSpeedMultiplier"));
			list.add(10, getConfig().getString("Backpacks."+backpack+".IncreasedHungerFeature.enabled"));
			list.add(11, getConfig().getString("Backpacks."+backpack+".IncreasedHungerFeature.extraHungerBarsToDeplete"));
			list.add(12, getConfig().getString("Backpacks."+backpack+".IncreasedHungerFeature.hungerBarsToSubtractWhenEating"));
			list.add(13, getConfig().getString("Backpacks."+backpack+".Purchasable"));
			list.add(14, getConfig().getString("Backpacks."+backpack+".Price"));
			backpackData.put(backpack, list);
		}
		File f = new File(getDataFolder()+File.separator+"messages.yml");
		FileConfiguration config = YamlConfiguration.loadConfiguration(f);
		for (String message : config.getConfigurationSection("").getKeys(false)) {
			messageData.put(message, config.getString(message));
		}
	}

	private void setup() {
		if (!getConfig().isSet("Config.MultipleBackpacksInInventory.average")) {
			average = false;
		} else {
			average = getConfig().getBoolean("Config.MultipleBackpacksInInventory.average");	
		}
		if (!getConfig().isSet("Config.MultipleBackpacksInInventory.add")) {
			add = false;
		} else {
			add = getConfig().getBoolean("Config.MultipleBackpacksInInventory.add");
		}
		for (String backpack : backpacks) {			
			List<String> key = backpackData.get(backpack);
			String backpackitem = key.get(2);
			String[] backpackitemSplit = backpackitem.split(":");
			Material baseItem;
			ItemStack backpackItemData;			
			if (backpackitemSplit.length > 1) {
				baseItem = Material.getMaterial(Integer.parseInt(backpackitemSplit[0]));
				backpackItemData = new ItemStack(baseItem, 1, (byte)Integer.parseInt(backpackitemSplit[1]));
				backpackItems.put(backpack, getConfigLore(backpackItemData, backpack));
			} else {
				backpackItems.put(backpack, getConfigLore(new ItemStack(Material.getMaterial(Integer.parseInt(backpackitemSplit[0]))), backpack));
			}
			ShapedRecipe recipe = null;
			if (key.get(1).equalsIgnoreCase("true")) {
				if (backpackitemSplit.length > 1) {
					baseItem = Material.getMaterial(Integer.parseInt(backpackitemSplit[0]));
					backpackItemData = new ItemStack(baseItem, 1, (byte)Integer.parseInt(backpackitemSplit[1]));
					recipe = new ShapedRecipe(getConfigLore(backpackItemData, backpack));
				} else {
					recipe = new ShapedRecipe(getConfigLore(new ItemStack(Material.getMaterial(Integer.parseInt(backpackitemSplit[0]))), backpack));
				}			
				recipe.shape("abc", "def", "ghi");
				int i = 0;
				for (String s : backpackRecipe.get(backpack)) {
					String[] itemIds = s.split(",");
					char shapechar = 0;
					for (String itemid : itemIds) {
						i++;
						switch (i) {
						case 1: shapechar = 'a'; break;
						case 2: shapechar = 'b'; break;
						case 3: shapechar = 'c'; break;
						case 4: shapechar = 'd'; break;
						case 5: shapechar = 'e'; break;
						case 6: shapechar = 'f'; break;
						case 7: shapechar = 'g'; break;
						case 8: shapechar = 'h'; break;
						case 9: shapechar = 'i'; break;
						}
						String[] itemsplit = itemid.split(":");
						if (itemsplit[0].equals("0")) continue;
						if (itemsplit.length > 1) {
							Material baseblock = Material.getMaterial(Integer.parseInt(itemsplit[0]));
							MaterialData ingredient = new MaterialData(baseblock, (byte)Integer.parseInt(itemsplit[1]));
							recipe.setIngredient(shapechar, ingredient);
						} else {
							Material baseblock = Material.getMaterial(Integer.parseInt(itemsplit[0]));
							recipe.setIngredient(shapechar, baseblock);
						}
					}
				}
				getServer().addRecipe(recipe);
			}
		}
	}
	
	public ItemStack getConfigLore(ItemStack item, String backpack) {
		List<String> key = backpackData.get(backpack);
		ItemMeta meta = item.getItemMeta();
		ArrayList<String> lore = new ArrayList<String>();
		lore.clear();
		if (backpackLore.get(backpack) != null) {
			for (String s : backpackLore.get(backpack)) {
				lore.add(ChatColor.translateAlternateColorCodes('&', s));
			}
			meta.setLore(lore);
		}
		meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', key.get(3)));
		item.setItemMeta(meta);
		return item;
	}
	
	public static boolean isAveraging() {
		return average;
	}

	public static boolean isAdding() {
		return add;
	}

}
