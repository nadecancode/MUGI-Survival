package me.allen.survival.event.villager;

import me.allen.survival.event.CancellableEvent;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Villager;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;

public class VillagerTradeEvent extends CancellableEvent {
        final HumanEntity player;
        final AbstractVillager villager;
        final MerchantRecipe recipe;
        final int offerIndex;
        final int orders;
        final int ingredientOneDiscountedPrice;
        final int ingredientOneTotalAmount;
        final int ingredientTwoTotalAmount;
        final int amountPurchased;
        final int amountLost;

        public VillagerTradeEvent(HumanEntity player, AbstractVillager villager, MerchantRecipe recipe, int offerIndex,
                                  int orders, int ingredientOneDiscountedPrice,
                                  int amountPurchased, int amountLost) {
            this.player = player;
            this.villager = villager;
            this.recipe = recipe;
            this.offerIndex = offerIndex;
            this.orders = orders;
            this.ingredientOneDiscountedPrice = ingredientOneDiscountedPrice;
            this.amountPurchased = amountPurchased;
            this.amountLost = amountLost;

            ingredientOneTotalAmount = ingredientOneDiscountedPrice * orders;
            if (recipe.getIngredients().size() > 1) {
                ItemStack bb = recipe.getIngredients().get(1);
                ingredientTwoTotalAmount = bb.getType() != Material.AIR ? bb.getAmount() * orders : 0;
            } else {
                ingredientTwoTotalAmount = 0;
            }
        }

        public HumanEntity getPlayer() {
            return player;
        }

        public AbstractVillager getVillager() {
            return villager;
        }

        public MerchantRecipe getRecipe() {
            return recipe;
        }

        /** For the total count of the item purchased use {@code getAmountPurchased}.*/
        public int getOrders() {
            return orders;
        }

        public int getOfferIndex() {
            return offerIndex;
        }

        /**
         * The actual amount of ingredient one charged for a single 'order'; e.g. the price after all
         * gossip/player-reputation and hero of the village effects have been applied.
         * Note that only the first ingredient is discounted by the villager.
         * @return amount of item 1 each order actually cost.
         */
        public int getIngredientOneDiscountedPrice() {
            return ingredientOneDiscountedPrice;
        }

        /** The total amount of {@code recipe.getIngredients().get(0)} spent */
        public int getIngredientOneTotalAmount() {
            return ingredientOneTotalAmount;
        }
        /** The total amount of {@code recipe.getIngredients().get(1)} spent, or zero if no ingredient 2*/
        public int getIngredientTwoTotalAmount() {
            return ingredientTwoTotalAmount;
        }

        public String getBestNameForIngredientOne() {
            return bestNameFor(recipe.getIngredients().get(0));
        }

        public String getBestNameForIngredientTwo() {
            if (recipe.getIngredients().size() > 1) {
                ItemStack stack = recipe.getIngredients().get(1);
                if (stack != null)
                    return bestNameFor(stack);
            }
            return null;
        }

        public ItemStack getResultItem() {
            return recipe.getResult();
        }

        public String getBestNameForResultItem() {
            return bestNameFor(recipe.getResult());
        }

        /** Total amount of {@code recipe.getResult()} purchased. This value is the total count the player received. */
        public int getAmountPurchased() {
            return amountPurchased;
        }

        /** When the player does not have inventory space for all of the items purchased they may drop or simply
         * be lost. I've seen both happen.*/
        public int getAmountLost() {
            return amountLost;
        }

        static private String bestNameFor(ItemStack stack) {
            if (stack == null) return "null";
            if (stack.getType() == Material.WRITTEN_BOOK) {
                BookMeta meta = (BookMeta)stack.getItemMeta();
                if (meta != null && meta.hasTitle() && meta.getTitle() != null)
                    return ChatColor.stripColor(meta.getTitle());
                // TODO: fallback to finding enchants
            }
            if (stack.getItemMeta() != null) {
                ItemMeta meta = stack.getItemMeta();
                if (meta.hasDisplayName())
                    return ChatColor.stripColor(meta.getDisplayName());
            }
            return stack.getType().name();
        }

        public String bestNameForVillager() {
            if (villager.getCustomName() != null)
                return villager.getCustomName();
            return villager.getName();
        }

        public boolean isWanderingTrader() {
            return villager instanceof WanderingTrader;
        }

        public Villager.Profession getVillagerProfession() {
            if (!(villager instanceof Villager)) return Villager.Profession.NONE;
            return ((Villager)villager).getProfession();
        }

        public Villager.Type getVillagerType() {
            if (!(villager instanceof Villager)) return null;
            return ((Villager)villager).getVillagerType();
        }
    }