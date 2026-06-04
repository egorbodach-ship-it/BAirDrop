package org.by1337.bairdrop.util;

import org.bukkit.OfflinePlayer;
import org.by1337.bairdrop.AirDrop;
import org.by1337.bairdrop.BAirDrop;
import org.jetbrains.annotations.NotNull;

public class PlaceholderHook extends me.clip.placeholderapi.expansion.PlaceholderExpansion {
    @Override
    public @NotNull String getAuthor() {
        return "By1337";
    }

    @Override
    public @NotNull String getIdentifier() {
        return "BAirDrop";
    }

    @Override
    public @NotNull String getVersion() {
        return BAirDrop.getInstance().getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        // --- глобальные / точные плейсхолдеры ---
        if (params.equals("time_start")) { // %bairdrop_time_start%
            if (BAirDrop.globalTimer == null)
                return AirManager.getTimeToNextAirdrop() + "";
            int time = 0;
            time += BAirDrop.globalTimer.getTimeToStart();
            if (BAirDrop.globalTimer.getAir() != null)
                time += BAirDrop.globalTimer.getAir().getTimeToStart();
            return time + "";
        }
        if (params.equals("time_start_format")) { // %bairdrop_time_start_format%
            if (BAirDrop.globalTimer == null)
                return AirManager.getFormat(AirManager.getTimeToNextAirdrop());
            int time = 0;
            time += BAirDrop.globalTimer.getTimeToStart();
            if (BAirDrop.globalTimer.getAir() != null)
                time += BAirDrop.globalTimer.getAir().getTimeToStart();
            return AirManager.getFormat(time);
        }
        if (params.equals("near")) { // %bairdrop_near%
            if (player == null) return "";
            AirDrop airDrop = null;
            int dist = 0;
            for (AirDrop air : BAirDrop.airDrops.values()) {
                if (!air.isAirDropStarted()) continue;
                if (!air.getAnyLoc().getWorld().equals(player.getPlayer().getWorld())) continue;
                if (dist > player.getPlayer().getLocation().distance(air.getAirDropLocation()) || airDrop == null) {
                    dist = (int) player.getPlayer().getLocation().distance(air.getAirDropLocation());
                    airDrop = air;
                }
            }
            if (airDrop == null)
                return BAirDrop.getConfigMessage().getMessage("air-near-none");
            return Message.messageBuilder(airDrop.replaceInternalPlaceholder(BAirDrop.getConfigMessage().getMessage("air-near").replace("{dist}", dist + "")));
        }

        // --- расписание встроенного планировщика ---
        // %bairdrop_schedule_<group>%        -> секунды до запуска
        // %bairdrop_schedule_<group>_format% -> HH:MM:SS
        if (params.startsWith("schedule_")) {
            String rest = params.substring("schedule_".length());
            boolean format = false;
            if (rest.endsWith("_format")) {
                format = true;
                rest = rest.substring(0, rest.length() - "_format".length());
            }
            if (BAirDrop.scheduler == null) return "error";
            if (!BAirDrop.scheduler.hasGroup(rest)) return "error";
            return format ? BAirDrop.scheduler.getFormat(rest)
                    : String.valueOf(BAirDrop.scheduler.getSecondsUntil(rest));
        }

        // --- плейсхолдеры по id (поддержка '_' в id) ---
        AirDrop airDrop;
        if (params.startsWith("is_start_")) {
            airDrop = BAirDrop.airDrops.get(params.substring("is_start_".length()));
            if (airDrop == null) return "error";
            return "" + airDrop.isAirDropStarted();
        }
        if (params.startsWith("is_locked_")) {
            airDrop = BAirDrop.airDrops.get(params.substring("is_locked_".length()));
            if (airDrop == null) return "error";
            if (!airDrop.isAirDropStarted())
                return BAirDrop.getConfigMessage().getMessage("air-no-respawn").replace("{id}", airDrop.getId());
            return "" + airDrop.isAirDropLocked();
        }
        if (params.startsWith("is_activated_")) {
            airDrop = BAirDrop.airDrops.get(params.substring("is_activated_".length()));
            if (airDrop == null) return "error";
            if (!airDrop.isAirDropStarted())
                return BAirDrop.getConfigMessage().getMessage("air-no-respawn").replace("{id}", airDrop.getId());
            return "" + airDrop.isActivated();
        }
        if (params.startsWith("air_name_")) {
            airDrop = BAirDrop.airDrops.get(params.substring("air_name_".length()));
            if (airDrop == null) return "error";
            return airDrop.getDisplayName();
        }
        if (params.startsWith("time_to_open_")) {
            airDrop = BAirDrop.airDrops.get(params.substring("time_to_open_".length()));
            if (airDrop == null) return "error";
            return airDrop.getTimeToOpen() + "";
        }
        if (params.startsWith("time_to_end_format_")) {
            airDrop = BAirDrop.airDrops.get(params.substring("time_to_end_format_".length()));
            if (airDrop == null) return "error";
            return AirManager.getFormat(airDrop.getTimeStop());
        }
        if (params.startsWith("time_to_start_new_format_")) {
            airDrop = BAirDrop.airDrops.get(params.substring("time_to_start_new_format_".length()));
            if (airDrop == null) return "error";
            return AirManager.formatTime(airDrop.getTimeToStart());
        }
        if (params.startsWith("time_to_start_format_")) {
            airDrop = BAirDrop.airDrops.get(params.substring("time_to_start_format_".length()));
            if (airDrop == null) return "error";
            return AirManager.getFormat(airDrop.getTimeToStart());
        }
        if (params.startsWith("time_to_end_")) {
            airDrop = BAirDrop.airDrops.get(params.substring("time_to_end_".length()));
            if (airDrop == null) return "error";
            return airDrop.getTimeStop() + "";
        }
        if (params.startsWith("time_to_start_")) {
            airDrop = BAirDrop.airDrops.get(params.substring("time_to_start_".length()));
            if (airDrop == null) return "error";
            return airDrop.getTimeToStart() + "";
        }
        if (params.startsWith("x_")) {
            airDrop = BAirDrop.airDrops.get(params.substring("x_".length()));
            if (airDrop == null) return "error";
            return airDrop.getAnyLoc() == null ? "?" : String.valueOf(airDrop.getAnyLoc().getX()).replace(".0", "");
        }
        if (params.startsWith("y_")) {
            airDrop = BAirDrop.airDrops.get(params.substring("y_".length()));
            if (airDrop == null) return "error";
            return airDrop.getAnyLoc() == null ? "?" : String.valueOf(airDrop.getAnyLoc().getY()).replace(".0", "");
        }
        if (params.startsWith("z_")) {
            airDrop = BAirDrop.airDrops.get(params.substring("z_".length()));
            if (airDrop == null) return "error";
            return airDrop.getAnyLoc() == null ? "?" : String.valueOf(airDrop.getAnyLoc().getZ()).replace(".0", "");
        }
        return null;
    }
}
