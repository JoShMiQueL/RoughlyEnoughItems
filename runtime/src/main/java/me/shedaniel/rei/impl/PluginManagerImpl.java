/*
 * This file is licensed under the MIT License, part of Roughly Enough Items.
 * Copyright (c) 2018, 2019, 2020 shedaniel
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

package me.shedaniel.rei.impl;

import com.google.common.base.MoreObjects;
import com.google.common.base.Stopwatch;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;
import me.shedaniel.rei.RoughlyEnoughItemsCore;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.client.registry.entry.EntryRegistry;
import me.shedaniel.rei.api.client.registry.screen.ScreenRegistry;
import me.shedaniel.rei.api.common.plugins.PluginManager;
import me.shedaniel.rei.api.common.plugins.PluginView;
import me.shedaniel.rei.api.common.plugins.REIPlugin;
import me.shedaniel.rei.api.common.plugins.REIPluginProvider;
import me.shedaniel.rei.api.common.registry.Reloadable;
import me.shedaniel.rei.api.common.util.CollectionUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.tuple.MutablePair;
import org.jetbrains.annotations.ApiStatus;

import java.io.Closeable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

@ApiStatus.Internal
@Environment(EnvType.CLIENT)
public class PluginManagerImpl<P extends REIPlugin<?>> implements PluginManager<P>, PluginView<P> {
    private final List<Reloadable<P>> reloadables = new ArrayList<>();
    private final Map<Class<? extends Reloadable<P>>, Reloadable<? super P>> cache = new ConcurrentHashMap<>();
    private final UnaryOperator<PluginView<P>> view;
    private boolean arePluginsLoading = false;
    private final List<REIPluginProvider<P>> plugins = new ArrayList<>();
    
    @SafeVarargs
    public PluginManagerImpl(UnaryOperator<PluginView<P>> view, Reloadable<P>... reloadables) {
        this.view = view;
        for (Reloadable<P> reloadable : reloadables) {
            registerReloadable(reloadable);
        }
    }
    
    @Override
    public void registerReloadable(Reloadable<? extends P> reloadable) {
        this.reloadables.add((Reloadable<P>) reloadable);
    }
    
    @Override
    public boolean arePluginsReloading() {
        return arePluginsLoading;
    }
    
    @Override
    public <T extends Reloadable<? super P>> T get(Class<T> reloadableClass) {
        Reloadable<? super P> reloadable = this.cache.get(reloadableClass);
        if (reloadable != null) return (T) reloadable;
        
        for (Reloadable<P> r : reloadables) {
            if (reloadableClass.isInstance(r)) {
                this.cache.put((Class<? extends Reloadable<P>>) reloadableClass, r);
                return (T) r;
            }
        }
        throw new IllegalArgumentException("Unknown reloadable type! " + reloadableClass.getName());
    }
    
    @Override
    public List<Reloadable<P>> getReloadables() {
        return Collections.unmodifiableList(reloadables);
    }
    
    @Override
    public PluginView<P> view() {
        return view.apply(this);
    }
    
    @Override
    public void registerPlugin(REIPluginProvider<? extends P> plugin) {
        plugins.add((REIPluginProvider<P>) plugin);
        RoughlyEnoughItemsCore.LOGGER.info("Registered plugin provider %s", plugin.getPluginProviderName());
    }
    
    @Override
    public List<REIPluginProvider<P>> getPluginProviders() {
        return Collections.unmodifiableList(plugins);
    }
    
    @Override
    public FluentIterable<P> getPlugins() {
        return FluentIterable.concat(Iterables.transform(plugins, REIPluginProvider::provide));
    }
    
    private static class SectionClosable implements Closeable {
        private MutablePair<Stopwatch, String> sectionData;
        
        public SectionClosable(MutablePair<Stopwatch, String> sectionData, String section) {
            this.sectionData = sectionData;
            sectionData.setRight(section);
            RoughlyEnoughItemsCore.LOGGER.debug("Reloading Section: \"%s\"", section);
            sectionData.getLeft().reset().start();
        }
        
        @Override
        public void close() {
            sectionData.getLeft().stop();
            String section = sectionData.getRight();
            RoughlyEnoughItemsCore.LOGGER.debug("Reloading Section: \"%s\" done in %s", section, sectionData.getLeft().toString());
            sectionData.getLeft().reset();
        }
    }
    
    private SectionClosable section(MutablePair<Stopwatch, String> sectionData, String section) {
        return new SectionClosable(sectionData, section);
    }
    
    private void pluginSection(MutablePair<Stopwatch, String> sectionData, String sectionName, List<P> list, Consumer<P> consumer) {
        for (P plugin : list) {
            try (SectionClosable section = section(sectionData, sectionName + " for " + plugin.getPluginName())) {
                consumer.accept(plugin);
            } catch (Throwable throwable) {
                RoughlyEnoughItemsCore.LOGGER.error(plugin.getPluginName() + " plugin failed to " + sectionName + "!", throwable);
            }
        }
    }
    
    @Override
    public void startReload() {
        try {
            long startTime = Util.getMillis();
            MutablePair<Stopwatch, String> sectionData = new MutablePair<>(Stopwatch.createUnstarted(), "");
            
            try (SectionClosable startReload = section(sectionData, "start-reload")) {
                for (Reloadable<P> reloadable : reloadables) {
                    reloadable.startReload();
                }
            }
            
            arePluginsLoading = true;
            List<P> plugins = new ArrayList<>(getPlugins().toList());
            plugins.sort(Comparator.comparingInt(P::getPriority).reversed());
            RoughlyEnoughItemsCore.LOGGER.info("Reloading REI, registered %d plugins: %s", plugins.size(), CollectionUtils.mapAndJoinToString(plugins, REIPlugin::getPluginName, ", "));
            Collections.reverse(plugins);
            
            pluginSection(sectionData, "pre-register", plugins, REIPlugin::preRegister);
            for (Reloadable<P> reloadable : getReloadables()) {
                Class<?> reloadableClass = reloadable.getClass();
                pluginSection(sectionData, "reloadable-plugin-" + MoreObjects.firstNonNull(reloadableClass.getSimpleName(), reloadableClass.getName()), plugins, reloadable::acceptPlugin);
            }
            pluginSection(sectionData, "post-register", plugins, REIPlugin::postRegister);
            
            try (SectionClosable endReload = section(sectionData, "end-reload")) {
                for (Reloadable<P> reloadable : reloadables) {
                    reloadable.endReload();
                }
            }
            
            arePluginsLoading = false;
            
            try (SectionClosable refilter = section(sectionData, "entry-registry-refilter")) {
                EntryRegistry.getInstance().refilter();
            }
            
            long usedTime = Util.getMillis() - startTime;
            RoughlyEnoughItemsCore.LOGGER.info("Reloaded %d stack entries, %d displays, %d exclusion zones suppliers, %d overlay deciders, %d visibility predicates and %d categories (%s) in %dms.",
                    EntryRegistry.getInstance().size(),
                    DisplayRegistry.getInstance().getDisplayCount(),
                    ScreenRegistry.getInstance().exclusionZones().getZonesCount(),
                    ScreenRegistry.getInstance().getDeciders().size(),
                    DisplayRegistry.getInstance().getVisibilityPredicates().size(),
                    CategoryRegistry.getInstance().size(),
                    CategoryRegistry.getInstance().stream()
                            .map(CategoryRegistry.CategoryConfiguration::getCategory)
                            .map(DisplayCategory::getTitle)
                            .map(Component::getString).collect(Collectors.joining(", ")),
                    usedTime
            );
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        } finally {
            arePluginsLoading = false;
        }
    }
}
