package me.allen.survival.util.command.util.event.villager;

import me.allen.survival.event.villager.VillagerTradeEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Villager;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.Normalizer;
import java.util.*;
import java.util.logging.Logger;

/**
 * Performs all of the logic needed to trigger the, cancelable, custom VillagerTradeListener.VillagerTradeEvent.
 */
public class VillagerTradeListener implements Listener {
    private final Logger logger = Logger.getLogger("VillagerTradeListener");

    private static final Set<InventoryAction> purchaseSingleItemActions;
    static {
        // Each of these action types are single order purchases that do not require free inventory space to satisfy.
        // I.e. they stack up on the cursor (hover under the mouse).
        purchaseSingleItemActions = new HashSet<>();
        purchaseSingleItemActions.add(InventoryAction.PICKUP_ONE);
        purchaseSingleItemActions.add(InventoryAction.PICKUP_ALL);
        purchaseSingleItemActions.add(InventoryAction.PICKUP_HALF);
        purchaseSingleItemActions.add(InventoryAction.PICKUP_SOME);
        purchaseSingleItemActions.add(InventoryAction.DROP_ONE_SLOT);
        purchaseSingleItemActions.add(InventoryAction.DROP_ALL_SLOT);  // strangely in trades this is a single item event
        purchaseSingleItemActions.add(InventoryAction.HOTBAR_SWAP);
    }

    /** Because there is no Inventory:clone method */
    public static class InventorySnapshot implements InventoryHolder {
        Inventory inventory;

        public InventorySnapshot(Inventory inv) {
            ItemStack[] source = inv.getStorageContents();
            inventory = Bukkit.createInventory(this, source.length, "Snapshot");
            for (int i = 0; i < source.length; i++) {
                inventory.setItem(i, source[i] != null ? source[i].clone() : null);
            }
        }
        public InventorySnapshot(int size) {
            inventory = Bukkit.createInventory(this, size, "Snapshot");
        }
        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    public VillagerTradeListener(JavaPlugin owner) {
        owner.getServer().getPluginManager().registerEvents(this, owner);
    }

    /**
     * Calculates if the given stacks are of the exact same item thus could be stacked.
     * @return true - stacks can be combined (assuming a max stack size > 1).
     */
    public static boolean areStackable(ItemStack a, ItemStack b) {
        if (a == null && b == null || (a == null && b.getType() == Material.AIR)
                || (b == null && a.getType() == Material.AIR)) return true;
        if (a == null || b == null || a.getType() != b.getType()) return false;
        if (a.getItemMeta() == null && b.getItemMeta() == null) return true;
        if (a.getItemMeta() == null || b.getItemMeta() == null) return false;
        return a.getItemMeta().equals(b.getItemMeta());
    }

    @EventHandler
    public void onInventoryClickEvent(final InventoryClickEvent event) {
        if (event.getAction() == InventoryAction.NOTHING) return;
        if (event.getInventory().getType() != InventoryType.MERCHANT) return;
        if (event.getSlotType() != InventoryType.SlotType.RESULT) return;
        // Currently (1.15.x) there are no non-AbstractVillager Merchants
        if (!(event.getInventory().getHolder() instanceof AbstractVillager)) return;

        final HumanEntity player = event.getWhoClicked();
        final AbstractVillager villager = (AbstractVillager)event.getInventory().getHolder();
        final MerchantInventory merchantInventory = (MerchantInventory)event.getInventory();
        final MerchantRecipe recipe = merchantInventory.getSelectedRecipe();

        if (recipe == null) return;
        final ItemStack discountedA = NmsOperations.getPriceAdjustedIngredient1(villager, merchantInventory.getSelectedRecipeIndex());
        final int discountedPriceA = discountedA.getAmount();
        final int maxUses = recipe.getMaxUses() - recipe.getUses();

        VillagerTradeEvent vtEvent = null;
        if (purchaseSingleItemActions.contains(event.getAction())) {
            vtEvent = new VillagerTradeEvent(
                    player, villager,recipe, merchantInventory.getSelectedRecipeIndex(),
                    1, discountedPriceA,
                    recipe.getResult().getAmount(), 0
            );
        } else if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            // This situation is where the player SHIFT+CLICKS the output item to buy multiple times at once.
            // Because this event is fired before any inventories have changed - we need to simulate what will happen
            // when the inventories update.
            InventorySnapshot playerSnap = new InventorySnapshot(player.getInventory());
            InventorySnapshot merchantSnap = new InventorySnapshot(9);
            for (int i = 0; i < 3; i++) {
                if (merchantInventory.getItem(i) != null)
                    merchantSnap.getInventory().setItem(i, merchantInventory.getItem(i).clone());
            }
            List<ItemStack> ingredients = recipe.getIngredients();
            ItemStack ma = merchantSnap.getInventory().getItem(0);
            ItemStack mb = merchantSnap.getInventory().getItem(1);
            ItemStack ra = ingredients.get(0);
            ItemStack rb = ingredients.size() > 1 ? ingredients.get(1) : null;
            if (rb != null && rb.getType() == Material.AIR) rb = null;

            if (areStackable(ra, mb)) {
                ItemStack tmp = ma;
                ma = mb;
                mb = tmp;
            }

            int amount = ma.getAmount() / discountedPriceA;
            if (rb != null && mb != null && rb.getType() != Material.AIR && mb.getType() != Material.AIR) {
                amount = Math.min(amount, mb.getAmount() / rb.getAmount());
            }
            amount = clamp(amount, 0, maxUses);

            // In order for "failed" below to be populated we need to compute each stack here
            int maxStackSize = recipe.getResult().getMaxStackSize();
            List<ItemStack> stacks = new ArrayList<>();
            int unaccounted = amount;
            while (unaccounted != 0) {
                ItemStack stack = recipe.getResult().clone();
                stack.setAmount(Math.min(maxStackSize, unaccounted));
                stacks.add(stack);
                unaccounted -= stack.getAmount();
            }
            HashMap<Integer, ItemStack> failed = playerSnap.getInventory().addItem(stacks.toArray(new ItemStack[0]));
            int loss = 0;
            if (!failed.isEmpty()) {
                // int requested = amount;
                for (ItemStack stack : failed.values()) {
                    amount -= stack.getAmount();
                }
                // If a partial result is delivered, the rest of it is dropped... or just lost... I've seen both happen
                int rem = amount % recipe.getResult().getAmount();
                if (rem != 0) {
                    loss = recipe.getResult().getAmount() - rem;
                    amount += loss;
                }
            }
            int orders = amount / recipe.getResult().getAmount();
            vtEvent = new VillagerTradeEvent(
                    player, villager, recipe, merchantInventory.getSelectedRecipeIndex(),
                    orders, discountedPriceA,
                    amount, loss
            );
        }
        if (vtEvent != null) {
            vtEvent.setCancelled(event.isCancelled());
            event.setCancelled(!vtEvent.callEvent());
        }
    }


    public static int clamp(int value, int min, int max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }
}
 