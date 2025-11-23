package io.github.vickye2.vickyesrelooter.client.gui;

import io.github.vickye2.vickyesrelooter.client.gui.widgets.*;
import io.github.vickye2.vickyesrelooter.data.LootableHolder;
import io.github.vickye2.vickyesrelooter.network.PacketHandler;
import io.github.vickye2.vickyesrelooter.network.packets.CreateTablePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class LootTableCreationScreen extends Screen {
    private final Minecraft mc = Minecraft.getInstance();

    private ItemStack inputStack = ItemStack.EMPTY;
    private final List<LootableHolder.Lootable> lootables = new ArrayList<>();

    private EditBox idField, emptyWeight, tableWeight;

    private EditBox lootableNameField, lootableDescField, lootableWeight;
    private ColorPickerWidget textColorPicker, descriptionColorPicker;
    private IntegerPickerWidget minAmount, maxAmount;

    private int slotY;
    private Integer minAmountValue = 1;
    private Integer maxAmountValue = 64;

    final int LENGTH = 9*18 - 10;
    private final List<AbstractWidget> lootableWidgets = new ArrayList<>();

    public LootTableCreationScreen() {
        super(Component.literal("Create Loot Table"));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;

        int y = 20;
        int spacing = 20;

        idField = new EditBox(this.font, centerX - 100, y, 200, 20, Component.literal("Table ID"));
        this.addRenderableWidget(idField);
        y += 20 + spacing;

        emptyWeight = new EditBox(this.font, centerX - 100, y, 200, 20, Component.literal("Emptiness Weight"));
        this.addRenderableWidget(emptyWeight);
        y += 20 + spacing;

        tableWeight = new EditBox(this.font, centerX - 100, y, 200, 20, Component.literal("Table Weight"));
        this.addRenderableWidget(tableWeight);
        y += 40 + spacing;

        int invX = this.width - 9*18 - 10;
        int invY = 10;

        lootableNameField = new EditBox(this.font, 20, y, LENGTH, 20, Component.literal("Lootable Name"));
        this.addRenderableWidget(lootableNameField);

        lootableDescField = new EditBox(this.font, 20 + LENGTH + 20, y, LENGTH, 20, Component.literal("Lootable Description"));
        this.addRenderableWidget(lootableDescField);

        y += spacing + 5;

        textColorPicker = new ColorPickerWidget(19, y, (colorInt) -> {
            lootableNameField.setTextColor(colorInt);
            return null;
        });
        this.addRenderableWidget(textColorPicker);


        descriptionColorPicker = new ColorPickerWidget(20 + 170, y, (colorInt) -> {
            lootableDescField.setTextColor(colorInt);
            return null;
        });
        this.addRenderableWidget(descriptionColorPicker);
        y += 70 + spacing;

        slotY = 4*18 + 30;

        maxAmount = new IntegerPickerWidget(
                20, y,
                () -> minAmountValue,
                () -> 64,
                value -> {
                    maxAmountValue = value;
                    return null;
                }
        );
        addRenderableWidget(maxAmount);

        minAmount = new IntegerPickerWidget(
                200, y,
                () -> 1,
                () -> maxAmountValue,
                value -> {
                    minAmountValue = value;
                    return null;
                }
        );
        this.addRenderableWidget(minAmount);
        y += 40 + spacing;

        lootableWeight = new EditBox(this.font, 20, y, LENGTH, 20, Component.literal("Lootable Weight"));
        lootableWeight.setValue(String.valueOf(1));
        lootableWeight.setResponder(text -> {
            if (!text.matches("\\d*")) {
                lootableWeight.setValue(text.replaceAll("^\\d", ""));
            }
        });
        this.addRenderableWidget(lootableWeight);

        this.addRenderableWidget(new PlayerInventoryWidget(invX, invY, clickedStack -> inputStack = clickedStack));
        this.addRenderableWidget(Button.builder(Component.literal("+"), btn -> {
            if (!inputStack.isEmpty()) {
                LootableHolder.Lootable lootable
                        = new LootableHolder.Lootable();
                lootable.id = "";
                lootable.weight = Integer.parseInt(lootableWeight.getValue());
                lootable.maxAmount = maxAmountValue;
                lootable.minAmount = minAmountValue;
                lootable.description = lootableDescField.getValue();
                lootable.name = lootableNameField.getValue();
                lootable.textColor = textColorPicker.getColorInt();
                lootable.descriptionColor = descriptionColorPicker.getColorInt();

                lootables.add(lootable);
                inputStack = ItemStack.EMPTY;

                lootableWeight.setValue("0");
                lootableDescField.setValue("");
                lootableNameField.setValue("");
            }
        }).bounds(width - 30, 4*18 + 30, 20, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Confirm"), btn -> {
            confirmTable();
        }).bounds(width - 9*18, height - 30, 9*18 - 10, 20).build());
    }

    public void initItemGrid(GuiGraphics stack) {
        int slotSize = 18;
        int padding = 4;
        int itemsPerRow = 9;
        int startX = 10;
        int startY = 10;

        int rows = (lootables.size() + itemsPerRow - 1) / itemsPerRow;
        rows = Math.max(rows, 1);
        int bgWidth = itemsPerRow * slotSize + padding * 2;
        int bgHeight = rows * slotSize + padding * 2;
        stack.fill(startX - padding, startY - padding, startX - padding + bgWidth, startY - padding + bgHeight, 0x44447777); // semi-transparent black
        for (int i = 0; i < lootables.size(); i++) {
            int finalI = i;
            int row = i / itemsPerRow;
            int col = i % itemsPerRow;

            int x = startX + col * slotSize;
            int y = startY + row * slotSize;

            ItemStack s = lootables.get(i).createStack(mc.font.random);
            ClickActionableItem widget = new ClickActionableItem(x, y, s, (thiz) -> {
                lootables.remove(finalI);
                lootableWidgets.remove(finalI);
                removeWidget(thiz);
                rebuildGrid(stack);
                return null;
            });
            lootableWidgets.add(i, widget);
            addRenderableWidget(widget);
        }
    }

    private void rebuildGrid(GuiGraphics stack) {
        // Remove old grid widgets
        lootableWidgets.forEach(this::removeWidget);
        lootableWidgets.clear();

        // Rebuild everything
        initItemGrid(stack);
    }


    private void confirmTable() {
        String id = lootableNameField.getValue().trim();
        int empty = Integer.parseInt(emptyWeight.getValue().trim());
        int table = Integer.parseInt(tableWeight.getValue().trim());

        if (id.isEmpty()) {
            return;
        }

        CreateTablePacket packet = new CreateTablePacket(id, empty, table, lootables);
        PacketHandler.INSTANCE.sendToServer(packet);
        this.onClose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(@NotNull GuiGraphics stack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(stack);
        assert mc.screen != null;
        stack.fill(lootableNameField.getX() - 30, lootableNameField.getY() - 30, lootableDescField.getX() + LENGTH + 30, lootableWeight.getY() + 30, 0xCC000C30); // slot border

        stack.drawCenteredString(this.font, this.title, this.width / 2, this.height - 10, 0xFFFFFF);
        stack.drawString(this.font, "Table Id:", idField.getX(), idField.getY() - 15, 0xFFFFFF);
        stack.drawString(this.font, "Table Empty Weight:", emptyWeight.getX(), emptyWeight.getY() - 15, 0xFFFFFF);
        stack.drawString(this.font, "Table Weight:", tableWeight.getX(), tableWeight.getY() - 15, 0xFFFFFF);
        stack.drawString(this.font, "Lootable Weight:", lootableWeight.getX(), lootableWeight.getY() - 15, 0xFFFFFF);
        stack.drawString(this.font, "Lootable Name:", lootableNameField.getX(), lootableNameField.getY() - 15, 0xFFFFFF);
        stack.drawString(this.font, "Lootable Description:", lootableDescField.getX(), lootableDescField.getY() - 15, 0xFFFFFF);
        stack.drawString(this.font, "Lootable Min Amount:", minAmount.getX(), minAmount.getY() - 15, 0xFFFFFF);
        stack.drawString(this.font, "Lootable Max Amount:", maxAmount.getX(), maxAmount.getY() - 15, 0xFFFFFF);
        super.render(stack, mouseX, mouseY, partialTicks);
        int slotX = width - 57;
        stack.fill(slotX, 4*18 + 30, slotX + 20, 4*18 + 30 + 20, 0x44447777); // slot border
        if (!inputStack.isEmpty()) {
            stack.renderItem(inputStack, slotX, slotY);
            stack.renderItemDecorations(mc.font, inputStack, slotX, slotY);
            if (mouseX >= slotX && mouseX <= slotX + 16 && mouseY >= slotY && mouseY <= slotY + 16) {
                stack.renderTooltip(font, inputStack, mouseX, mouseY);
            }
        }

        initItemGrid(stack);
    }
}
