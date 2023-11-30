package me.tycho.enhancednights;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class DayNightCycle implements Listener {

    private Stage stage;

    public DayNightCycle() {
        this.stage = Stage.getStage(Bukkit.getWorld("world").getTime());
        Bukkit.getServer().getPluginManager().registerEvents(this, EnhancedNights.instance);
        Bukkit.getScheduler().runTaskTimer(EnhancedNights.instance, this::update, 0, EnhancedNights.instance.getConfig().getInt("update_interval"));

    }

    public void update() {
        stage = Stage.getStage(Objects.requireNonNull(Bukkit.getWorld("world")).getTime());
    }

    public Stage getStage() {
        update();
        return this.stage;
    }

    /**
     * Listens for a player heal. If the source is natural regeneration, reduce it by the configured amount based on two factors
     * - The current stage of the day/night cycle
     * - The light level of the player
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerHeal(@NotNull EntityRegainHealthEvent event) {
        if (event.getRegainReason() == org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason.SATIATED) {
            if (event.getEntity() instanceof Player player) {
                if (player.getWorld().getEnvironment() == org.bukkit.World.Environment.NORMAL) {
                    if (isNight()) {
                        // The player should heal with a factor proportional to the light level
                        int light = player.getLocation().getBlock().getLightLevel();
                        event.setAmount(event.getAmount() * (light / 15.0) * EnhancedNights.instance.getConfig().getDouble("night_multiplier"));
                    } else if (isDay()) {
                        // The light level required for max healing should be less
                        int light = player.getLocation().getBlock().getLightLevel();
                        event.setAmount(event.getAmount() * Math.max(1.0, light / 10.0));
                    }
                }
            }
        }
    }


    private final Set<UUID> recentBedExplosions = new HashSet<>();

    /**
     * Makes it impossible to sleep during the night
     * There are small windows of time where it is possible to sleep during sunset
     */
    @EventHandler
    public void onPlayerSleep(PlayerInteractEvent event) {
        // List of all bed types
        Set<Material> beds = Set.of(Material.BLACK_BED, Material.BLUE_BED, Material.BROWN_BED, Material.CYAN_BED, Material.GRAY_BED, Material.GREEN_BED, Material.LIGHT_BLUE_BED, Material.LIGHT_GRAY_BED, Material.LIME_BED, Material.MAGENTA_BED, Material.ORANGE_BED, Material.PINK_BED, Material.PURPLE_BED, Material.RED_BED, Material.WHITE_BED, Material.YELLOW_BED);

        if (event.getClickedBlock() != null && beds.contains(event.getClickedBlock().getType())) {
            if (isNight() && event.getClickedBlock().getWorld().getEnvironment() == World.Environment.NORMAL) {
                event.setCancelled(true);
                // Create explosion
                event.getClickedBlock().getWorld().createExplosion(event.getClickedBlock().getLocation(), 5, true, true);
                // Track this explosion for custom death message
                List<UUID> uuids = event.getClickedBlock().getLocation().getNearbyPlayers(5).stream().map(Entity::getUniqueId).toList();
                recentBedExplosions.addAll(uuids);
                new BukkitRunnable(){
                    @Override
                    public void run() {
                        uuids.forEach(recentBedExplosions::remove);
                    }
                }.runTaskLater(EnhancedNights.instance, 5);
            }
        }
    }

    /**
     * Handles custom death messages for bed explosions.
     */
    @EventHandler
    public void onEntityDamage(PlayerDeathEvent event) {

        if (Objects.requireNonNull(event.getEntity().getLastDamageCause()).getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) {
            Player player = event.getEntity();
            if (recentBedExplosions.contains(player.getUniqueId())) {
                event.deathMessage(Component.text().content(player.getName() + " was killed by [Intentional Game Design]").build());
                // Remove the player from the set after handling
                recentBedExplosions.remove(player.getUniqueId());
            }
        }
        recentBedExplosions.remove(event.getEntity().getUniqueId());
    }


    /**
     * Determines if it is currently day
     * Note: !isDay() does not mean it is night, it could be sunset or sunrise
     * @return true if it is currently day
     */
    public boolean isDay() {
        return this.stage == Stage.DAY;
    }

    public boolean isNight() {
        return this.stage == Stage.TWILIGHT || this.stage == Stage.MIDNIGHT || this.stage == Stage.DOOMSDAY;
    }


    public enum Stage {
        DAY(0, Component.text("Day").color(NamedTextColor.YELLOW), Component.text("☀")),
        SUNSET(12000, Component.text("Sunset").color(NamedTextColor.GOLD), Component.text("☀")),
        TWILIGHT(13000, Component.text("Twilight").color(NamedTextColor.GOLD), Component.text("☀")),
        MIDNIGHT(18000, Component.text("Midnight").color(NamedTextColor.DARK_BLUE), Component.text("☾")),
        DOOMSDAY(21000, Component.text("Doomsday").color(NamedTextColor.DARK_RED), Component.text("☾")),
        SUNRISE(23000, Component.text("Sunrise").color(NamedTextColor.GOLD), Component.text("☾"));

        private final long startTime;
        private final Component displayName;
        private final Component icon;

        Stage(long startTime, Component displayName, Component icon) {
            this.startTime = startTime;
            this.displayName = displayName;
            this.icon = icon;
        }

        public long getStartTime() {
            return this.startTime;
        }

        public static @Nullable Stage getStage(long time) {
            for (Stage stage : Stage.values()) {
                if (time >= stage.getStartTime() && time < stage.getStartTime() + 12000) {
                    return stage;
                }
            }
            return null;
        }

        public Component getDisplayName() {
            return this.displayName;
        }

        public Component getIcon() {
            return this.icon;
        }
    }
}
