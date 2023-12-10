package me.tycho.enhancednights;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Bed;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
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

        Stage s = Stage.getStage(Objects.requireNonNull(Bukkit.getWorld("world")).getTime());
        if (s != stage){
            changeStage(s);
        }
    }

    private void changeStage(Stage s) {
        this.stage = s;
        switch (s) {
            case DOOMSDAY:
                for (Player player : Bukkit.getServer().getOnlinePlayers()) {
                    applyEffectIfNotInMoonlight(player);
                }
                break;
            // Add more cases as needed
            // case OTHER_STAGE:
            //     // Do something for OTHER_STAGE
            //     break;
            default:
                for (Player player : Bukkit.getServer().getOnlinePlayers()) {
                    player.removePotionEffect(PotionEffectType.HUNGER);
                }
                break;
        }
    }

    private void applyEffectIfNotInMoonlight(Player player) {
        if (player.getLocation().getBlock().getLightFromBlocks() < 4) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, Integer.MAX_VALUE, 1));
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (this.stage == Stage.DOOMSDAY) {
            applyEffectIfNotInMoonlight(event.getPlayer());
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (this.stage == Stage.DOOMSDAY) {
            applyEffectIfNotInMoonlight(event.getPlayer());
        }
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
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerHeal(@NotNull EntityRegainHealthEvent event) {
        if (event.getRegainReason() == EntityRegainHealthEvent.RegainReason.SATIATED) {
            if (event.getEntity() instanceof Player player) {
                if (player.getWorld().getEnvironment() == World.Environment.NORMAL) {
                    if (isNight()) {
                        // The player should heal with a factor proportional to the light level
                        int light = player.getLocation().getBlock().getLightLevel();
                        event.setAmount(event.getAmount() * (light / 15.0) * EnhancedNights.instance.getConfig().getDouble("night_multiplier"));
                    } else {
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
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerSleep(PlayerInteractEvent event) {
        Set<Material> beds = Set.of(Material.BLACK_BED, Material.BLUE_BED, Material.BROWN_BED, Material.CYAN_BED, Material.GRAY_BED, Material.GREEN_BED, Material.LIGHT_BLUE_BED, Material.LIGHT_GRAY_BED, Material.LIME_BED, Material.MAGENTA_BED, Material.ORANGE_BED, Material.PINK_BED, Material.PURPLE_BED, Material.RED_BED, Material.WHITE_BED, Material.YELLOW_BED);

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null && beds.contains(event.getClickedBlock().getType())) {
            Block bedBlock = event.getClickedBlock();

            // Check if the clicked block is the foot of the bed and find the head
            if (bedBlock.getBlockData() instanceof Bed) {
                Bed bed = (Bed) bedBlock.getBlockData();
                if (bed.getPart() == Bed.Part.FOOT) {
                    bedBlock = bedBlock.getRelative(bed.getFacing());
                }
            }

            if (isNight() && bedBlock.getWorld().getEnvironment() == World.Environment.NORMAL) {
                event.setCancelled(true);

                // Remove the bed block to prevent item drop
                bedBlock.setType(Material.AIR);

                Block finalBedBlock = bedBlock;
                new BukkitRunnable() {
                    @Override
                    public void run() {

                        // Create explosion at the head of the bed
                        finalBedBlock.getWorld().createExplosion(finalBedBlock.getLocation(), 5, true, true);
                    }
                }.runTaskLater(EnhancedNights.instance, 1L); // 1 tick delay to allow death event to be called after

                // Track this explosion for custom death message
                List<UUID> uuids = bedBlock.getLocation().getNearbyPlayers(5).stream().map(Player::getUniqueId).toList();
                recentBedExplosions.addAll(uuids);

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        uuids.forEach(recentBedExplosions::remove);
                    }
                }.runTaskLater(EnhancedNights.instance, 100L); // 100 ticks delay (5 seconds)
            }
        }
    }

    /**
     * Handles custom death messages for bed explosions.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamage(PlayerDeathEvent event) {
        if (event.getEntity().getLastDamageCause() != null &&
                event.getEntity().getLastDamageCause().getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) {
            Player player = event.getEntity();
            if (recentBedExplosions.contains(player.getUniqueId())) {
                event.deathMessage(Component.text().content(player.getName() + " was killed by [Intentional Game Design]").build());
                recentBedExplosions.remove(player.getUniqueId());
            }
        }
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
        DAY(0, 12000, Component.text("Day").color(NamedTextColor.YELLOW), Component.text("☀").color(NamedTextColor.YELLOW)),
        SUNSET(12000, 13000, Component.text("Sunset").color(NamedTextColor.GOLD), Component.text("☀").color(NamedTextColor.GOLD)),

        TWILIGHT(13000, 17000, Component.text("Twilight").color(NamedTextColor.AQUA), Component.text("☾").color(NamedTextColor.AQUA)),
        MIDNIGHT(17000, 21000, Component.text("Midnight").color(TextColor.color(0, 45, 94)), Component.text("☾").color(TextColor.color(0, 60, 115))),
        DOOMSDAY(21000, 23000, Component.text("Doomsday").color(NamedTextColor.DARK_RED), Component.text("☾").color(NamedTextColor.RED)),
        SUNRISE(23000, 24000, Component.text("Sunrise").color(NamedTextColor.GOLD), Component.text("☀").color(NamedTextColor.GOLD)); // Minecraft day is 24000 ticks

        private final long startTime;
        private final long endTime;
        private final Component displayName;
        private final Component icon;

        Stage(long startTime, long endTime, Component displayName, Component icon) {
            this.startTime = startTime;
            this.endTime = endTime;
            this.displayName = displayName;
            this.icon = icon;
        }

        public static @Nullable Stage getStage(long time) {
            for (Stage stage : Stage.values()) {
                if (time >= stage.getStartTime() && time < stage.getEndTime()) {
                    return stage;
                }
            }
            // Handle wrap-around for a new day
            if (time >= 0 && time < DAY.getStartTime()) {
                return SUNRISE;
            }
            return null;
        }

        public long getStartTime() {
            return this.startTime;
        }

        public long getEndTime() {
            return this.endTime;
        }

        public Component getDisplayName() {
            return this.displayName;
        }

        public Component getIcon() {
            return this.icon;
        }
    }
}
