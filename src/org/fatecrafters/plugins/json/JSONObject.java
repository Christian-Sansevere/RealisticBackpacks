package org.fatecrafters.plugins.json;

/*
 * Copyright (c) 2002 JSON.org
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * The Software shall be used for Good, not Evil.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

public class JSONObject {

	private static final int keyPoolSize = 100;

	@SuppressWarnings("rawtypes")
	private static HashMap keyPool = new HashMap(keyPoolSize);

	private static final class Null {

		@Override
		protected final Object clone() {
			return this;
		}

		@Override
		public boolean equals(final Object object) {
			return object == null || object == this;
		}

		@Override
		public String toString() {
			return "null";
		}
	}

	@SuppressWarnings("rawtypes")
	private final Map map;

	public static final Object NULL = new Null();

	@SuppressWarnings("rawtypes")
	public JSONObject() {
		this.map = new HashMap();
	}

	public JSONObject(final JSONObject jo, final String[] names) {
		this();
		for (int i = 0; i < names.length; i += 1) {
			try {
				this.putOnce(names[i], jo.opt(names[i]));
			} catch (final Exception ignore) {
			}
		}
	}

	public JSONObject(final JSONTokener x) throws JSONException {
		this();
		char c;
		String key;

		if (x.nextClean() != '{') {
			throw x.syntaxError("A JSONObject text must begin with '{'");
		}
		for (;;) {
			c = x.nextClean();
			switch (c) {
			case 0:
				throw x.syntaxError("A JSONObject text must end with '}'");
			case '}':
				return;
			default:
				x.back();
				key = x.nextValue().toString();
			}

			// The key is followed by ':'. We will also tolerate '=' or '=>'.

			c = x.nextClean();
			if (c == '=') {
				if (x.next() != '>') {
					x.back();
				}
			} else if (c != ':') {
				throw x.syntaxError("Expected a ':' after a key");
			}
			this.putOnce(key, x.nextValue());

			// Pairs are separated by ','. We will also tolerate ';'.

			switch (x.nextClean()) {
			case ';':
			case ',':
				if (x.nextClean() == '}') {
					return;
				}
				x.back();
				break;
			case '}':
				return;
			default:
				throw x.syntaxError("Expected a ',' or '}'");
			}
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public JSONObject(final Map map) {
		this.map = new HashMap();
		if (map != null) {
			final Iterator i = map.entrySet().iterator();
			while (i.hasNext()) {
				final Map.Entry e = (Map.Entry) i.next();
				final Object value = e.getValue();
				if (value != null) {
					this.map.put(e.getKey(), wrap(value));
				}
			}
		}
	}

	public JSONObject(final Object bean) {
		this();
		this.populateMap(bean);
	}

	@SuppressWarnings("rawtypes")
	public JSONObject(final Object object, final String names[]) {
		this();
		final Class c = object.getClass();
		for (int i = 0; i < names.length; i += 1) {
			final String name = names[i];
			try {
				this.putOpt(name, c.getField(name).get(object));
			} catch (final Exception ignore) {
			}
		}
	}

	public JSONObject(final String source) throws JSONException {
		this(new JSONTokener(source));
	}

	@SuppressWarnings("rawtypes")
	public JSONObject(final String baseName, final Locale locale) throws JSONException {
		this();
		final ResourceBundle bundle = ResourceBundle.getBundle(baseName, locale, Thread.currentThread().getContextClassLoader());

		// Iterate through the keys in the bundle.

		final Enumeration keys = bundle.getKeys();
		while (keys.hasMoreElements()) {
			final Object key = keys.nextElement();
			if (key instanceof String) {

				// Go through the path, ensuring that there is a nested JSONObject for each
				// segment except the last. Add the value using the last segment's name into
				// the deepest nested JSONObject.

				final String[] path = ((String) key).split("\\.");
				final int last = path.length - 1;
				JSONObject target = this;
				for (int i = 0; i < last; i += 1) {
					final String segment = path[i];
					JSONObject nextTarget = target.optJSONObject(segment);
					if (nextTarget == null) {
						nextTarget = new JSONObject();
						target.put(segment, nextTarget);
					}
					target = nextTarget;
				}
				target.put(path[last], bundle.getString((String) key));
			}
		}
	}

	public JSONObject accumulate(final String key, final Object value) throws JSONException {
		testValidity(value);
		final Object object = this.opt(key);
		if (object == null) {
			this.put(key, value instanceof JSONArray ? new JSONArray().put(value) : value);
		} else if (object instanceof JSONArray) {
			((JSONArray) object).put(value);
		} else {
			this.put(key, new JSONArray().put(object).put(value));
		}
		return this;
	}

	public JSONObject append(final String key, final Object value) throws JSONException {
		testValidity(value);
		final Object object = this.opt(key);
		if (object == null) {
			this.put(key, new JSONArray().put(value));
		} else if (object instanceof JSONArray) {
			this.put(key, ((JSONArray) object).put(value));
		} else {
			throw new JSONException("JSONObject[" + key + "] is not a JSONArray.");
		}
		return this;
	}

	public static String doubleToString(final double d) {
		if (Double.isInfinite(d) || Double.isNaN(d)) {
			return "null";
		}

		// Shave off trailing zeros and decimal point, if possible.

		String string = Double.toString(d);
		if (string.indexOf('.') > 0 && string.indexOf('e') < 0 && string.indexOf('E') < 0) {
			while (string.endsWith("0")) {
				string = string.substring(0, string.length() - 1);
			}
			if (string.endsWith(".")) {
				string = string.substring(0, string.length() - 1);
			}
		}
		return string;
	}

	public Object get(final String key) throws JSONException {
		if (key == null) {
			throw new JSONException("Null key.");
		}
		final Object object = this.opt(key);
		if (object == null) {
			throw new JSONException("JSONObject[" + quote(key) + "] not found.");
		}
		return object;
	}

	public boolean getBoolean(final String key) throws JSONException {
		final Object object = this.get(key);
		if (object.equals(Boolean.FALSE) || (object instanceof String && ((String) object).equalsIgnoreCase("false"))) {
			return false;
		} else if (object.equals(Boolean.TRUE) || (object instanceof String && ((String) object).equalsIgnoreCase("true"))) {
			return true;
		}
		throw new JSONException("JSONObject[" + quote(key) + "] is not a Boolean.");
	}

	public double getDouble(final String key) throws JSONException {
		final Object object = this.get(key);
		try {
			return object instanceof Number ? ((Number) object).doubleValue() : Double.parseDouble((String) object);
		} catch (final Exception e) {
			throw new JSONException("JSONObject[" + quote(key) + "] is not a number.");
		}
	}

	public int getInt(final String key) throws JSONException {
		final Object object = this.get(key);
		try {
			return object instanceof Number ? ((Number) object).intValue() : Integer.parseInt((String) object);
		} catch (final Exception e) {
			throw new JSONException("JSONObject[" + quote(key) + "] is not an int.");
		}
	}

	public JSONArray getJSONArray(final String key) throws JSONException {
		final Object object = this.get(key);
		if (object instanceof JSONArray) {
			return (JSONArray) object;
		}
		throw new JSONException("JSONObject[" + quote(key) + "] is not a JSONArray.");
	}

	public JSONObject getJSONObject(final String key) throws JSONException {
		final Object object = this.get(key);
		if (object instanceof JSONObject) {
			return (JSONObject) object;
		}
		throw new JSONException("JSONObject[" + quote(key) + "] is not a JSONObject.");
	}

	public long getLong(final String key) throws JSONException {
		final Object object = this.get(key);
		try {
			return object instanceof Number ? ((Number) object).longValue() : Long.parseLong((String) object);
		} catch (final Exception e) {
			throw new JSONException("JSONObject[" + quote(key) + "] is not a long.");
		}
	}

	public static String[] getNames(final JSONObject jo) {
		final int length = jo.length();
		if (length == 0) {
			return null;
		}
		@SuppressWarnings("rawtypes")
		final Iterator iterator = jo.keys();
		final String[] names = new String[length];
		int i = 0;
		while (iterator.hasNext()) {
			names[i] = (String) iterator.next();
			i += 1;
		}
		return names;
	}

	@SuppressWarnings("rawtypes")
	public static String[] getNames(final Object object) {
		if (object == null) {
			return null;
		}
		final Class klass = object.getClass();
		final Field[] fields = klass.getFields();
		final int length = fields.length;
		if (length == 0) {
			return null;
		}
		final String[] names = new String[length];
		for (int i = 0; i < length; i += 1) {
			names[i] = fields[i].getName();
		}
		return names;
	}

	public String getString(final String key) throws JSONException {
		final Object object = this.get(key);
		if (object instanceof String) {
			return (String) object;
		}
		throw new JSONException("JSONObject[" + quote(key) + "] not a string.");
	}

	public boolean has(final String key) {
		return this.map.containsKey(key);
	}

	public JSONObject increment(final String key) throws JSONException {
		final Object value = this.opt(key);
		if (value == null) {
			this.put(key, 1);
		} else if (value instanceof Integer) {
			this.put(key, ((Integer) value).intValue() + 1);
		} else if (value instanceof Long) {
			this.put(key, ((Long) value).longValue() + 1);
		} else if (value instanceof Double) {
			this.put(key, ((Double) value).doubleValue() + 1);
		} else if (value instanceof Float) {
			this.put(key, ((Float) value).floatValue() + 1);
		} else {
			throw new JSONException("Unable to increment [" + quote(key) + "].");
		}
		return this;
	}

	public boolean isNull(final String key) {
		return JSONObject.NULL.equals(this.opt(key));
	}

	@SuppressWarnings("rawtypes")
	public Iterator keys() {
		return this.keySet().iterator();
	}

	@SuppressWarnings("rawtypes")
	public Set keySet() {
		return this.map.keySet();
	}

	public int length() {
		return this.map.size();
	}

	public JSONArray names() {
		final JSONArray ja = new JSONArray();
		@SuppressWarnings("rawtypes")
		final Iterator keys = this.keys();
		while (keys.hasNext()) {
			ja.put(keys.next());
		}
		return ja.length() == 0 ? null : ja;
	}

	public static String numberToString(final Number number) throws JSONException {
		if (number == null) {
			throw new JSONException("Null pointer");
		}
		testValidity(number);

		// Shave off trailing zeros and decimal point, if possible.

		String string = number.toString();
		if (string.indexOf('.') > 0 && string.indexOf('e') < 0 && string.indexOf('E') < 0) {
			while (string.endsWith("0")) {
				string = string.substring(0, string.length() - 1);
			}
			if (string.endsWith(".")) {
				string = string.substring(0, string.length() - 1);
			}
		}
		return string;
	}

	public Object opt(final String key) {
		return key == null ? null : this.map.get(key);
	}

	public boolean optBoolean(final String key) {
		return this.optBoolean(key, false);
	}

	public boolean optBoolean(final String key, final boolean defaultValue) {
		try {
			return this.getBoolean(key);
		} catch (final Exception e) {
			return defaultValue;
		}
	}

	public double optDouble(final String key) {
		return this.optDouble(key, Double.NaN);
	}

	public double optDouble(final String key, final double defaultValue) {
		try {
			return this.getDouble(key);
		} catch (final Exception e) {
			return defaultValue;
		}
	}

	public int optInt(final String key) {
		return this.optInt(key, 0);
	}

	public int optInt(final String key, final int defaultValue) {
		try {
			return this.getInt(key);
		} catch (final Exception e) {
			return defaultValue;
		}
	}

	public JSONArray optJSONArray(final String key) {
		final Object o = this.opt(key);
		return o instanceof JSONArray ? (JSONArray) o : null;
	}

	public JSONObject optJSONObject(final String key) {
		final Object object = this.opt(key);
		return object instanceof JSONObject ? (JSONObject) object : null;
	}

	public long optLong(final String key) {
		return this.optLong(key, 0);
	}

	public long optLong(final String key, final long defaultValue) {
		try {
			return this.getLong(key);
		} catch (final Exception e) {
			return defaultValue;
		}
	}

	public String optString(final String key) {
		return this.optString(key, "");
	}

	public String optString(final String key, final String defaultValue) {
		final Object object = this.opt(key);
		return NULL.equals(object) ? defaultValue : object.toString();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void populateMap(final Object bean) {
		final Class klass = bean.getClass();

		// If klass is a System class then set includeSuperClass to false.

		final boolean includeSuperClass = klass.getClassLoader() != null;

		final Method[] methods = includeSuperClass ? klass.getMethods() : klass.getDeclaredMethods();
		for (int i = 0; i < methods.length; i += 1) {
			try {
				final Method method = methods[i];
				if (Modifier.isPublic(method.getModifiers())) {
					final String name = method.getName();
					String key = "";
					if (name.startsWith("get")) {
						if ("getClass".equals(name) || "getDeclaringClass".equals(name)) {
							key = "";
						} else {
							key = name.substring(3);
						}
					} else if (name.startsWith("is")) {
						key = name.substring(2);
					}
					if (key.length() > 0 && Character.isUpperCase(key.charAt(0)) && method.getParameterTypes().length == 0) {
						if (key.length() == 1) {
							key = key.toLowerCase();
						} else if (!Character.isUpperCase(key.charAt(1))) {
							key = key.substring(0, 1).toLowerCase() + key.substring(1);
						}

						final Object result = method.invoke(bean, (Object[]) null);
						if (result != null) {
							this.map.put(key, wrap(result));
						}
					}
				}
			} catch (final Exception ignore) {
			}
		}
	}

	public JSONObject put(final String key, final boolean value) throws JSONException {
		this.put(key, value ? Boolean.TRUE : Boolean.FALSE);
		return this;
	}

	@SuppressWarnings("rawtypes")
	public JSONObject put(final String key, final Collection value) throws JSONException {
		this.put(key, new JSONArray(value));
		return this;
	}

	public JSONObject put(final String key, final double value) throws JSONException {
		this.put(key, new Double(value));
		return this;
	}

	public JSONObject put(final String key, final int value) throws JSONException {
		this.put(key, new Integer(value));
		return this;
	}

	public JSONObject put(final String key, final long value) throws JSONException {
		this.put(key, new Long(value));
		return this;
	}

	public JSONObject put(final String key, @SuppressWarnings("rawtypes") final Map value) throws JSONException {
		this.put(key, new JSONObject(value));
		return this;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public JSONObject put(String key, final Object value) throws JSONException {
		String pooled;
		if (key == null) {
			throw new JSONException("Null key.");
		}
		if (value != null) {
			testValidity(value);
			pooled = (String) keyPool.get(key);
			if (pooled == null) {
				if (keyPool.size() >= keyPoolSize) {
					keyPool = new HashMap(keyPoolSize);
				}
				keyPool.put(key, key);
			} else {
				key = pooled;
			}
			this.map.put(key, value);
		} else {
			this.remove(key);
		}
		return this;
	}

	public JSONObject putOnce(final String key, final Object value) throws JSONException {
		if (key != null && value != null) {
			if (this.opt(key) != null) {
				throw new JSONException("Duplicate key \"" + key + "\"");
			}
			this.put(key, value);
		}
		return this;
	}

	public JSONObject putOpt(final String key, final Object value) throws JSONException {
		if (key != null && value != null) {
			this.put(key, value);
		}
		return this;
	}

	public static String quote(final String string) {
		final StringWriter sw = new StringWriter();
		synchronized (sw.getBuffer()) {
			try {
				return quote(string, sw).toString();
			} catch (final IOException ignored) {
				// will never happen - we are writing to a string writer
				return "";
			}
		}
	}

	public static Writer quote(final String string, final Writer w) throws IOException {
		if (string == null || string.length() == 0) {
			w.write("\"\"");
			return w;
		}

		char b;
		char c = 0;
		String hhhh;
		int i;
		final int len = string.length();

		w.write('"');
		for (i = 0; i < len; i += 1) {
			b = c;
			c = string.charAt(i);
			switch (c) {
			case '\\':
			case '"':
				w.write('\\');
				w.write(c);
				break;
			case '/':
				if (b == '<') {
					w.write('\\');
				}
				w.write(c);
				break;
			case '\b':
				w.write("\\b");
				break;
			case '\t':
				w.write("\\t");
				break;
			case '\n':
				w.write("\\n");
				break;
			case '\f':
				w.write("\\f");
				break;
			case '\r':
				w.write("\\r");
				break;
			default:
				if (c < ' ' || (c >= '\u0080' && c < '\u00a0') || (c >= '\u2000' && c < '\u2100')) {
					w.write("\\u");
					hhhh = Integer.toHexString(c);
					w.write("0000", 0, 4 - hhhh.length());
					w.write(hhhh);
				} else {
					w.write(c);
				}
			}
		}
		w.write('"');
		return w;
	}

	public Object remove(final String key) {
		return this.map.remove(key);
	}

	public static Object stringToValue(final String string) {
		Double d;
		if (string.equals("")) {
			return string;
		}
		if (string.equalsIgnoreCase("true")) {
			return Boolean.TRUE;
		}
		if (string.equalsIgnoreCase("false")) {
			return Boolean.FALSE;
		}
		if (string.equalsIgnoreCase("null")) {
			return JSONObject.NULL;
		}

		/*
		 * If it might be a number, try converting it.
		 * If a number cannot be produced, then the value will just
		 * be a string. Note that the plus and implied string
		 * conventions are non-standard. A JSON parser may accept
		 * non-JSON forms as long as it accepts all correct JSON forms.
		 */

		final char b = string.charAt(0);
		if ((b >= '0' && b <= '9') || b == '.' || b == '-' || b == '+') {
			try {
				if (string.indexOf('.') > -1 || string.indexOf('e') > -1 || string.indexOf('E') > -1) {
					d = Double.valueOf(string);
					if (!d.isInfinite() && !d.isNaN()) {
						return d;
					}
				} else {
					final Long myLong = new Long(string);
					if (myLong.longValue() == myLong.intValue()) {
						return new Integer(myLong.intValue());
					} else {
						return myLong;
					}
				}
			} catch (final Exception ignore) {
			}
		}
		return string;
	}

	public static void testValidity(final Object o) throws JSONException {
		if (o != null) {
			if (o instanceof Double) {
				if (((Double) o).isInfinite() || ((Double) o).isNaN()) {
					throw new JSONException("JSON does not allow non-finite numbers.");
				}
			} else if (o instanceof Float) {
				if (((Float) o).isInfinite() || ((Float) o).isNaN()) {
					throw new JSONException("JSON does not allow non-finite numbers.");
				}
			}
		}
	}

	public JSONArray toJSONArray(final JSONArray names) throws JSONException {
		if (names == null || names.length() == 0) {
			return null;
		}
		final JSONArray ja = new JSONArray();
		for (int i = 0; i < names.length(); i += 1) {
			ja.put(this.opt(names.getString(i)));
		}
		return ja;
	}

	@Override
	public String toString() {
		try {
			return this.toString(0);
		} catch (final Exception e) {
			return null;
		}
	}

	public String toString(final int indentFactor) throws JSONException {
		final StringWriter w = new StringWriter();
		synchronized (w.getBuffer()) {
			return this.write(w, indentFactor, 0).toString();
		}
	}

	@SuppressWarnings("rawtypes")
	public static String valueToString(final Object value) throws JSONException {
		if (value == null || value.equals(null)) {
			return "null";
		}
		if (value instanceof JSONString) {
			Object object;
			try {
				object = ((JSONString) value).toJSONString();
			} catch (final Exception e) {
				throw new JSONException(e);
			}
			if (object instanceof String) {
				return (String) object;
			}
			throw new JSONException("Bad value from toJSONString: " + object);
		}
		if (value instanceof Number) {
			return numberToString((Number) value);
		}
		if (value instanceof Boolean || value instanceof JSONObject || value instanceof JSONArray) {
			return value.toString();
		}
		if (value instanceof Map) {
			return new JSONObject((Map) value).toString();
		}
		if (value instanceof Collection) {
			return new JSONArray((Collection) value).toString();
		}
		if (value.getClass().isArray()) {
			return new JSONArray(value).toString();
		}
		return quote(value.toString());
	}

	@SuppressWarnings("rawtypes")
	public static Object wrap(final Object object) {
		try {
			if (object == null) {
				return NULL;
			}
			if (object instanceof JSONObject || object instanceof JSONArray || NULL.equals(object) || object instanceof JSONString || object instanceof Byte || object instanceof Character || object instanceof Short || object instanceof Integer || object instanceof Long || object instanceof Boolean || object instanceof Float || object instanceof Double || object instanceof String) {
				return object;
			}

			if (object instanceof Collection) {
				return new JSONArray((Collection) object);
			}
			if (object.getClass().isArray()) {
				return new JSONArray(object);
			}
			if (object instanceof Map) {
				return new JSONObject((Map) object);
			}
			final Package objectPackage = object.getClass().getPackage();
			final String objectPackageName = objectPackage != null ? objectPackage.getName() : "";
			if (objectPackageName.startsWith("java.") || objectPackageName.startsWith("javax.") || object.getClass().getClassLoader() == null) {
				return object.toString();
			}
			return new JSONObject(object);
		} catch (final Exception exception) {
			return null;
		}
	}

	public Writer write(final Writer writer) throws JSONException {
		return this.write(writer, 0, 0);
	}

	@SuppressWarnings("rawtypes")
	static final Writer writeValue(final Writer writer, final Object value, final int indentFactor, final int indent) throws JSONException, IOException {
		if (value == null || value.equals(null)) {
			writer.write("null");
		} else if (value instanceof JSONObject) {
			((JSONObject) value).write(writer, indentFactor, indent);
		} else if (value instanceof JSONArray) {
			((JSONArray) value).write(writer, indentFactor, indent);
		} else if (value instanceof Map) {
			new JSONObject((Map) value).write(writer, indentFactor, indent);
		} else if (value instanceof Collection) {
			new JSONArray((Collection) value).write(writer, indentFactor, indent);
		} else if (value.getClass().isArray()) {
			new JSONArray(value).write(writer, indentFactor, indent);
		} else if (value instanceof Number) {
			writer.write(numberToString((Number) value));
		} else if (value instanceof Boolean) {
			writer.write(value.toString());
		} else if (value instanceof JSONString) {
			Object o;
			try {
				o = ((JSONString) value).toJSONString();
			} catch (final Exception e) {
				throw new JSONException(e);
			}
			writer.write(o != null ? o.toString() : quote(value.toString()));
		} else {
			quote(value.toString(), writer);
		}
		return writer;
	}

	static final void indent(final Writer writer, final int indent) throws IOException {
		for (int i = 0; i < indent; i += 1) {
			writer.write(' ');
		}
	}

	Writer write(final Writer writer, final int indentFactor, final int indent) throws JSONException {
		try {
			boolean commanate = false;
			final int length = this.length();
			@SuppressWarnings("rawtypes")
			final Iterator keys = this.keys();
			writer.write('{');

			if (length == 1) {
				final Object key = keys.next();
				writer.write(quote(key.toString()));
				writer.write(':');
				if (indentFactor > 0) {
					writer.write(' ');
				}
				writeValue(writer, this.map.get(key), indentFactor, indent);
			} else if (length != 0) {
				final int newindent = indent + indentFactor;
				while (keys.hasNext()) {
					final Object key = keys.next();
					if (commanate) {
						writer.write(',');
					}
					if (indentFactor > 0) {
						writer.write('\n');
					}
					indent(writer, newindent);
					writer.write(quote(key.toString()));
					writer.write(':');
					if (indentFactor > 0) {
						writer.write(' ');
					}
					writeValue(writer, this.map.get(key), indentFactor, newindent);
					commanate = true;
				}
				if (indentFactor > 0) {
					writer.write('\n');
				}
				indent(writer, indent);
			}
			writer.write('}');
			return writer;
		} catch (final IOException exception) {
			throw new JSONException(exception);
		}
	}
}
