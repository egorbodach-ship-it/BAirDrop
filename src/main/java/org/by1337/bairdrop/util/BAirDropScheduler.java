package org.by1337.bairdrop.util;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.by1337.bairdrop.AirDrop;
import org.by1337.bairdrop.BAirDrop;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/**
 * Встроенный планировщик аирдропов.
 * Поддерживает группы (пулы) с взвешенным рандомом и авто-запуск по таймеру.
 * Не зависит от global-time. Конфигурируется через schedules.yml.
 */
public class BAirDropScheduler {

    public static class Group {
        final String name;
        final LinkedHashMap<String, Integer> weights = new LinkedHashMap<>();
        boolean scheduleEnabled = false;
        int interval = 3600;
        int minPlayers = 1;
        int remaining = 3600;

        Group(String name) {
            this.name = name;
        }
    }

    private final Map<String, Group> groups = new LinkedHashMap<>();
    private final Random random = new Random();
    private BukkitRunnable task;
    private boolean stop = false;

    public void load() {
        groups.clear();
        File file = new File(BAirDrop.getInstance().getDataFolder(), "schedules.yml");
        if (!file.exists()) {
            try {
                BAirDrop.getInstance().saveResource("schedules.yml", false);
            } catch (IllegalArgumentException e) {
                Message.warning("[BAirDropScheduler] schedules.yml не найден в jar");
            }
        }
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        ConfigurationSection groupsSec = cfg.getConfigurationSection("groups");
        if (groupsSec != null) {
            for (String groupName : groupsSec.getKeys(false)) {
                Group g = new Group(groupName);
                ConfigurationSection members = groupsSec.getConfigurationSection(groupName + ".members");
                if (members != null) {
                    for (String airId : members.getKeys(false)) {
                        g.weights.put(airId, Math.max(1, members.getInt(airId, 1)));
                    }
                }
                groups.put(groupName.toLowerCase(Locale.ROOT), g);
            }
        }

        ConfigurationSection schedSec = cfg.getConfigurationSection("schedules");
        if (schedSec != null) {
            for (String groupName : schedSec.getKeys(false)) {
                Group g = groups.get(groupName.toLowerCase(Locale.ROOT));
                if (g == null) {
                    Message.warning("[BAirDropScheduler] schedules: неизвестная группа '" + groupName + "'");
                    continue;
                }
                g.scheduleEnabled = schedSec.getBoolean(groupName + ".enabled", true);
                g.interval = Math.max(1, schedSec.getInt(groupName + ".interval", 3600));
                g.minPlayers = schedSec.getInt(groupName + ".min-players", 1);
                g.remaining = g.interval;
            }
        }
    }

    public void start() {
        stop = false;
        task = new BukkitRunnable() {
            @Override
            public void run() {
                if (stop) {
                    cancel();
                    return;
                }
                int online = Bukkit.getOnlinePlayers().size();
                for (Group g : groups.values()) {
                    if (!g.scheduleEnabled) continue;
                    if (online < g.minPlayers) continue;
                    g.remaining--;
                    if (g.remaining <= 0) {
                        g.remaining = g.interval;
                        try {
                            startWeightedRandom(g.name, null);
                        } catch (Throwable t) {
                            Message.error("[BAirDropScheduler] ошибка запуска группы " + g.name + ": " + t.getMessage());
                        }
                    }
                }
            }
        };
        task.runTaskTimer(BAirDrop.getInstance(), 20L, 20L);
    }

    public void stop() {
        stop = true;
        if (task != null) {
            try {
                task.cancel();
            } catch (Throwable ignored) {
            }
            task = null;
        }
    }

    public boolean hasGroup(String group) {
        return group != null && groups.containsKey(group.toLowerCase(Locale.ROOT));
    }

    /**
     * Запускает взвешенно-случайный аирдроп из группы.
     *
     * @return id запущенного аирдропа или null.
     */
    public String startWeightedRandom(String group, Player feedback) {
        Group g = groups.get(group == null ? "" : group.toLowerCase(Locale.ROOT));
        if (g == null) return null;

        List<String> ids = new ArrayList<>();
        List<Integer> weights = new ArrayList<>();
        int total = 0;
        for (Map.Entry<String, Integer> e : g.weights.entrySet()) {
            AirDrop air = BAirDrop.airDrops.get(e.getKey());
            if (air == null) continue;
            if (air.isAirDropStarted()) continue;
            int w = Math.max(1, e.getValue());
            ids.add(e.getKey());
            weights.add(w);
            total += w;
        }
        if (ids.isEmpty() || total <= 0) {
            Message.warning("[BAirDropScheduler] нет доступных аирдропов в группе '" + g.name + "'");
            return null;
        }
        int r = random.nextInt(total);
        String chosen = ids.get(ids.size() - 1);
        int cum = 0;
        for (int i = 0; i < ids.size(); i++) {
            cum += weights.get(i);
            if (r < cum) {
                chosen = ids.get(i);
                break;
            }
        }
        AirDrop air = BAirDrop.airDrops.get(chosen);
        air.startCommand(feedback);
        Message.debug("[BAirDropScheduler] группа '" + g.name + "' -> запущен '" + chosen + "'", LogLevel.LOW);
        return chosen;
    }

    public int getSecondsUntil(String group) {
        Group g = groups.get(group == null ? "" : group.toLowerCase(Locale.ROOT));
        if (g == null || !g.scheduleEnabled) return -1;
        return g.remaining;
    }

    public String getFormat(String group) {
        int s = getSecondsUntil(group);
        if (s < 0) return "--:--:--";
        return AirManager.getFormat(s);
    }
}
