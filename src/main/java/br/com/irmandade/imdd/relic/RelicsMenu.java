package br.com.irmandade.imdd.relic;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.util.List;

/** GUI 9x3 do /relics: clique esquerdo ou direito entrega uma cópia; o item continua na GUI. */
public class RelicsMenu extends GenericContainerScreenHandler {

    private static Text title() {
        MutableText t = Text.literal("IMDD").setStyle(Style.EMPTY.withColor(0x8844eb).withBold(true).withItalic(false));
        t.append(Text.literal("»").setStyle(Style.EMPTY.withColor(0xAAAAAA).withBold(false).withItalic(false)));
        t.append(Text.literal(" Relíquia").setStyle(Style.EMPTY.withColor(0x2e2e2e).withBold(false).withItalic(false)));
        return t;
    }

    public RelicsMenu(int syncId, PlayerInventory playerInv, Inventory container) {
        super(ScreenHandlerType.GENERIC_9X3, syncId, playerInv, container, 3);
    }

    public static void open(ServerPlayerEntity player) {
        SimpleInventory inv = new SimpleInventory(27);
        List<ItemStack> relics = RelicRegistry.stacks();
        for (int i = 0; i < Math.min(27, relics.size()); i++) inv.setStack(i, relics.get(i).copy());
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, pInv, p) -> new RelicsMenu(syncId, pInv, inv), title()));
    }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        if (slotIndex >= 0 && slotIndex < 27 && actionType == SlotActionType.PICKUP && (button == 0 || button == 1)) {
            ItemStack shown = getSlot(slotIndex).getStack();
            if (!shown.isEmpty()) {
                ItemStack give = shown.copy();
                player.getInventory().insertStack(give);
                if (!give.isEmpty()) player.dropItem(give, false);
            }
        }
        // Nenhuma outra interação é permitida; ressincroniza o cliente (cursor/slots)
        syncState();
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) { return ItemStack.EMPTY; }
}
