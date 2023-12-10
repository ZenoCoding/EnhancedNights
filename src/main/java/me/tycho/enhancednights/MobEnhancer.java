package me.tycho.enhancednights;

import com.archyx.aureliumskills.api.AureliumAPI;
import com.archyx.aureliumskills.skills.Skills;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.world.ChunkLoadEvent;

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
        updateEntityLevel(event.getEntity());
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        for (Entity entity : event.getChunk().getEntities()) {
            if (entity instanceof LivingEntity) {
                updateEntityLevel((LivingEntity) entity);
            }
        }
    }

    private void updateEntityLevel(LivingEntity entity) {
        boolean isNight = plugin.getDayNightCycle().isNight();
        int averageLevel = 0;

        if (skillsEnabled && isNight) {
            int totalSkillLevels = entity.getLocation().getNearbyPlayers(plugin.getConfig().getInt("mob_skill_radius")).stream()
                    .mapToInt(player -> {
                        int playerSum = AureliumAPI.getSkillLevel(player, Skills.FIGHTING) +
                                AureliumAPI.getSkillLevel(player, Skills.ARCHERY) +
                                AureliumAPI.getSkillLevel(player, Skills.DEFENSE) +
                                AureliumAPI.getSkillLevel(player, Skills.SORCERY);
                        return playerSum / 4;
                    }).sum();

            int playerCount = entity.getLocation().getNearbyPlayers(plugin.getConfig().getInt("mob_skill_radius")).size();
            if (playerCount > 0) {
                averageLevel = totalSkillLevels / playerCount; // Calculate average skill level
            }
        }

        // Apply night-time buffs if it's night, regardless of skillsEnabled
        if (isNight) {
            applyNightTimeBuffs(entity, averageLevel);
            applyMoonPhaseEffects(entity);
            setDisplayName(entity, averageLevel);
        }
    }

    private void setDisplayName(LivingEntity entity, int additionalLevel) {
        entity.customName(Component.text()
                .color(NamedTextColor.GOLD)
                .content("Lvl " + additionalLevel)
                .append(Component.text().color(NamedTextColor.DARK_GRAY).content(". | "))
                .append(Component.text().color(NamedTextColor.WHITE).content(entity.getName()))
                .append(Component.text().color(NamedTextColor.DARK_GRAY).content(" | "))
                .append(Component.text().color(NamedTextColor.RED).content("❤ "))
                .append(Component.text().color(NamedTextColor.WHITE).content(String.valueOf(Math.round(entity.getHealth()))))
                .append(Component.text().color(NamedTextColor.DARK_GRAY).content("/"))
                .append(Component.text().color(NamedTextColor.WHITE).content(String.valueOf(Math.round(entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()))))
                .build()
        );
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

    }

    private void setAttribute(LivingEntity entity, Attribute attribute, double multiplier) {
        AttributeInstance attributeInstance = entity.getAttribute(attribute);
        if (attributeInstance != null) {
            double newAttributeValue = attributeInstance.getValue() * multiplier;
            attributeInstance.setBaseValue(newAttributeValue);
            if (attribute == Attribute.GENERIC_MAX_HEALTH) {
                entity.setHealth(newAttributeValue);
            }
        }
    }
    private void applyMoonPhaseEffects(LivingEntity entity) {
        int moonPhase = entity.getWorld().getMoonPhase().ordinal();
        double healthMultiplier, damageMultiplier, speedMultiplier;

        switch (moonPhase) {
            case 0:
                healthMultiplier = 1.2;
                damageMultiplier = 1.2;
                speedMultiplier = 1.2;
                break;
            case 1:
                healthMultiplier = 1.3;
                damageMultiplier = 1.3;
                speedMultiplier = 1.3;
                break;
            case 2:
                healthMultiplier = 1.4;
                damageMultiplier = 1.4;
                speedMultiplier = 1.4;
                break;
            case 3:
                healthMultiplier = 1.5;
                damageMultiplier = 1.5;
                speedMultiplier = 1.5;
                break;
            case 4:
                healthMultiplier = 1.6;
                damageMultiplier = 1.6;
                speedMultiplier = 1.6;
                break;
            case 5:
                healthMultiplier = 1.7;
                damageMultiplier = 1.7;
                speedMultiplier = 1.7;
                break;
            case 6:
                healthMultiplier = 1.8;
                damageMultiplier = 1.8;
                speedMultiplier = 1.8;
                break;
            case 7:
                // Reset the multipliers
                healthMultiplier = 1.0;
                damageMultiplier = 1.0;
                speedMultiplier = 1.0;
                break;
            default:
                return;
        }

        setAttribute(entity, Attribute.GENERIC_MAX_HEALTH, healthMultiplier);
        setAttribute(entity, Attribute.GENERIC_ATTACK_DAMAGE, damageMultiplier);
        setAttribute(entity, Attribute.GENERIC_MOVEMENT_SPEED, speedMultiplier);
    }

}
