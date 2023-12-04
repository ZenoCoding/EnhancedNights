package me.tycho.enhancednights;

import com.archyx.aureliumskills.api.AureliumAPI;
import com.archyx.aureliumskills.skills.Skills;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
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
        // send mob damage before and after modification to nearbly player

        boolean isNight = plugin.getDayNightCycle().isNight();
        int averageLevel = 0;

        if (skillsEnabled && isNight) {
            // Calculate average skill level of nearby players if skills are enabled and it's night
            int totalSkillLevels = event.getLocation().getNearbyPlayers(plugin.getConfig().getInt("mob_skill_radius")).stream()
                    .mapToInt(player -> {
                        int playerSum = AureliumAPI.getSkillLevel(player, Skills.FIGHTING) +
                                AureliumAPI.getSkillLevel(player, Skills.ARCHERY) +
                                AureliumAPI.getSkillLevel(player, Skills.DEFENSE) +
                                AureliumAPI.getSkillLevel(player, Skills.SORCERY);
                        return playerSum / 4;
                    }).sum();

            int playerCount = event.getLocation().getNearbyPlayers(plugin.getConfig().getInt("mob_skill_radius")).size();
            if (playerCount > 0) {
                averageLevel = totalSkillLevels / playerCount; // Calculate average skill level
            }
        }

        // Apply night-time buffs if it's night, regardless of skillsEnabled
        if (isNight) {
            applyNightTimeBuffs(event.getEntity(), averageLevel);
        }
    }

    private void applyNightTimeBuffs(LivingEntity entity, int additionalLevel) {
        // Define the base multipliers for night-time buffs
        double healthMultiplier = 1.2; // Example value
        double damageMultiplier = 1.7; // Example value
        double speedMultiplier = 1.4; // Example value

        // Scale buffs further based on additional level (from players' skills)
        double scaledHealthMultiplier = healthMultiplier + (additionalLevel / 20.0);
        double scaledDamageMultiplier = damageMultiplier + (additionalLevel / 20.0);
        double scaledSpeedMultiplier = speedMultiplier + (Math.sqrt(additionalLevel) / 10.0);

        // Apply the buffs
        setAttribute(entity, Attribute.GENERIC_MAX_HEALTH, scaledHealthMultiplier);
        setAttribute(entity, Attribute.GENERIC_ATTACK_DAMAGE, scaledDamageMultiplier);
        setAttribute(entity, Attribute.GENERIC_MOVEMENT_SPEED, scaledSpeedMultiplier);

        // add display name
        entity.customName(Component.text()
                .content("Level " + additionalLevel + " ")
                .color(NamedTextColor.GRAY)
                .append(Component.text(entity.getName())).build()
        );
    }

    private void setAttribute(LivingEntity entity, Attribute attribute, double multiplier) {
        AttributeInstance attributeInstance = entity.getAttribute(attribute);
        if (attributeInstance != null) {
            double newAttributeValue = attributeInstance.getValue() * multiplier;
            attributeInstance.setBaseValue(newAttributeValue);
        }
    }

}
