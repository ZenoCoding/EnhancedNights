package me.tycho.enhancednights;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NightExpansion extends PlaceholderExpansion {
    private final EnhancedNights plugin;

    public NightExpansion(EnhancedNights plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "nights";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Tycho";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        DayNightCycle dayNightCycle = EnhancedNights.instance.getDayNightCycle();
        switch (params.toLowerCase()) {
            case "is_day":
                return String.valueOf(dayNightCycle.isDay());
            case "is_night":
                return String.valueOf(dayNightCycle.isNight());
            case "stage":
                return dayNightCycle.getStage().name().toLowerCase().replace("_", " ");
            case "stage_display_name":
                return LegacyComponentSerializer.legacyAmpersand().serialize(dayNightCycle.getStage().getDisplayName());
            case "stage_icon":
                return LegacyComponentSerializer.legacyAmpersand().serialize(dayNightCycle.getStage().getIcon());
            default:
                return null;
        }
    }
}
