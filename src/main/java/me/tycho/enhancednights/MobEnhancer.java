package me.tycho.enhancednights;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

public class MobEnhancer implements Listener {
    private final EnhancedNights plugin;
    private final boolean skillsEnabled;

    public MobEnhancer(EnhancedNights plugin){
        this.plugin = plugin;
        this.skillsEnabled = Bukkit.getPluginManager().getPlugin("AureliumSkills") != null;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }


    @EventHandler
    public void onMobSpawn(CreatureSpawnEvent event){
        if (skillsEnabled) {
            // add level to mobs based on average skill level in a 50 block radius
            // get players in 50 block radius
            
        }

        if (plugin.getDayNightCycle().isNight()){

        }
    }

}
