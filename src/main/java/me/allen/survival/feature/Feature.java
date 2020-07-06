package me.allen.survival.feature;

import me.allen.survival.Survival;
import me.allen.survival.util.command.CommandHandler;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;

public abstract class Feature implements Listener {

    protected final Survival plugin;

    public Feature(Survival survival) {
        this.plugin = survival;
    }

    protected boolean onEnable() {
        return true;
    }

    public void onDisable() {}

    public final boolean register() {
        if (!onEnable()) return false;

        Bukkit.getPluginManager()
                .registerEvents(this, this.plugin);
        CommandHandler.registerClass(this.getClass());
        return true;
    }

}
