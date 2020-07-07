package me.allen.survival.feature.impl;

import com.google.common.collect.Sets;
import de.tr7zw.changeme.nbtapi.NBTItem;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.allen.survival.Survival;
import me.allen.survival.event.gem.GemAdvanceEvent;
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
import org.bukkit.inventory.FurnaceInventory;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;

import java.util.Set;
import java.util.function.Function;
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

    private final Function<ItemStack, Boolean> GEM_CHECKER = ((itemStack) -> itemStack != null && !itemStack.getType().equals(Material.AIR) && new NBTItem(itemStack).hasKey(GEM_ITEM_NBT_KEY));

    @Override
    protected boolean onEnable() {
        this.registeredNamespacedKeys = Sets.newHashSet();

        for (Material material : Material.values()) {
            if (GemApplicableType.matchApplicableType(material) != null) {
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
        if (Stream.of(event.getInventory().getStorageContents()).anyMatch(GEM_CHECKER::apply)) event.getInventory().setResult(null);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onCraft(CraftItemEvent event) {
        if (event.isCancelled()) return;
        if (Stream.of(event.getInventory().getStorageContents()).anyMatch(GEM_CHECKER::apply)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onFurnaceBurn(FurnaceBurnEvent event) {
        if (event.isCancelled() || !event.isBurning()) return;

        Furnace furnace = (Furnace) event.getBlock().getState();

        ItemStack smelting = furnace.getInventory().getSmelting(), fuel = furnace.getInventory().getFuel();

        if (fuel != null && GEM_CHECKER.apply(fuel)) {
            if (smelting != null && GemApplicableType.matchApplicableType(smelting.getType()) != null) {
                event.setBurnTime(event.getBurnTime() / 8); // Coal burn time / 8 means only one item per gem since coal is 8 items
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onFurnaceSmelt(FurnaceSmeltEvent event) {
        if (event.isCancelled()) return;

        ItemStack source = event.getSource();

        if (applyGem(source, 1)) event.setResult(source);
    }

    @EventHandler(priority = EventPriority.MONITOR) // Monitor because I want it to be called at last, I don't want to create some Gem duplications by exploiting the protection plugin mechanics
    public void onGemMine(BlockBreakEvent event) { // "Called when a block gets broken by a player"
        if (event.isCancelled()) return;

        Block block = event.getBlock();
        if (!block.getType().equals(Material.STONE) && !block.getType().equals(Material.DIORITE) && !block.getType().equals(Material.ANDESITE) && !block.getType().equals(Material.GRANITE)) return; // Currently only stones will give possibilities of giving Gems.

        Player player = event.getPlayer();

        double chance = 0.1D; // Base chance is 10%

        ItemStack mainHandItem = player.getInventory().getItemInMainHand();

        if (mainHandItem.getEnchantmentLevel(Enchantment.SILK_TOUCH) > 0 || !block.getType().equals(Material.STONE)) chance *= 0.2D; // 1/5 Chance with Silk Touch and Non-Stone nerf

        if (Math.random() < chance) {
            block.getWorld().dropItemNaturally(block.getLocation(), GEM_ITEM);
            player.sendMessage(ChatColor.GREEN + "You found a " + ChatColor.BOLD + "LV.1 GEM" + ChatColor.GREEN + " from mining!");
        }
    }

    public boolean applyGem(ItemStack itemStack, int gemLevel) {
        GemApplicableType applicableType = GemApplicableType.matchApplicableType(itemStack.getType());

        if (applicableType == null) return false;

        Enchantment applicationEnchantment = applicableType.getApplicableEnchantment();

        int enchantmentLevel = itemStack.getEnchantmentLevel(applicationEnchantment);
        if (enchantmentLevel <= 0) { // If the item doesn't have sharpness at all then just give 100% chance
            itemStack.addUnsafeEnchantment(applicationEnchantment, 1);
        } else {
            double chance = 0.5D / (Math.max((enchantmentLevel - 3), 0.2) * 2); // 50% base chance / ((2 * max((current enchantment level - 3, 0.2)))
            // Explanation: Base chance is 50%, and as the enchantment level is less than or equal to 3, the chance is automatically "approximately" 90% (As enchantment 1-3 are easy to get)
            // As the enchantment level gets greater than 3, the chance starts to get multiplied by a factor of 1 / (2(n-3)) where n is the enchantment level

            if (applicableType == GemApplicableType.TOOL && enchantmentLevel > 4) chance *= chance; // According to the conversation in the discord, square the base chance will give a fair depreciation & appreciation value
            else if (applicableType == GemApplicableType.BOW && enchantmentLevel > 3) chance *= 0.25D; // Assuming full charges, the fair value is 1/4 of the sword sharpness appreciation & depreciation

            double random = Math.random();
            if (random <= chance) {
                GemAdvanceEvent event = new GemAdvanceEvent(applicableType, itemStack);
                if (event.callEvent())
                    event.getTargetItem()
                        .addUnsafeEnchantment(applicationEnchantment, enchantmentLevel + 1);
            }
        }

        return true;
    }

    @RequiredArgsConstructor
    @Getter
    public enum GemApplicableType {
        SWORD(Enchantment.DAMAGE_ALL),
        BOW(Enchantment.ARROW_DAMAGE),
        ARMOR(Enchantment.PROTECTION_ENVIRONMENTAL),
        TOOL(Enchantment.DIG_SPEED);

        private final Enchantment applicableEnchantment;

        public static GemApplicableType matchApplicableType(Material material) {
            String materialName = material.name().toUpperCase();

            if (materialName.endsWith("SWORD")) {
                return  GemApplicableType.SWORD;
            } else if (materialName.endsWith("BOW")) {
                return GemApplicableType.BOW;
            //} else if (materialName.endsWith("HELMET") || materialName.endsWith("CHESTPLATE") || materialName.endsWith("LEGGINS") || materialName.endsWith("BOOTS")) {
                //applicableType = GemApplicableType.ARMOR;
            } else if (materialName.endsWith("SHOVEL") || materialName.endsWith("HOE") || materialName.endsWith("PICKAXE") || materialName.endsWith("AXE")) { // Order here matters for PICKAXE and AXE because PICKAXE contains AXE
                return GemApplicableType.TOOL;
            } else {
                return null;
            }
        }
    }
}
