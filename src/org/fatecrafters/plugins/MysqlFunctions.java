package org.fatecrafters.plugins;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.bukkit.inventory.Inventory;

public class MysqlFunctions {

	private static RealisticBackpacks plugin;

	public static void setPlugin(RealisticBackpacks plugin) {
		MysqlFunctions.plugin = plugin;
	}

	public static boolean checkIfTableExists(String table) {
		try {
			Connection conn = DriverManager.getConnection(plugin.getUrl(), plugin.getUser(), plugin.getPass());
			Statement state = conn.createStatement();
			DatabaseMetaData dbm = conn.getMetaData();
			ResultSet tables = dbm.getTables(null, null, "rb_data", null);
			state.close();
			conn.close();
			if (tables.next()) {
				return true;
			} else {
				return false;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	public static void createTables() {
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
			public void run() {
				try {
					Connection conn = DriverManager.getConnection(plugin.getUrl(), plugin.getUser(), plugin.getPass());
					PreparedStatement state = conn.prepareStatement("CREATE TABLE rb_data (player VARCHAR(16), backpack VARCHAR(20), inventory TEXT);");
					state.executeUpdate();
					state.close(); 
					conn.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		});
	}

	public static void addBackpackData(final String playerName, final String backpack, final Inventory inv) throws SQLException {
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
			public void run() {
				try {
					Connection conn = DriverManager.getConnection(plugin.getUrl(), plugin.getUser(), plugin.getPass());
					Statement statement = conn.createStatement();
					ResultSet res = statement.executeQuery("SELECT EXISTS(SELECT 1 FROM rb_data WHERE player = '"+playerName+"' AND backpack = '"+backpack+"' LIMIT 1);");
					PreparedStatement state = null;
					if (res.next()) {
						if (res.getInt(1) == 1) {
							state = conn.prepareStatement("UPDATE rb_data SET player='"+playerName+"', backpack='"+backpack+"', inventory='"+InventoryHandler.InventoryToString(inv)+"' WHERE player='"+playerName+"' AND backpack='"+backpack+"';");
						} else {
							state = conn.prepareStatement("INSERT INTO rb_data (player, backpack, inventory) VALUES('"+playerName+"', '"+backpack+"', '"+InventoryHandler.InventoryToString(inv)+"' );");
						}
					}
					state.executeUpdate();
					state.close();
					conn.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		});
	}

	public static Inventory getBackpackInv(final String playerName, final String backpack) throws SQLException {
		Inventory returnInv = null;
		try {
			Connection conn = DriverManager.getConnection(plugin.getUrl(), plugin.getUser(), plugin.getPass());
			Statement state = conn.createStatement();
			ResultSet res = state.executeQuery("SELECT inventory FROM rb_data WHERE player='"+playerName+"' AND backpack='"+backpack+"' LIMIT 1;");
			res.next();
			String invString = res.getString(1);
			if (!(invString == null)) {
				returnInv = InventoryHandler.StringToInventory(invString, RealisticBackpacks.backpackData.get(backpack).get(3));
			} else {
				returnInv = null;
			}
			state.close();
			conn.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return returnInv;
	}

	public static void delete(final String playerName, final String backpack) {
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
			public void run() {
				try {
					Connection conn = DriverManager.getConnection(plugin.getUrl(), plugin.getUser(), plugin.getPass());
					PreparedStatement state = conn.prepareStatement("DELETE FROM rb_data WHERE player = '"+playerName+"' AND backpack = '"+backpack+"';");
					state.executeUpdate();
					state.close();
					conn.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		});
	}

}
