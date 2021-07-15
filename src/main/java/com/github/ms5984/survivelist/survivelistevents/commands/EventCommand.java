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
import com.github.ms5984.survivelist.survivelistevents.api.EventService;
import com.github.ms5984.survivelist.survivelistevents.api.exceptions.AlreadyPresentPlayerException;
import com.github.ms5984.survivelist.survivelistevents.api.exceptions.InventoryNotClearPlayerException;
import com.github.ms5984.survivelist.survivelistevents.api.exceptions.NotPresentPlayerException;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Handles /event command and its subcommands.
 */
public class EventCommand implements TabExecutor {
    private final EventService eventService;
    private final ImmutableMap<String, String> helpMenu;
    private final ImmutableList<String> tabCompletions;

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
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!command.testPermission(sender)) return true;
        if (args.length != 1) {
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
                }
                sender.sendMessage("/event " + key + " - " + entry.getValue());
            }
            return true;
        }
        // Process subcommands
        // check for player
        if (!(sender instanceof Player)) {
            // not player, send error
            sender.sendMessage(SurvivelistEvents.Messages.MUST_BE_PLAYER.toString());
            return true;
        }
        // is player; cast and store
        final Player player = (Player) sender;
        if (args[0].equalsIgnoreCase("join")) {
            // Test permission
            if (!Optional.ofNullable(SurvivelistEvents.Permissions.EVENT_JOIN.getNode()).map(sender::hasPermission).orElse(false)) {
                sender.sendMessage(SurvivelistEvents.Messages.NO_PERMISSION.toString());
                return true;
            }
            // Join event
            try {
                eventService.getEvent().addPlayer(player).teleportToEvent();
            } catch (AlreadyPresentPlayerException | InventoryNotClearPlayerException e) {
                // Send message "you are already in the event" or "please empty your inventory"
                sender.sendMessage(e.getMessage());
                return true;
            }
            player.sendMessage(SurvivelistEvents.Messages.JOIN_MESSAGE_SELF.toString());
            eventService.getEvent().sendMessage(SurvivelistEvents.Messages.JOIN_ANNOUNCE_.replace(player.getName()), p -> p != player);
        } else if (args[0].equalsIgnoreCase("leave")) {
            // Test permission
            if (!Optional.ofNullable(SurvivelistEvents.Permissions.EVENT_LEAVE.getNode()).map(sender::hasPermission).orElse(false)) {
                sender.sendMessage(SurvivelistEvents.Messages.NO_PERMISSION.toString());
                return true;
            }
            // Leave event
            try {
                eventService.getEvent().removePlayer(player);
            } catch (NotPresentPlayerException e) {
                // Send message "you are not in the event"
                sender.sendMessage(e.getMessage());
                return true;
            }
            player.sendMessage(SurvivelistEvents.Messages.LEAVE_MESSAGE_SELF.toString());
            eventService.getEvent().sendMessage(SurvivelistEvents.Messages.LEAVE_ANNOUNCE_.replace(player.getName()), p -> p != player);
        } else if (args[0].equalsIgnoreCase("sethere")) {
            // Test permission
            if (!Optional.ofNullable(SurvivelistEvents.Permissions.EVENT_SETHERE.getNode()).map(sender::hasPermission).orElse(false)) {
                sender.sendMessage(SurvivelistEvents.Messages.NO_PERMISSION.toString());
                return true;
            }
            // Set event location
            final Location location = player.getLocation();
            eventService.getEvent().setEventLocation(location);
            player.sendMessage(SurvivelistEvents.Messages.LOCATION_SET_.replace(location));
        } else if (args[0].equalsIgnoreCase("start")) {
            // Test permission
            if (!Optional.ofNullable(SurvivelistEvents.Permissions.EVENT_START.getNode()).map(sender::hasPermission).orElse(false)) {
                sender.sendMessage(SurvivelistEvents.Messages.NO_PERMISSION.toString());
                return true;
            }
            // Start event
        } else if (args[0].equalsIgnoreCase("end")) {
            // Test permission
            if (!Optional.ofNullable(SurvivelistEvents.Permissions.EVENT_END.getNode()).map(sender::hasPermission).orElse(false)) {
                sender.sendMessage(SurvivelistEvents.Messages.NO_PERMISSION.toString());
                return true;
            }
            // End event
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        final ArrayList<String> completions = new ArrayList<>(tabCompletions);
        if (args.length == 1 && command.testPermissionSilent(sender)) {
            if (!Optional.ofNullable(SurvivelistEvents.Permissions.EVENT_SETHERE.getNode()).map(sender::hasPermission).orElse(false)) {
                completions.remove("sethere");
            }
            for (String tabCompletion : tabCompletions) {
                if (!tabCompletion.startsWith(args[0])) {
                    completions.remove(tabCompletion);
                }
            }
            return completions;
        }
        return ImmutableList.of();
    }
}
