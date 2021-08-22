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
package com.github.ms5984.survivelist.survivelistevents.util;

import com.github.ms5984.survivelist.survivelistevents.SurvivelistEvents;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Represents a Yaml-based data file.
 *
 * @since 1.0.0
 */
@SuppressWarnings("UnusedReturnValue")
public class DataFile {
    private final File file;
    private final YamlConfiguration configuration;
    private final ExecutorService executorService;

    public DataFile(String filename) {
        this(null, filename);
    }
    public DataFile(String directory, String filename) {
        File dir = JavaPlugin.getProvidingPlugin(getClass()).getDataFolder();
        // if directory is not null && not empty
        if (directory != null && !directory.isEmpty()) {
            dir = new File(dir, directory);
        }
        // Create parent dir as needed
        if (!dir.mkdirs() && !dir.isDirectory()) {
            throw new IllegalStateException("Unable to create or resolve parent directory: " + dir);
        }
        // Create file (object)
        this.file = new File(dir, (filename.endsWith(".yml") ? filename : filename + ".yml"));
        // Load from file
        if (file.isFile()) {
            configuration = YamlConfiguration.loadConfiguration(file);
        } else {
            configuration = new YamlConfiguration();
        }
        // Get / Set up ExecutorService
        this.executorService = SurvivelistEvents.getDataService().getExecutor(this);
    }

    /**
     * Get a value.
     * <p>
     * Utilizes CompletableFuture to allow for easy wait/map/andThen logic.
     * <b>Performed async. Avoid calling Bukkit API.</b>
     *
     * @param readOperation operation to get value
     * @param <R> type of value (may be inferred)
     * @return the value returned by provided function
     */
    public <R> CompletableFuture<R> getValue(Function<FileConfiguration, R> readOperation) {
        return CompletableFuture.supplyAsync(() -> readOperation.apply(configuration), executorService);
    }

    /**
     * Get a value immediately, blocking until it is resolved.
     * <p>
     * <b>Performed async. Avoid calling Bukkit API.</b>
     *
     * @param readOperation operation to get value
     * @param <R> type of value (may be inferred)
     * @return the value returned by provided function
     */
    public final <R> R getValueNow(Function<FileConfiguration, R> readOperation) {
        return getValue(readOperation).join();
    }

    /**
     * Update values in the data file.
     *
     * @return Void-return CompletableFuture for andThen-type logic
     */
    public CompletableFuture<Void> update(Consumer<FileConfiguration> updateOperation) {
        return CompletableFuture.runAsync(() -> updateOperation.accept(configuration), executorService);
    }

    /**
     * Save the data file.
     *
     * @return Void-return CompletableFuture for andThen-type logic
     */
    public CompletableFuture<Void> save() {
        return CompletableFuture.runAsync(() -> {
            try {
                configuration.save(file);
            } catch (IOException e) {
                throw new IllegalStateException("Unable to save DataFile[" + file + "]", e);
            }
        }, executorService);
    }

    /**
     * Delete the data file (if it exists).
     * <p>
     * Additionally clears internal FileConfiguration.
     *
     * @return true if the file existed and was deleted
     */
    public CompletableFuture<Boolean> delete() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                configuration.loadFromString("");
            } catch (InvalidConfigurationException e) {
                throw new IllegalStateException(e);
            }
            return file.delete();
        }, executorService);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DataFile dataFile = (DataFile) o;
        return file.equals(dataFile.file);
    }

    @Override
    public int hashCode() {
        return Objects.hash(file);
    }
}
