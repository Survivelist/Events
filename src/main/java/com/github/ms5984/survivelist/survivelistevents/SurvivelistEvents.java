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
import com.github.ms5984.survivelist.survivelistevents.commands.EventCommand;
import com.github.ms5984.survivelist.survivelistevents.commands.EventTpCommand;
import com.github.ms5984.survivelist.survivelistevents.model.SurvivelistServerEvent;
import com.github.ms5984.survivelist.survivelistevents.util.DataService;
import com.github.ms5984.survivelist.survivelistevents.util.TextLibrary;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * SurvivelistEvents plugin implementation.
 */
public final class SurvivelistEvents extends JavaPlugin implements EventService {
    private static SurvivelistEvents instance;
    private final SurvivelistServerEvent event = new SurvivelistServerEvent();
    private final DataService dataService = new DataService();
    private PluginCommand eventCmd;
    private PluginCommand eventTpCmd;

    @Override
    public void onEnable() {
        // Plugin startup logic
        instance = this;
        TextLibrary.setup(this);
        Permissions.registerSubDefaults();
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
    }

    @Override
    public @NotNull ServerEvent getEvent() {
        return event;
    }

    public static DataService getDataService() {
        return instance.dataService;
    }

    public enum Permissions {
        EVENT_COMMAND(() -> Optional.ofNullable(instance.eventCmd).map(PluginCommand::getPermission).orElse(null)),
        EVENTTP_COMMAND(() -> Optional.ofNullable(instance.eventTpCmd).map(PluginCommand::getPermission).orElse(null)),
        EVENT_SETHERE(() -> instance.getConfig().getString("subcommand-info.event.sethere.permission")),
        EVENT_JOIN(() -> instance.getConfig().getString("subcommand-info.event.join.permission")),
        EVENT_LEAVE(() -> instance.getConfig().getString("subcommand-info.event.leave.permission")),
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

        public @Nullable String getNode() {
            return supplier.get();
        }
    }

    public enum Messages {
        NO_PERMISSION("no-permission"),
        MUST_BE_PLAYER("player"),
        LOCATION_SET_("location-set"), // fields that end in _ generally call replacements
        PLEASE_EMPTY_INVENTORY("empty-inventory"),
        JOIN_MESSAGE_SELF("joining.self"),
        JOIN_ANNOUNCE_("joining.announce"),
        JOIN_ALREADY_IN("joining.already-in"),
        LEAVE_MESSAGE_SELF("leaving.self"),
        LEAVE_ANNOUNCE_("leaving.announce"),
        LEAVE_NOT_IN("leaving.not-in"),
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
