package org.fatecrafters.plugins.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.fatecrafters.plugins.json.JSONArray;
import org.fatecrafters.plugins.json.JSONException;
import org.fatecrafters.plugins.json.JSONObject;

/**
 * Fancy JSON serialization mostly by evilmidget38.
 * 
 * @author evilmidget38, gomeow
 * 
 */
public class Serialization {

	@SuppressWarnings("unchecked")
	public static Map<String, Object> toMap(final JSONObject object) throws JSONException {
		final Map<String, Object> map = new HashMap<String, Object>();
		final Iterator<String> keys = object.keys();
		while (keys.hasNext()) {
			final String key = keys.next();
			map.put(key, fromJson(object.get(key)));
		}
		return map;
	}

	private static Object fromJson(final Object json) throws JSONException {
		if (json == JSONObject.NULL) {
			return null;
		} else if (json instanceof JSONObject) {
			return toMap((JSONObject) json);
		} else if (json instanceof JSONArray) {
			return toList((JSONArray) json);
		} else {
			return json;
		}
	}

	public static List<Object> toList(final JSONArray array) throws JSONException {
		final List<Object> list = new ArrayList<Object>();
		for (int i = 0; i < array.length(); i++) {
			list.add(fromJson(array.get(i)));
		}
		return list;
	}

	public static List<String> stringToList(final String listString) {
		return Arrays.asList(listString.split("<->"));
	}

	public static String listToString(final List<String> list) {
		String newString = null;
		for (final String s : list) {
			if (newString == null) {
				newString = s;
			} else {
				newString = newString + "<->" + s;
			}
		}
		return newString;
	}

	public static List<String> toString(final Inventory inv) {
		final List<String> result = new ArrayList<String>();
		final List<ConfigurationSerializable> items = new ArrayList<ConfigurationSerializable>();
		for (final ItemStack is : inv.getContents()) {
			items.add(is);
		}
		for (final ConfigurationSerializable cs : items) {
			if (cs == null) {
				result.add("null");
			} else {
				result.add(new JSONObject(serialize(cs)).toString());
			}
		}
		return result;
	}

	public static Inventory toInventory(final List<String> stringItems, final String name, final int size) {
		final Inventory inv = Bukkit.createInventory(null, size, ChatColor.translateAlternateColorCodes('&', name));
		final List<ItemStack> contents = new ArrayList<ItemStack>();
		for (final String piece : stringItems) {
			if (piece.equalsIgnoreCase("null")) {
				contents.add(null);
			} else {
				try {
					final ItemStack item = (ItemStack) deserialize(toMap(new JSONObject(piece)));
					contents.add(item);
				} catch (final JSONException e) {
					e.printStackTrace();
				}
			}
		}
		final ItemStack[] items = new ItemStack[contents.size()];
		for (int x = 0; x < contents.size(); x++) {
			items[x] = contents.get(x);
		}
		inv.setContents(items);
		return inv;
	}

	public static Map<String, Object> serialize(final ConfigurationSerializable cs) {
		final Map<String, Object> serialized = recreateMap(cs.serialize());
		for (final Entry<String, Object> entry : serialized.entrySet()) {
			if (entry.getValue() instanceof ConfigurationSerializable) {
				entry.setValue(serialize((ConfigurationSerializable) entry.getValue()));
			}
		}
		serialized.put(ConfigurationSerialization.SERIALIZED_TYPE_KEY, ConfigurationSerialization.getAlias(cs.getClass()));
		return serialized;
	}

	public static Map<String, Object> recreateMap(final Map<String, Object> original) {
		final Map<String, Object> map = new HashMap<String, Object>();
		for (final Entry<String, Object> entry : original.entrySet()) {
			map.put(entry.getKey(), entry.getValue());
		}
		return map;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static ConfigurationSerializable deserialize(final Map<String, Object> map) {
		for (final Entry<String, Object> entry : map.entrySet()) {
			if (entry.getValue() instanceof Map && ((Map) entry.getValue()).containsKey(ConfigurationSerialization.SERIALIZED_TYPE_KEY)) {
				entry.setValue(deserialize((Map) entry.getValue()));
			}
		}
		return ConfigurationSerialization.deserializeObject(map);
	}
}
