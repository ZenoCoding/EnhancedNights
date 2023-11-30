package me.tycho.enhancednights;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class EnhancedNights extends JavaPlugin {
    public static EnhancedNights instance;
    private DayNightCycle dayNightCycle;

    @Override
    public void onEnable() {
        // Plugin startup logic
        instance = this;
        dayNightCycle = new DayNightCycle();
        new MobEnhancer(this);

        // register placeholder expansion
        if(Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null)
            new NightExpansion(this).register();

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public DayNightCycle getDayNightCycle() {
        return dayNightCycle;
    }

}
