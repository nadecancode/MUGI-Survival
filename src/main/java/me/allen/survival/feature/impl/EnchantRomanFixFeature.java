package me.allen.survival.feature.impl;

import com.destroystokyo.paper.event.inventory.PrepareResultEvent;
import com.google.common.collect.Sets;
import me.allen.survival.Survival;
import me.allen.survival.event.gem.GemAdvanceEvent;
import me.allen.survival.event.villager.VillagerTradeEvent;
import me.allen.survival.feature.Feature;
import me.allen.survival.util.command.Command;
import me.allen.survival.util.command.util.RomanUtil;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class EnchantRomanFixFeature extends Feature {
    private final NamespacedKey ENCHANTMENT_FIX_KEY = new NamespacedKey(this.plugin, "survival-fix-enchant-numbers");

    // For no level showing, to replicate Vanilla mechanics
    private final Set<Enchantment> NO_ENCHANTMENT_LEVEL_MARK = Sets.newHashSet(
        Enchantment.MENDING,
        Enchantment.WATER_WORKER,
        Enchantment.SILK_TOUCH,
        Enchantment.CHANNELING,
        Enchantment.MULTISHOT,
        Enchantment.ARROW_INFINITE,
        Enchantment.ARROW_FIRE
    );

    public EnchantRomanFixFeature(Survival survival) {
        super(survival);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPrepareResult(PrepareResultEvent event) {
        if (event.getResult() != null) event.setResult(convertItem(event.getResult()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEnchantItem(EnchantItemEvent event) {
        Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin, () -> event.getItem().setItemMeta(convertMeta(event.getItem().getItemMeta())));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPickup(EntityPickupItemEvent event) {
        if (event.isCancelled()) return;
        if (event.getItem().getItemStack().getEnchantments().isEmpty()) return;
        event.getItem().setItemStack(convertItem(event.getItem().getItemStack()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onGemAdvance(GemAdvanceEvent event) {
        if (event.isCancelled()) return;

        event.setTargetItem(convertItem(event.getTargetItem()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onVillagerTrade(VillagerTradeEvent event) {
        if (event.isCancelled()) return;
        event.getResultItem().setItemMeta(convertMeta(event.getResultItem().getItemMeta()));
    }

    @Command(names = "fix-enchant", permissionNode = "mugi.admin")
    public void fixEnchant(Player sender) {
        sender.getInventory().setItemInMainHand(convertItem(sender.getInventory().getItemInMainHand()));
        sender.sendMessage(ChatColor.GREEN + "Enchantment Fixed");
    }

    private ItemStack convertItem(ItemStack itemStack) {
        itemStack.setItemMeta(convertMeta(itemStack.getItemMeta()));
        return itemStack;
    }

    private ItemMeta convertMeta(ItemMeta itemMeta) {
        List<String> lore = itemMeta.hasLore() ? itemMeta.getLore() : new ArrayList<>();
        List<String> enchantLore = new ArrayList<>();

        if (itemMeta.getPersistentDataContainer().has(ENCHANTMENT_FIX_KEY, PersistentDataType.INTEGER) && lore != null) { // `lore != null` is not necessary but IntelliJ IDEA doesn't resolve this and gives me warning
            int len = itemMeta.getPersistentDataContainer().get(ENCHANTMENT_FIX_KEY, PersistentDataType.INTEGER);
            lore.subList(0, len).clear();
        }

        for (Map.Entry<Enchantment, Integer> enchantment : itemMeta.getEnchants().entrySet()) {
            String line = "";
            String enchantName = enchantment.getKey().getKey().toString();

            if (enchantment.getKey().getKey().toString().replace("_", " ").contains("curse")) {
                String curses = WordUtils.capitalize(enchantName.replace("minecraft:", "").replace("_", " ")).replace("Of", "of");
                String[] cursesSplitted = curses.split(" ");
                String newName = ChatColor.RED + "Curse of";

                String curse;
                for(Iterator<String> curseIterator = Arrays.asList(cursesSplitted).iterator(); curseIterator.hasNext(); newName = newName + " " + curse) {
                    curse = curseIterator.next();
                    if (curse.equalsIgnoreCase("curse")) {
                        break;
                    }
                }

                line = newName.replace("Of", "of");
                enchantLore.add(0, line);
            } else {
                line = ChatColor.GRAY + WordUtils.capitalize(enchantName.replace("minecraft:", "").replace("_", " ")).replace("Of", "of").replace("Sweeping", "Sweeping Edge");
                line = NO_ENCHANTMENT_LEVEL_MARK.contains(enchantment.getKey()) ? line : (line + " " + RomanUtil.toRoman(enchantment.getValue()));

                enchantLore.add(line);
            }
        }

        itemMeta.getPersistentDataContainer().set(ENCHANTMENT_FIX_KEY, PersistentDataType.INTEGER, enchantLore.size());

        if (lore != null) { // `lore != null` is not necessary but IntelliJ IDEA doesn't resolve this and gives me warning
            for (String line : enchantLore) {
                lore.add(0, line);
            }

            Collections.reverse(lore); // Simply reverse here for the alphabetical order (Vanilla mechanics)
        }

        itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        itemMeta.setLore(lore);

        return itemMeta;
    }
}
