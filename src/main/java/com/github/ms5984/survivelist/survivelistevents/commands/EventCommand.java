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
package com.github.ms5984.survivelist.survivelistevents.commands;

import com.github.ms5984.survivelist.survivelistevents.SurvivelistEvents;
import com.github.ms5984.survivelist.survivelistevents.api.EventPlayer;
import com.github.ms5984.survivelist.survivelistevents.api.EventService;
import com.github.ms5984.survivelist.survivelistevents.api.Mode;
import com.github.ms5984.survivelist.survivelistevents.api.ServerEvent;
import com.github.ms5984.survivelist.survivelistevents.api.exceptions.AlreadyPresentPlayerException;
import com.github.ms5984.survivelist.survivelistevents.api.exceptions.EventAlreadyRunningException;
import com.github.ms5984.survivelist.survivelistevents.api.exceptions.InventoryNotClearPlayerException;
import com.github.ms5984.survivelist.survivelistevents.api.exceptions.NotPresentPlayerException;
import com.github.ms5984.survivelist.survivelistevents.model.EventItem;
import com.github.ms5984.survivelist.survivelistevents.util.LocationUtil;
import com.github.ms5984.survivelist.survivelistevents.util.TextLibrary;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Handles /event command and its subcommands.
 *
 * @since 1.0.0
 */
public class EventCommand implements TabExecutor {
    private final EventService eventService;
    private final ImmutableMap<String, String> helpMenu;
    private final ImmutableList<String> tabCompletions;
    private final TextComponent forceStartMessage;

    public EventCommand(SurvivelistEvents plugin) {
        this.eventService = plugin;
        final ImmutableMap.Builder<String, String> buildHelpMenu = new ImmutableMap.Builder<>();
        final ImmutableList.Builder<String> buildTabCompletions = new ImmutableList.Builder<>();
        final ConfigurationSection eventSection = plugin.getConfig().getConfigurationSection("subcommand-info.event");
        //noinspection ConstantConditions
        for (String key : eventSection.getKeys(false)) {
            buildTabCompletions.add(key);
            buildHelpMenu.put(key, eventSection.getString(key + ".description"));
        }
        this.helpMenu = buildHelpMenu.build();
        this.tabCompletions = buildTabCompletions.build();
        this.forceStartMessage = Component.text(SurvivelistEvents.Messages.FORCE_START.toString())
                .append(Component.space())
                .append(
                        Component.text("click here")
                                .clickEvent(ClickEvent.runCommand("/event start force"))
                                .color(NamedTextColor.DARK_GRAY)
                );
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!command.testPermission(sender)) return true;
        if (args.length < 1) {
            showHelpMenu(sender);
            return true;
        }
        // Process subcommands
        final Optional<Player> playerOptional = Optional.of(sender).filter(Player.class::isInstance).map(Player.class::cast);
        final Optional<ServerEvent> eventOptional = eventService.getEvent();
        if (args[0].equalsIgnoreCase("join")) {
            // Test permission
            if (!Optional.ofNullable(SurvivelistEvents.Permissions.EVENT_JOIN.getNode()).map(sender::hasPermission).orElse(false)) {
                sender.sendMessage(SurvivelistEvents.Messages.NO_PERMISSION.toString());
                return true;
            }
            // check for player
            if (playerOptional.isEmpty()) {
                // not player, send error
                sender.sendMessage(SurvivelistEvents.Messages.MUST_BE_PLAYER.toString());
                return true;
            }
            // store if present
            final Player player = (Player) sender;
            // If no event is running then message and exit
            if (eventOptional.isEmpty()) {
                sender.sendMessage(SurvivelistEvents.Messages.NO_EVENT.toString());
                return true;
            }
            // Get current event
            final ServerEvent serverEvent = eventOptional.get();
            // Join event
            final EventPlayer eventPlayer;
            try {
                eventPlayer = serverEvent.addPlayer(player);
            } catch (AlreadyPresentPlayerException | InventoryNotClearPlayerException e) {
                // Send message "you are already in the event" or "please empty your inventory"
                sender.sendMessage(e.getMessage());
                return true;
            }
            final Mode mode = eventService.getAllModes().get(eventService.getEventMode());
            if (mode == null) throw new IllegalStateException();
            if (mode.usesEventLocation()) {
                // Teleport to event location
                eventPlayer.teleportToEvent();
            } else if (mode.usesTeamLocations()) {
                // Teleport to a random team
                eventService.getTeams()
                        .map(ArrayList::new)
                        .map(list -> list.get(ThreadLocalRandom.current().nextInt(list.size())))
                        .ifPresent(team -> {
                            eventPlayer.teleportToTeamLocation(team);
                            player.sendMessage(SurvivelistEvents.Messages.JOIN_TEAM_.replace(team));
                        });
            }
            // Give items, if needed
            if (!mode.itemsToGivePlayers().isEmpty()) {
                final Map<String, EventItem> eventItems = eventService.getEventItems();
                for (String item : mode.itemsToGivePlayers()) {
                    Optional.ofNullable(eventItems.get(item))
                            .ifPresent(eventItem -> eventItem.giveToPlayer(player));
                }
            }
            player.sendMessage(SurvivelistEvents.Messages.JOIN_MESSAGE_SELF.toString());
            serverEvent.sendMessage(SurvivelistEvents.Messages.JOIN_ANNOUNCE_.replace(player.getName()), p -> p != player);
        } else if (args[0].equalsIgnoreCase("leave")) {
            // Test permission
            if (!Optional.ofNullable(SurvivelistEvents.Permissions.EVENT_LEAVE.getNode()).map(sender::hasPermission).orElse(false)) {
                sender.sendMessage(SurvivelistEvents.Messages.NO_PERMISSION.toString());
                return true;
            }
            // check for player
            if (playerOptional.isEmpty()) {
                // not player, send error
                sender.sendMessage(SurvivelistEvents.Messages.MUST_BE_PLAYER.toString());
                return true;
            }
            // store if present
            final Player player = (Player) sender;
            // If no event is running then message and exit
            if (eventOptional.isEmpty()) {
                sender.sendMessage(SurvivelistEvents.Messages.NO_EVENT.toString());
                return true;
            }
            // Get current event
            final ServerEvent serverEvent = eventOptional.get();
            // Leave event
            try {
                serverEvent.removePlayer(player);
            } catch (NotPresentPlayerException e) {
                // Send message "you are not in the event"
                sender.sendMessage(e.getMessage());
                return true;
            }
            player.sendMessage(SurvivelistEvents.Messages.LEAVE_MESSAGE_SELF.toString());
            serverEvent.sendMessage(SurvivelistEvents.Messages.LEAVE_ANNOUNCE_.replace(player.getName()), p -> p != player);
        } else if (args[0].equalsIgnoreCase("sethere")) {
            // Test permission
            if (!Optional.ofNullable(SurvivelistEvents.Permissions.EVENT_SETHERE.getNode()).map(sender::hasPermission).orElse(false)) {
                sender.sendMessage(SurvivelistEvents.Messages.NO_PERMISSION.toString());
                return true;
            }
            // check for player
            if (playerOptional.isEmpty()) {
                // not player, send error
                sender.sendMessage(SurvivelistEvents.Messages.MUST_BE_PLAYER.toString());
                return true;
            }
            // store if present
            final Player player = (Player) sender;
            // Set event location
            final Location location = player.getLocation();
            eventService.setEventLocation(location);
            player.sendMessage(SurvivelistEvents.Messages.LOCATION_SET_.replace(location));
        } else if (args[0].equalsIgnoreCase("start")) {
            // Test permission
            if (!Optional.ofNullable(SurvivelistEvents.Permissions.EVENT_START.getNode()).map(sender::hasPermission).orElse(false)) {
                sender.sendMessage(SurvivelistEvents.Messages.NO_PERMISSION.toString());
                return true;
            }
            // Start event
            final ServerEvent newEvent;
            try {
                newEvent = eventService.startEvent();
            } catch (EventAlreadyRunningException e) {
                // Send message "An event is in progress!"
                sender.sendMessage(TextLibrary.translate("&c&o" + e.getMessage()));
                if (args.length >= 2 && args[1].equalsIgnoreCase("force")) {
                    // Forcibly end previous event
                    if (!eventService.endEvent()) {
                        throw new IllegalStateException("Unable to end old event!");
                    }
                    final ServerEvent serverEvent;
                    try {
                        // Start new event
                        serverEvent = eventService.startEvent();
                    } catch (EventAlreadyRunningException eventAlreadyRunningException) {
                        throw new IllegalStateException("Unable to replace old event.", eventAlreadyRunningException);
                    }
                    // on success send successful replacement message
                    sender.sendMessage(SurvivelistEvents.Messages.REPLACED_.replace(serverEvent));
                } else {
                    // Send message about force param
                    playerOptional.ifPresentOrElse(p -> p.sendMessage(forceStartMessage),
                            () -> sender.sendMessage(SurvivelistEvents.Messages.FORCE_START.toString()));
                }
                return true;
            }
            sender.sendMessage(SurvivelistEvents.Messages.STARTED_.replace(newEvent));
        } else if (args[0].equalsIgnoreCase("end")) {
            // Test permission
            if (!Optional.ofNullable(SurvivelistEvents.Permissions.EVENT_END.getNode()).map(sender::hasPermission).orElse(false)) {
                sender.sendMessage(SurvivelistEvents.Messages.NO_PERMISSION.toString());
                return true;
            }
            // End event
            if (eventService.endEvent()) {
                // ended event successfully
                sender.sendMessage(SurvivelistEvents.Messages.ENDED.toString());
            } else {
                // no event
                sender.sendMessage(SurvivelistEvents.Messages.NO_EVENT.toString());
            }
        } else if (args[0].equalsIgnoreCase("setteam")) {
            // Test permission
            if (!Optional.ofNullable(SurvivelistEvents.Permissions.EVENT_SETTEAM.getNode()).map(sender::hasPermission).orElse(false)) {
                sender.sendMessage(SurvivelistEvents.Messages.NO_PERMISSION.toString());
                return true;
            }
            // check for player
            if (playerOptional.isEmpty()) {
                // not player, send error
                sender.sendMessage(SurvivelistEvents.Messages.MUST_BE_PLAYER.toString());
                return true;
            }
            // Check for second arg
            if (args.length < 2) {
                showHelpMenu(sender);
                return true;
            }
            // Set team location
            final Location location = ((Player) sender).getLocation();
            try {
                eventService.setTeamLocation(args[1], location);
            } catch (IllegalArgumentException e) {
                sender.sendMessage(e.getMessage());
                return true;
            }
            sender.sendMessage(SurvivelistEvents.Messages.TEAM_LOCATION_SET__.replace(args[1], LocationUtil.prettyPrintLocation(location)));
        } else if (args[0].equalsIgnoreCase("setmode")) {
            // Test permission
            if (!Optional.ofNullable(SurvivelistEvents.Permissions.EVENT_SETMODE.getNode()).map(sender::hasPermission).orElse(false)) {
                sender.sendMessage(SurvivelistEvents.Messages.NO_PERMISSION.toString());
                return true;
            }
            // Check for second arg
            if (args.length < 2) {
                showHelpMenu(sender);
                return true;
            }
            // Check validity + set mode
            if (!eventService.getAllModes().containsKey(args[1])) {
                // invalid mode
                sender.sendMessage(SurvivelistEvents.Messages.MODE_INVALID.toString());
                return true;
            }
            if (eventService.setEventMode(args[1])) {
                // changed mode, current event stopped
                sender.sendMessage(SurvivelistEvents.Messages.MODE_CHANGE_STOP.toString());
            }
            // mode set
            sender.sendMessage(SurvivelistEvents.Messages.MODE_SET_.replace(args[1]));
        } else {
            showHelpMenu(sender);
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (command.testPermissionSilent(sender)) {
            if (args.length == 1) {
                final ArrayList<String> completions = new ArrayList<>(tabCompletions);
                if (!Optional.ofNullable(SurvivelistEvents.Permissions.EVENT_SETHERE.getNode()).map(sender::hasPermission).orElse(false)) {
                    completions.remove("sethere");
                }
                for (String tabCompletion : tabCompletions) {
                    if (!tabCompletion.startsWith(args[0])) {
                        completions.remove(tabCompletion);
                    }
                }
                return completions;
            } else if (args.length == 2) {
                if (args[0].equalsIgnoreCase("start")) {
                    if ("force".startsWith(args[1])) {
                        return ImmutableList.of("force");
                    }
                } else if (args[0].equalsIgnoreCase("setteam")) {
                    final ArrayList<String> strings = eventService.getTeams().map(ArrayList::new).orElse(null);
                    if (strings != null) {
                        strings.removeIf(s -> !s.startsWith(args[1]));
                        return strings;
                    }
                } else if (args[0].equalsIgnoreCase("setmode")) {
                    final ArrayList<String> strings = new ArrayList<>(eventService.getAllModes().keySet());
                    if (!strings.isEmpty()) {
                        strings.removeIf(s -> !s.startsWith(args[1]));
                        return strings;
                    }
                }
            }
        }
        return ImmutableList.of();
    }

    private void showHelpMenu(CommandSender sender) {
        // Show help menu
        sender.sendMessage("Commands:");
        for (Map.Entry<String, String> entry : helpMenu.entrySet()) {
            final String key = entry.getKey();
            if (key.equals("sethere") && !Optional.ofNullable(SurvivelistEvents.Permissions.EVENT_SETHERE.getNode()).map(sender::hasPermission).orElse(false)) {
                continue;
            } else if (entry.getKey().equals("start") && !Optional.ofNullable(SurvivelistEvents.Permissions.EVENT_START.getNode()).map(sender::hasPermission).orElse(false)) {
                continue;
            } else if (entry.getKey().equals("end") && !Optional.ofNullable(SurvivelistEvents.Permissions.EVENT_END.getNode()).map(sender::hasPermission).orElse(false)) {
                continue;
            } else if (entry.getKey().equals("setteam") && !Optional.ofNullable(SurvivelistEvents.Permissions.EVENT_SETTEAM.getNode()).map(sender::hasPermission).orElse(false)) {
                continue;
            } else if (entry.getKey().equals("setmode") && !Optional.ofNullable(SurvivelistEvents.Permissions.EVENT_SETMODE.getNode()).map(sender::hasPermission).orElse(false)) {
                continue;
            }
            sender.sendMessage("/event " + key + " - " + entry.getValue());
        }
    }
}
