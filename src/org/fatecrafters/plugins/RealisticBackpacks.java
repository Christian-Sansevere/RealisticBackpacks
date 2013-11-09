package org.fatecrafters.plugins;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.MaterialData;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.fatecrafters.plugins.listeners.CraftListener;
import org.fatecrafters.plugins.listeners.EntityListener;
import org.fatecrafters.plugins.listeners.InventoryListener;
import org.fatecrafters.plugins.listeners.PlayerListener;
import org.fatecrafters.plugins.metrics.MetricsLite;
import org.fatecrafters.plugins.util.MysqlFunctions;
import org.fatecrafters.plugins.util.RBUtil;

public class RealisticBackpacks extends JavaPlugin {

	public static RBInterface NMS;

	public static Economy econ = null;

	private boolean usingMysql = false;
	private boolean vault = true;
	private boolean usingPermissions = true;
	private static boolean average = false;
	private static boolean add = false;
	private String user = null;
	private String password = null;
	private String url;

	public List<String> backpacks = new ArrayList<String>();
	public HashMap<String, String> messageData = new HashMap<String, String>();
	public HashMap<String, List<String>> backpackData = new HashMap<String, List<String>>();
	public HashMap<String, List<String>> backpackLore = new HashMap<String, List<String>>();
	public HashMap<String, List<String>> backpackRecipe = new HashMap<String, List<String>>();
	public HashMap<String, ItemStack> backpackItems = new HashMap<String, ItemStack>();
	public HashMap<String, ItemStack> backpackOverrides = new HashMap<String, ItemStack>();
	public HashMap<String, List<String>> backpackBlacklist = new HashMap<String, List<String>>();
	public HashMap<String, List<String>> backpackWhitelist = new HashMap<String, List<String>>();

	public HashMap<String, String> playerData = new HashMap<String, String>();
	public HashMap<String, String> adminFullView = new HashMap<String, String>();
	public List<String> adminRestrictedView = new ArrayList<String>();
	public List<String> slowedPlayers = new ArrayList<String>();

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
	 * 16 = UseWhitelist
	 * 17 = addGlow
	 */

	@Override
	public void onEnable() {
		try {
			MetricsLite metrics = new MetricsLite(this);
			metrics.start();
		} catch (IOException e) {
			getLogger().severe("Metrics failed to work.");
		}
		final String p = getServer().getClass().getPackage().getName();
		final String version = p.substring(p.lastIndexOf('.') + 1);
		try {
			String classname = null;
			if (version.contains("craftbukkit")) {
				classname = getClass().getPackage().getName() + ".versions.preVersioning";
			} else {
				classname = getClass().getPackage().getName() + ".versions." + version;
			}
			final Class<?> clazz = Class.forName(classname);
			final Constructor<?> cons = clazz.getDeclaredConstructor(getClass());
			final Object obj = cons.newInstance(this);
			if (obj instanceof RBInterface) {
				NMS = (RBInterface) obj;
			}
		} catch (final Exception e) {
			getLogger().severe("* ! * ! * !* ! * ! * ! * ! * !* ! * ! * ! *");
			getLogger().severe("This version of craftbukkit is not supported, please contact the developer stating this version: " + version);
			getLogger().severe("RealisticBackpacks will now disable.");
			getLogger().severe("* ! * ! * !* ! * ! * ! * ! * !* ! * ! * ! *");
			setEnabled(false);
		}
		if (isEnabled()) {
			saveDefaultConfig();
			MysqlFunctions.setMysqlFunc(this);
			RBUtil.setRBUtil(this);
			if (!setupEconomy()) {
				getLogger().info("Vault not found, economy features disabled.");
				vault = false;
			} else {
				getLogger().info("Vault found, economy features enabled.");
			}
			final File f = new File(getDataFolder() + File.separator + "messages.yml");
			if (!f.exists()) {
				try {
					f.createNewFile();
				} catch (final IOException e) {
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
			setMessage("listCommandNotBuyable", "&aNot Purchasable");
			setMessage("listCommandNoPermission", "&aInsufficient permissions to purchase");
			setMessage("cantPutItemInBackpack", "&cThis item can not go in this backpack!");
			setConfig("Data.FileSystem", "flatfile");
			setConfig("Data.MySQL.database", "minecraft");
			setConfig("Data.MySQL.username", "user");
			setConfig("Data.MySQL.password", "pass");
			setConfig("Data.MySQL.ip", "localhost");
			setConfig("Data.MySQL.port", 3306);
			setConfig("Config.usePermissions", true);
			saveConfig();
			reloadConfig();
			setupLists();
			final File userdata = new File(getDataFolder() + File.separator + "userdata");
			if (!userdata.exists()) {
				userdata.mkdirs();
			}
			setup();
			getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
			getServer().getPluginManager().registerEvents(new CraftListener(this), this);
			getServer().getPluginManager().registerEvents(new InventoryListener(this), this);
			getServer().getPluginManager().registerEvents(new EntityListener(this), this);
			getCommand("rb").setExecutor(new MainCommand(this));
			getLogger().info("Realistic Backpacks has been enabled.");
		}
	}

	@Override
	public void onDisable() {
		econ = null;
		NMS = null;
		HandlerList.unregisterAll(this);
		getServer().getPluginManager().disablePlugin(this);
		getLogger().info("Realistic Backpacks has been disabled.");
	}

	private boolean setupEconomy() {
		if (getServer().getPluginManager().getPlugin("Vault") == null) {
			return false;
		}
		final RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
		if (rsp == null) {
			return false;
		}
		econ = rsp.getProvider();
		return econ != null;
	}

	private void setMessage(final String name, final String message) {
		final File f = new File(getDataFolder() + File.separator + "messages.yml");
		final FileConfiguration config = YamlConfiguration.loadConfiguration(f);
		if (!config.isSet(name)) {
			config.set(name, message);
			try {
				config.save(f);
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void setConfig(final String path, final Object set) {
		if (!getConfig().isSet(path)) {
			getConfig().set(path, set);
		}
	}

	public void setupLists() {
		backpacks.clear();
		backpackRecipe.clear();
		backpackData.clear();
		backpackLore.clear();
		for (final String backpack : getConfig().getConfigurationSection("Backpacks").getKeys(false)) {
			final List<String> list = new ArrayList<String>();
			backpacks.add(backpack);
			list.add(0, getConfig().getString("Backpacks." + backpack + ".Size"));
			list.add(1, getConfig().getString("Backpacks." + backpack + ".UseRecipe"));
			if (getConfig().getStringList("Backpacks." + backpack + ".Recipe") != null) {
				backpackRecipe.put(backpack, getConfig().getStringList("Backpacks." + backpack + ".Recipe"));
			} else {
				backpackRecipe.put(backpack, null);
			}
			list.add(2, getConfig().getString("Backpacks." + backpack + ".BackpackItem.id"));
			list.add(3, getConfig().getString("Backpacks." + backpack + ".BackpackItem.name"));
			if (getConfig().getStringList("Backpacks." + backpack + ".BackpackItem.lore") != null) {
				backpackLore.put(backpack, getConfig().getStringList("Backpacks." + backpack + ".BackpackItem.lore"));
			} else {
				backpackLore.put(backpack, null);
			}
			list.add(4, getConfig().getString("Backpacks." + backpack + ".onDeath.destroyContents"));
			list.add(5, getConfig().getString("Backpacks." + backpack + ".onDeath.dropContents"));
			list.add(6, getConfig().getString("Backpacks." + backpack + ".onDeath.dropBackpack"));
			list.add(7, getConfig().getString("Backpacks." + backpack + ".onDeath.keepBackpack"));
			list.add(8, getConfig().getString("Backpacks." + backpack + ".WalkSpeedFeature.enabled"));
			list.add(9, getConfig().getString("Backpacks." + backpack + ".WalkSpeedFeature.walkingSpeed"));
			list.add(10, getConfig().getString("Backpacks." + backpack + ".IncreasedHungerFeature.enabled"));
			list.add(11, getConfig().getString("Backpacks." + backpack + ".IncreasedHungerFeature.extraHungerBarsToDeplete"));
			list.add(12, getConfig().getString("Backpacks." + backpack + ".IncreasedHungerFeature.hungerBarsToSubtractWhenEating"));
			list.add(13, getConfig().getString("Backpacks." + backpack + ".Purchasable"));
			list.add(14, getConfig().getString("Backpacks." + backpack + ".Price"));
			list.add(15, getConfig().getString("Backpacks." + backpack + ".OpenWith"));
			list.add(16, getConfig().getString("Backpacks." + backpack + ".UseWhitelist"));
			list.add(17, getConfig().getString("Backpacks." + backpack + ".addGlow"));
			backpackData.put(backpack, list);
			backpackBlacklist.put(backpack, getConfig().getStringList("Backpacks." + backpack + ".ItemBlacklist"));
			backpackWhitelist.put(backpack, getConfig().getStringList("Backpacks." + backpack + ".ItemWhitelist"));
		}
		final File f = new File(getDataFolder() + File.separator + "messages.yml");
		final FileConfiguration config = YamlConfiguration.loadConfiguration(f);
		for (final String message : config.getConfigurationSection("").getKeys(false)) {
			messageData.put(message, config.getString(message));
		}
	}

	public void setup() {
		user = getConfig().getString("Data.MySQL.username");
		password = getConfig().getString("Data.MySQL.password");
		url = "jdbc:mysql://" + getConfig().getString("Data.MySQL.ip") + ":" + getConfig().getInt("Data.MySQL.port") + "/" + getConfig().getString("Data.MySQL.database");

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

		if (!getConfig().isSet("Data.FileSystem")) {
			usingMysql = false;
		} else if (getConfig().getString("Data.FileSystem").equalsIgnoreCase("mysql") || getConfig().getString("Data.FileSystem").equalsIgnoreCase("sql")) {
			usingMysql = true;
			if (!MysqlFunctions.checkIfTableExists("rb_data")) {
				MysqlFunctions.createTables();
			}
		} else {
			usingMysql = false;
		}

		if (!getConfig().isSet("Config.usePermissions")) {
			usingPermissions = true;
		} else {
			usingPermissions = getConfig().getBoolean("Config.usePermissions");
		}

		for (final String backpack : backpacks) {

			final String override = getConfig().getString("Backpacks." + backpack + ".Override");
			if (override != null) {
				backpackOverrides.put(backpack, RBUtil.getItemstackFromString(override));
			} else {
				backpackOverrides.put(backpack, null);
			}

			final List<String> key = backpackData.get(backpack);
			final String backpackItem = key.get(2);
			backpackItems.put(backpack, getConfigLore(RBUtil.getItemstackFromString(backpackItem), backpack));

			ShapedRecipe recipe = null;
			if (key.get(1).equalsIgnoreCase("true")) {
				recipe = new ShapedRecipe(backpackItems.get(backpack));
				recipe.shape("abc", "def", "ghi");
				int i = 0;
				for (final String s : backpackRecipe.get(backpack)) {
					final String[] itemIds = s.split(",");
					char shapechar = 0;
					for (final String itemid : itemIds) {
						i++;
						switch (i) {
						case 1:
							shapechar = 'a';
							break;
						case 2:
							shapechar = 'b';
							break;
						case 3:
							shapechar = 'c';
							break;
						case 4:
							shapechar = 'd';
							break;
						case 5:
							shapechar = 'e';
							break;
						case 6:
							shapechar = 'f';
							break;
						case 7:
							shapechar = 'g';
							break;
						case 8:
							shapechar = 'h';
							break;
						case 9:
							shapechar = 'i';
							break;
						}
						final String[] itemsplit = itemid.split(":");
						if (itemsplit[0].equals("0")) {
							continue;
						}
						if (itemsplit.length > 1) {
							final Material baseblock = Material.getMaterial(Integer.parseInt(itemsplit[0]));
							final MaterialData ingredient = new MaterialData(baseblock, (byte) Integer.parseInt(itemsplit[1]));
							recipe.setIngredient(shapechar, ingredient);
						} else {
							final Material baseblock = Material.getMaterial(Integer.parseInt(itemsplit[0]));
							recipe.setIngredient(shapechar, baseblock);
						}
					}
				}
				getServer().addRecipe(recipe);
			}
		}
	}

	private ItemStack getConfigLore(final ItemStack item, final String backpack) {
		final List<String> key = backpackData.get(backpack);
		final ItemMeta meta = item.getItemMeta();
		final ArrayList<String> lore = new ArrayList<String>();
		lore.clear();
		if (backpackLore.get(backpack) != null) {
			for (final String s : backpackLore.get(backpack)) {
				lore.add(ChatColor.translateAlternateColorCodes('&', s));
			}
			meta.setLore(lore);
		}
		meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', key.get(3)));
		item.setItemMeta(meta);
		return item;
	}

	public boolean isAveraging() {
		return average;
	}

	public boolean isAdding() {
		return add;
	}

	public boolean isUsingMysql() {
		return usingMysql;
	}

	public boolean isUsingVault() {
		return vault;
	}

	public boolean isUsingPerms() {
		return usingPermissions;
	}

	public String getUser() {
		return user;
	}

	public String getPass() {
		return password;
	}

	public String getUrl() {
		return url;
	}

}
