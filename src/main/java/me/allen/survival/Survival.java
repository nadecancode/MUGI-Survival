package me.allen.survival;

import com.google.common.collect.Sets;
import lombok.Getter;
import me.allen.survival.feature.Feature;
import me.allen.survival.util.ClassUtil;
import me.allen.survival.util.command.CommandHandler;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Set;

public class Survival extends JavaPlugin {

    @Getter
    private static Survival instance;

    private Set<Feature> loadedFeatures;

    @Override
    public void onEnable() {
        instance = this;
        long before = System.currentTimeMillis();
        CommandHandler.init(this);
        this.loadedFeatures = Sets.newHashSet();
        ClassUtil
                .getClassesInPackage(this, "me.allen.survival.feature.impl")
                .forEach(featureClass -> {
                    if (!Feature.class.isAssignableFrom(featureClass)) return; // The sub-feature implementation classes must extend the parent `Feature` class
                    try {
                        Feature feature = (Feature) featureClass.getConstructor(Survival.class).newInstance(this);
                        if (feature.register()) { // The feature register must pass the `onEnable` method in order to get actually registered
                            this.loadedFeatures.add(feature);
                            Bukkit.getConsoleSender().sendMessage(ChatColor.WHITE + "  Loaded Feature `" + featureClass.getSimpleName() + "`");
                        }
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }
                });
        Bukkit
                .getConsoleSender()
                .sendMessage(ChatColor.GRAY + "MUGICRAFT Survival has been enabled (" + (System.currentTimeMillis() - before) + "ms)");
    }

    @Override
    public void onDisable() {
        loadedFeatures.forEach(Feature::onDisable);
    }

}
