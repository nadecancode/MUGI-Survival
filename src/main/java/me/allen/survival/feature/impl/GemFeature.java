package me.allen.survival.feature.impl;

import com.google.common.collect.Sets;
import de.tr7zw.changeme.nbtapi.NBTItem;
import me.allen.survival.Survival;
import me.allen.survival.feature.Feature;
import me.allen.survival.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.Furnace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;

import java.applet.Applet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

public class GemFeature extends Feature {

    private final String GEM_ITEM_NBT_KEY = "Survival | Gem | Level";

    private final ItemStack GEM_ITEM = new ItemBuilder(Material.COAL)
            .displayname("&a&lGem")
            .glow()
            .lore(
                    "&7This Lv.1 Gem could give your armors & weapons",
                    "&7an advancement"
            )
            .unsafe()
            .setInt(GEM_ITEM_NBT_KEY, 1)
            .builder()
            .build();

    public GemFeature(Survival survival) {
        super(survival);
    }

    private Set<NamespacedKey> registeredNamespacedKeys;

    @Override
    protected boolean onEnable() {
        this.registeredNamespacedKeys = Sets.newHashSet();

        for (Material material : Material.values()) {
            if (material.name().toUpperCase().contains("SWORD")) {
                NamespacedKey namespacedKey = new NamespacedKey(this.plugin, "survival-gem-recipe-" + material.name().toLowerCase().replace("_", "-"));
                Bukkit.getServer().addRecipe(new FurnaceRecipe(namespacedKey, new ItemStack(material), new RecipeChoice.ExactChoice(GEM_ITEM), 0.0F, 20 * 10).setInput(material));
                this.registeredNamespacedKeys.add(namespacedKey);
            }
        }

        return true;
    }

    @Override
    public void onDisable() {
        this.registeredNamespacedKeys.forEach(Bukkit::removeRecipe);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        if (Stream.of(event.getInventory().getStorageContents()).anyMatch(itemStack -> itemStack.isSimilar(GEM_ITEM))) event.getInventory().setResult(null);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onCraft(CraftItemEvent event) {
        if (event.isCancelled()) return;
        if (Stream.of(event.getInventory().getStorageContents()).anyMatch(itemStack -> itemStack.isSimilar(GEM_ITEM))) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onFurnaceBurn(FurnaceBurnEvent event) {
        if (event.isCancelled() || !event.isBurning()) return;

        Furnace furnace = (Furnace) event.getBlock().getState();

        ItemStack smelting = furnace.getInventory().getSmelting(), fuel = furnace.getInventory().getFuel();

        if (fuel != null && fuel.isSimilar(GEM_ITEM)) {
            if (smelting != null && !smelting.getType().name().toUpperCase().contains("SWORD")) {
                event.setBurning(false);
                event.setBurnTime(0);
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onFurnaceSmelt(FurnaceSmeltEvent event) {
        if (event.isCancelled()) return;

        if (!event.getSource().getType().name().toUpperCase().contains("SWORD")) return;

        ItemStack source = event.getSource(), result = source.clone();

        int sharpnessLevel = source.getEnchantmentLevel(Enchantment.DAMAGE_ALL);
        if (sharpnessLevel <= 0) { // If the item doesn't have sharpness at all then just give 100% chance
            result.addUnsafeEnchantment(Enchantment.DAMAGE_ALL, 1);
        } else {
            double chance = 0.5D / (Math.max((sharpnessLevel - 3), 0.5D) * 2); // 50% base chance / ((2 * max((current sharpness level - 3, 0.5)))
            // Explanation: Base chance is 50%, and as the sharpness level is less than or equal to 3, the chance is automatically "approximately" 90% (As sharpness 1-3 are easy to get)
            // As the sharpness level gets greater than 3, the chance starts to get multiplied by a factor of 1 / (2(n-3)) where n is the sharpness level

            double random = Math.random();
            if (random <= chance) {
                result
                        .addUnsafeEnchantment(Enchantment.DAMAGE_ALL, sharpnessLevel + 1);
            }
        }

        event.setResult(result);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.isCancelled()) return;

        if (!event.getSlotType().equals(InventoryType.SlotType.RESULT) || event.getInventory().getType() != InventoryType.FURNACE) {
            return;
        }

        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        ItemStack result = event.getCurrentItem();
        if (result != null && result.getEnchantmentLevel(Enchantment.DAMAGE_ALL) >= 1) {
            Furnace furnace = (Furnace) event.getInventory().getHolder();

            if (furnace == null) return;

            furnace.setBurnTime((short) 0);
            furnace.setCookTime((short) 0);
            furnace.update();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onHopperRetrieveFurnace(InventoryMoveItemEvent event) {
        if (event.isCancelled()) return;

        if (event.getSource().getType().equals(InventoryType.FURNACE)) {
            if (event.getItem().getEnchantmentLevel(Enchantment.DAMAGE_ALL) >= 1) {
                FurnaceInventory furnaceInventory = (FurnaceInventory) event.getSource();
                Furnace furnace = furnaceInventory.getHolder();

                if (furnace == null) return;

                furnace.setBurnTime((short) 0);
                furnace.setCookTime((short) 0);
                furnace.update();
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR) // Monitor because I want it to be called at last, I don't want to create some Gem duplications by exploiting the protection plugin mechanics
    public void onGemMine(BlockBreakEvent event) { // "Called when a block gets broken by a player"
        if (event.isCancelled()) return;

        Block block = event.getBlock();
        if (!block.getType().equals(Material.STONE) && !block.getType().equals(Material.DIORITE) && !block.getType().equals(Material.ANDESITE) && !block.getType().equals(Material.GRANITE)) return; // Currently only stones will give possibilities of giving Gems.

        Player player = event.getPlayer();

        double chance = 0.1D; // Base chance is 5%

        ItemStack mainHandItem = player.getInventory().getItemInMainHand();

        if (mainHandItem.getEnchantmentLevel(Enchantment.SILK_TOUCH) > 0) chance = 0.01D;

        if (Math.random() < chance) {
            player
                    .getInventory()
                    .addItem(GEM_ITEM);
            player.sendMessage(ChatColor.GREEN + "You found a " + ChatColor.BOLD + "LV.1 GEM" + ChatColor.GREEN + " from mining!");
        }
    }
}
