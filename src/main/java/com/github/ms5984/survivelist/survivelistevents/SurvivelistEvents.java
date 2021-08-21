/*
 * MIT License
 *
 * Copyright (c) 2021 Matt (ms5984) <https://github.com/ms5984>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.github.ms5984.survivelist.survivelistevents;

import com.github.ms5984.survivelist.survivelistevents.api.EventService;
import com.github.ms5984.survivelist.survivelistevents.api.ServerEvent;
import com.github.ms5984.survivelist.survivelistevents.api.exceptions.EventAlreadyRunningException;
import com.github.ms5984.survivelist.survivelistevents.commands.EventCommand;
import com.github.ms5984.survivelist.survivelistevents.commands.EventTpCommand;
import com.github.ms5984.survivelist.survivelistevents.model.EventItem;
import com.github.ms5984.survivelist.survivelistevents.api.Mode;
import com.github.ms5984.survivelist.survivelistevents.model.PlayerDataService;
import com.github.ms5984.survivelist.survivelistevents.model.SurvivelistServerEvent;
import com.github.ms5984.survivelist.survivelistevents.util.DataFile;
import com.github.ms5984.survivelist.survivelistevents.util.DataService;
import com.github.ms5984.survivelist.survivelistevents.util.TextLibrary;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * SurvivelistEvents plugin implementation.
 *
 * @since 1.0.0
 */
public final class SurvivelistEvents extends JavaPlugin implements EventService {
    public enum SpawnMode {
        LOCATION,
        TEAMS,
        ;
    }
    private static SurvivelistEvents instance;
    private SurvivelistServerEvent event;
    private final DataService dataService = new DataService();
    private DataFile dataFile;
    private Location eventLocation;
    private final Map<String, Location> teamLocations = new ConcurrentHashMap<>(8);
    private final Map<String, EventItem> eventItems = new ConcurrentHashMap<>();
    private final Map<String, Mode> modes = new ConcurrentHashMap<>();
    private PluginCommand eventCmd;
    private PluginCommand eventTpCmd;
    private String eventMode;

    @Override
    public void onEnable() {
        // Plugin startup logic
        instance = this;
        Permissions.registerSubDefaults();
        Permissions.setupManageStarNode();
        this.dataFile = new DataFile("event-data.yml");
        // load single location
        this.eventLocation = dataFile.getValueNow(fc -> fc.getLocation("location"));
        // load team locations
        dataFile.getValueNow(fc -> Optional.ofNullable(fc.getConfigurationSection("teams")).map(section -> section.getKeys(false)))
                .ifPresent(teams -> {
                    for (String team : teams) {
                        final Location teamLocation = dataFile.getValueNow(fc -> fc.getLocation("teams." + team));
                        if (teamLocation == null) continue;
                        teamLocations.put(team, teamLocation);
                    }
                });
        if (dataFile.getValue(fc -> fc.getString("status")).thenApply("active"::equals).join()) {
            this.event = new SurvivelistServerEvent(this);
        }
        // load items
        ConfigurationSerialization.registerClass(EventItem.class);
//        saveResource("items/salmon.yml", false);
        loadItems();
        saveDefaultConfig();
        this.eventMode = loadModesFromConfig();
        this.eventCmd = instance.getCommand("event");
        this.eventTpCmd = instance.getCommand("eventtp");
        final EventCommand eventCommand = new EventCommand(this);
        final EventTpCommand eventTpCommand = new EventTpCommand(this);
        eventCmd.setExecutor(eventCommand);
        eventCmd.setTabCompleter(eventCommand);
        eventTpCmd.setExecutor(eventTpCommand);
        eventTpCmd.setTabCompleter(eventTpCommand);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        PlayerDataService.clearCache(this);
    }

    @Override
    public @NotNull ServerEvent startEvent() throws EventAlreadyRunningException {
        if (event != null) throw new EventAlreadyRunningException(event, Messages.EVENT_RUNNING.toString());
        this.event = new SurvivelistServerEvent(this);
        dataFile.update(fc -> fc.set("status", "active")).whenComplete((v, e) -> dataFile.save());
        return event;
    }

    @Override
    public boolean endEvent() {
        if (event != null) {
            event.endEvent(this);
            event = null;
            dataFile.update(fc -> fc.set("status", null)).whenComplete((v, e) -> dataFile.save());
            return true;
        }
        return false;
    }

    @Override
    public @NotNull Optional<ServerEvent> getEvent() {
        return Optional.ofNullable(event);
    }

    @Override
    public @NotNull Optional<Location> getEventLocation() {
        return Optional.ofNullable(eventLocation);
    }

    @Override
    public void setEventLocation(Location location) {
        eventLocation = (location == null) ? null : location.clone();
        dataFile.update(fc -> fc.set("location", eventLocation)).whenComplete((n, e) -> dataFile.save());
    }

    // new features

    @Override
    public @NotNull Optional<Map<String, Location>> getTeamLocations() {
        return Optional.of(teamLocations)
                .filter(Predicate.not(Map::isEmpty))
                .map(ImmutableMap::copyOf);
    }

    @Override
    public void setTeamLocation(@NotNull String team, @Nullable Location teamLocation) throws IllegalArgumentException {
        if (team.contains(".")) throw new IllegalArgumentException("Team names cannot contain periods!");
        if (teamLocation == null) {
            synchronized (teamLocations) {
                teamLocations.remove(team);
            }
        } else {
            final Location copy = teamLocation.clone();
            synchronized (teamLocations) {
                this.teamLocations.put(team, copy);
            }
        }
        dataFile.update(fc -> fc.set("teams", teamLocations)).whenComplete((n, e) -> dataFile.save());
    }

    @Override
    public @NotNull Map<String, Mode> getAllModes() {
        return ImmutableMap.copyOf(modes);
    }

    @Override
    public @NotNull String getEventMode() {
        return eventMode;
    }

    @Override
    public boolean setEventMode(String eventMode) throws IllegalArgumentException {
        if (!modes.containsKey(eventMode)) throw new IllegalArgumentException("Invalid eventMode!");
        final Mode newMode = modes.get(eventMode);
        if (newMode == modes.get(this.eventMode)) return false;
        boolean ended = false;
        if (event != null) {
            ended = endEvent();
        }
        //set change after ending
        this.eventMode = eventMode;
        // Store set mode in datafile
        dataFile.update(fc -> fc.set("last-mode", this.eventMode)).whenComplete((n, e) -> dataFile.save());
        return ended;
    }

    @Override
    public @NotNull Map<String, EventItem> getEventItems() {
        return ImmutableMap.copyOf(eventItems);
    }

    private @NotNull String loadModesFromConfig() {
        final ConfigurationSection modesSection = getConfig().getConfigurationSection("modes");
        if (modesSection == null) throw new IllegalStateException("Unable to load valid modes!");
        for (String modeKey : modesSection.getKeys(false)) {
            final ConfigurationSection mode = modesSection.getConfigurationSection(modeKey);
            if (mode == null) continue;
            final List<String> spawnTypes = mode.getStringList("spawn");
            if (spawnTypes.isEmpty()) {
                getLogger().info("Skipping " + modeKey + " mode section: missing spawn parameters");
                continue;
            }
            final ImmutableList.Builder<SpawnMode> builder = new ImmutableList.Builder<>();
            for (String spawn : spawnTypes) {
                final SpawnMode spawnMode;
                try {
                    spawnMode = SpawnMode.valueOf(spawn.toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException ignored) {
                    continue;
                }
                builder.add(spawnMode);
            }
            final ImmutableList<SpawnMode> spawnModes = builder.build();
            if (spawnModes.isEmpty()) {
                getLogger().warning("Skipped " + modeKey + " mode section: no valid spawn parameters");
                continue;
            }
            final Set<String> items = ImmutableSet.copyOf(mode.getStringList("items"));
            this.modes.put(modeKey, new Mode() {
                final boolean usesLocation = spawnModes.contains(SpawnMode.LOCATION);
                final boolean usesTeams = spawnModes.contains(SpawnMode.TEAMS);

                @Override
                public boolean usesEventLocation() {
                    return usesLocation;
                }

                @Override
                public boolean usesTeamLocations() {
                    return usesTeams;
                }

                @Override
                public @NotNull Set<String> itemsToGivePlayers() {
                    return items;
                }
            });
        }
        final String lastMode = dataFile.getValueNow(fc -> fc.getString("last-mode"));
        if (lastMode != null) {
            return lastMode;
        }
        final String defaultMode = getConfig().getString("default-mode");
        if (defaultMode == null || !modes.containsKey(defaultMode)) {
            // select first as backup
            for (String s : modes.keySet()) {
                return s;
            }
            throw new IllegalStateException();
        }
        return defaultMode;
    }

    private void loadItems() {
        final ConfigurationSection modesSection = getConfig().getConfigurationSection("modes");
        if (modesSection == null) {
            synchronized (eventItems) {
                eventItems.clear();
            }
            return;
        }
        for (String key : modesSection.getKeys(false)) {
            final ConfigurationSection mode = getConfig().getConfigurationSection("modes." + key);
            if (mode == null) continue;
            for (String item : mode.getStringList("items")) {
                final File file = new File(getDataFolder(), "items/" + item + ".yml");
                if (!file.exists()) {
                    try {
                        saveResource("items/" + item + ".yml", false);
                    } catch (IllegalArgumentException ignored) {
                        continue;
                    }
                }
                final DataFile dataFile = new DataFile("items", item);
                final ImmutableMap<String, EventItem> valueNow = dataFile.getValueNow(fc -> {
                    final ImmutableMap.Builder<String, EventItem> builder = new ImmutableMap.Builder<>();
                    for (String configKey : fc.getKeys(false)) {
                        final EventItem eventItem = fc.getSerializable(configKey, EventItem.class);
                        if (eventItem != null) builder.put(configKey, eventItem);
                    }
                    return builder.build();
                });
                synchronized (eventItems) {
                    eventItems.putAll(valueNow);
                }
            }
        }
    }

    public static DataService getDataService() {
        return instance.dataService;
    }

    public enum Permissions {
        EVENT_COMMAND(() -> Optional.ofNullable(instance.eventCmd).map(PluginCommand::getPermission).orElse(null)),
        EVENTTP_COMMAND(() -> Optional.ofNullable(instance.eventTpCmd).map(PluginCommand::getPermission).orElse(null)),
        EVENT_JOIN(() -> instance.getConfig().getString("subcommand-info.event.join.permission")),
        EVENT_LEAVE(() -> instance.getConfig().getString("subcommand-info.event.leave.permission")),
        EVENT_SETHERE(() -> instance.getConfig().getString("subcommand-info.event.sethere.permission")),
        EVENT_START(() -> instance.getConfig().getString("subcommand-info.event.start.permission")),
        EVENT_END(() -> instance.getConfig().getString("subcommand-info.event.end.permission")),
        EVENT_SETTEAM(() -> instance.getConfig().getString("subcommand-info.event.setteam.permission")),
        EVENT_SETMODE(() -> instance.getConfig().getString("subcommand-info.event.setmode.permission")),
        ;
        private final Supplier<String> supplier;

        Permissions(Supplier<String> supplier) {
            this.supplier = supplier;
        }

        private static void registerSubDefaults() {
            final String eventJoinNode = EVENT_JOIN.getNode();
            if (eventJoinNode != null) {
                final Permission joinPerm = new Permission(eventJoinNode);
                joinPerm.setDefault(PermissionDefault.TRUE);
                Bukkit.getPluginManager().addPermission(joinPerm);
            }
            final String eventLeaveNode = EVENT_LEAVE.getNode();
            if (eventLeaveNode != null) {
                final Permission leavePerm = new Permission(eventLeaveNode);
                leavePerm.setDefault(PermissionDefault.TRUE);
                Bukkit.getPluginManager().addPermission(leavePerm);
            }
        }

        private static void setupManageStarNode() {
            final Permission manageStar = Bukkit.getPluginManager().getPermission("events.manage.*");
            if (manageStar == null) throw new IllegalStateException();
            final String setHereNode = EVENT_SETHERE.getNode();
            if (setHereNode != null) {
                final Permission setHerePerm = new Permission(setHereNode);
                setHerePerm.addParent(manageStar, true);
                Bukkit.getPluginManager().addPermission(setHerePerm);
            }
            final String startNode = EVENT_START.getNode();
            if (startNode != null) {
                final Permission startPerm = new Permission(startNode);
                startPerm.addParent(manageStar, true);
                Bukkit.getPluginManager().addPermission(startPerm);
            }
            final String endNode = EVENT_END.getNode();
            if (endNode != null) {
                final Permission endPerm = new Permission(endNode);
                endPerm.addParent(manageStar, true);
                Bukkit.getPluginManager().addPermission(endPerm);
            }
            final String setTeamNode = EVENT_SETTEAM.getNode();
            if (setTeamNode != null) {
                final Permission setTeamPerm = new Permission(setTeamNode);
                setTeamPerm.addParent(manageStar, true);
                Bukkit.getPluginManager().addPermission(setTeamPerm);
            }
            final String setModeNode = EVENT_SETMODE.getNode();
            if (setModeNode != null) {
                final Permission setModePerm = new Permission(setModeNode);
                setModePerm.addParent(manageStar, true);
                Bukkit.getPluginManager().addPermission(setModePerm);
            }
        }

        public @Nullable String getNode() {
            return supplier.get();
        }
    }

    public enum Messages {
        NO_PERMISSION("no-permission"),
        MUST_BE_PLAYER("player"),
        LOCATION_SET_("location-set"), // fields that end in _ generally call replacements
        /**
         * Replacements: 0 = team name, 1 = location string
         */
        TEAM_LOCATION_SET__("team-location-set"),
        PLEASE_EMPTY_INVENTORY("empty-inventory"),
        JOIN_MESSAGE_SELF("joining.self"),
        JOIN_ANNOUNCE_("joining.announce"),
        JOIN_ALREADY_IN("joining.already-in"),
        /**
         * Replacements: 0 = team name
         */
        JOIN_TEAM_("joining.team"),
        LEAVE_MESSAGE_SELF("leaving.self"),
        LEAVE_ANNOUNCE_("leaving.announce"),
        LEAVE_NOT_IN("leaving.not-in"),
        LEAVE_FORCE_END("leaving.force-end"),
        NO_EVENT("no-event"),
        EVENT_RUNNING("event-running"),
        FORCE_START("force-start"),
        EVENT_TP("event-tp"),
        REPLACED_("replaced"),
        ENDED("ended"),
        STARTED_("started"),
        NO_TEAMS("no-teams"),
        /**
         * Replacements: 0 = player name, 1 = team name
         */
        ASSIGNED__("assigned"),
        MODE_INVALID("mode.invalid"),
        MODE_CHANGE_STOP("mode.change-stop"),
        /**
         * Replacements: 0 = mode
         */
        MODE_SET_("mode.set"),
        ;

        private final String node;

        Messages(String node) {
            this.node = "messages." + node;
        }

        public @Nullable String get() {
            return instance.getConfig().getString(node);
        }

        public @NotNull String replace(Object... replacements) {
            String toString = toString();
            for (int i = 0; i < replacements.length; i++) {
                toString = toString.replace("{" + i + "}", String.valueOf(replacements[i]));
            }
            return toString;
        }

        @Override
        public String toString() {
            return TextLibrary.translate(get());
        }
    }
}
