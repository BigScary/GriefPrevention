package me.ryanhamshire.GriefPrevention.Configuration;



import org.bukkit.Color;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.file.YamlConfigurationOptions;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * YamlConfiguration class that will allow data inheritance.
 */
public class InheritedConfiguration extends YamlConfiguration {
    /*
    We take a Parent and a Child Configuration in our constructor.
    When a setting is retrieved:
    if the setting exists in the child, return it.
    otherwise, retrieve it from the parent.
    Either way, cache for that specific setting which config it came from.

    When saving a setting,if there is a mapped configuration in SettingsMap, save to that;
    otherwise, save to the child.
     */
    private FileConfiguration ParentConfiguration = null;
    private FileConfiguration ChildConfiguration = null;
    private Map<String,FileConfiguration> SettingsMap = new HashMap<String,FileConfiguration>();
    public InheritedConfiguration(FileConfiguration Parent,FileConfiguration Child){
        ParentConfiguration = Parent;
        ChildConfiguration=Child;

    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    @Override
    public Set<String> getKeys(boolean deep) {
        return super.getKeys(deep);
    }

    @Override
    public Map<String, Object> getValues(boolean deep) {
        return super.getValues(deep);
    }

    @Override
    public boolean contains(String path) {
        return super.contains(path);
    }

    @Override
    public boolean isSet(String path) {
        return super.isSet(path);
    }

    @Override
    public String getCurrentPath() {
        return super.getCurrentPath();    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public String getName() {
        return super.getName();    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public Configuration getRoot() {
        return super.getRoot();    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void addDefault(String path, Object value) {
        super.addDefault(path, value);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public ConfigurationSection getDefaultSection() {
        return super.getDefaultSection();    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void set(String path, Object value) {
        super.set(path, value);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public Object get(String path) {
        return super.get(path);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public Object get(String path, Object def) {
        return super.get(path, def);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public ConfigurationSection createSection(String path) {
        return super.createSection(path);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public ConfigurationSection createSection(String path, Map<?, ?> map) {
        return super.createSection(path, map);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public String getString(String path) {
        return super.getString(path);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public String getString(String path, String def) {
        return super.getString(path, def);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public boolean isString(String path) {
        return super.isString(path);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public int getInt(String path) {
        return super.getInt(path);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public int getInt(String path, int def) {
        return super.getInt(path, def);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public boolean isInt(String path) {
        return super.isInt(path);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public boolean getBoolean(String path) {
        return super.getBoolean(path);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public boolean getBoolean(String path, boolean def) {
        return super.getBoolean(path, def);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public boolean isBoolean(String path) {
        return super.isBoolean(path);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public double getDouble(String path) {
        return super.getDouble(path);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public double getDouble(String path, double def) {
        return super.getDouble(path, def);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public boolean isDouble(String path) {
        return super.isDouble(path);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public long getLong(String path) {
        return super.getLong(path);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public long getLong(String path, long def) {
        return super.getLong(path, def);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public boolean isLong(String path) {
        return super.isLong(path);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public List<?> getList(String path) {
        return super.getList(path);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public List<?> getList(String path, List<?> def) {
        return super.getList(path, def);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public boolean isList(String path) {
        return super.isList(path);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public List<String> getStringList(String path) {
        return super.getStringList(path);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public List<Integer> getIntegerList(String path) {
        return super.getIntegerList(path);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public List<Boolean> getBooleanList(String path) {
        return super.getBooleanList(path);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public List<Double> getDoubleList(String path) {
        return super.getDoubleList(path);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public List<Float> getFloatList(String path) {
        return super.getFloatList(path);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public List<Long> getLongList(String path) {
        return super.getLongList(path);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public List<Byte> getByteList(String path) {
        return super.getByteList(path);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public List<Character> getCharacterList(String path) {
        return super.getCharacterList(path);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public List<Short> getShortList(String path) {
        return super.getShortList(path);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public List<Map<?, ?>> getMapList(String path) {
        return super.getMapList(path);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public Vector getVector(String path) {
        return super.getVector(path);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public Vector getVector(String path, Vector def) {
        return super.getVector(path, def);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public boolean isVector(String path) {
        return super.isVector(path);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public OfflinePlayer getOfflinePlayer(String path) {
        return super.getOfflinePlayer(path);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public OfflinePlayer getOfflinePlayer(String path, OfflinePlayer def) {
        return super.getOfflinePlayer(path, def);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public boolean isOfflinePlayer(String path) {
        return super.isOfflinePlayer(path);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public ItemStack getItemStack(String path) {
        return super.getItemStack(path);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public ItemStack getItemStack(String path, ItemStack def) {
        return super.getItemStack(path, def);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public boolean isItemStack(String path) {
        return super.isItemStack(path);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public Color getColor(String path) {
        return super.getColor(path);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public Color getColor(String path, Color def) {
        return super.getColor(path, def);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public boolean isColor(String path) {
        return super.isColor(path);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public ConfigurationSection getConfigurationSection(String path) {
        return super.getConfigurationSection(path);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public boolean isConfigurationSection(String path) {
        return super.isConfigurationSection(path);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    protected boolean isPrimitiveWrapper(Object input) {
        return super.isPrimitiveWrapper(input);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    protected Object getDefault(String path) {
        return super.getDefault(path);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    protected void mapChildrenKeys(Set<String> output, ConfigurationSection section, boolean deep) {
        super.mapChildrenKeys(output, section, deep);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    protected void mapChildrenValues(Map<String, Object> output, ConfigurationSection section, boolean deep) {
        super.mapChildrenValues(output, section, deep);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public String toString() {
        return super.toString();    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void addDefaults(Map<String, Object> defaults) {
        super.addDefaults(defaults);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void addDefaults(Configuration defaults) {
        super.addDefaults(defaults);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void setDefaults(Configuration defaults) {
        super.setDefaults(defaults);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public Configuration getDefaults() {
        return super.getDefaults();    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public ConfigurationSection getParent() {
        return super.getParent();    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void save(File file) throws IOException {
        super.save(file);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void save(String file) throws IOException {
        super.save(file);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public String saveToString() {
        return super.saveToString();    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void load(File file) throws FileNotFoundException, IOException, InvalidConfigurationException {
        super.load(file);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void load(InputStream stream) throws IOException, InvalidConfigurationException {
        super.load(stream);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void load(String file) throws FileNotFoundException, IOException, InvalidConfigurationException {
        super.load(file);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void loadFromString(String contents) throws InvalidConfigurationException {
        super.loadFromString(contents);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    protected void convertMapsToSections(Map<?, ?> input, ConfigurationSection section) {
        super.convertMapsToSections(input, section);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    protected String parseHeader(String input) {
        return super.parseHeader(input);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    protected String buildHeader() {
        return super.buildHeader();    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public YamlConfigurationOptions options() {
        return super.options();    //To change body of overridden methods use File | Settings | File Templates.
    }
}
