package me.allen.survival.event.gem;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import me.allen.survival.event.CancellableEvent;
import me.allen.survival.feature.impl.GemFeature;
import org.bukkit.inventory.ItemStack;

@AllArgsConstructor
@Setter
@Getter
public class GemAdvanceEvent extends CancellableEvent {
    private final GemFeature.GemApplicableType gemApplicableType;
    private ItemStack targetItem;
}
