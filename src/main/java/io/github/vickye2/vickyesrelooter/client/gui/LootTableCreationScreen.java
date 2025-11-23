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
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
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
        super(Component.literal(""));
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
        emptyWeight.setResponder(text -> {
            if (!text.matches("\\d*")) {
                // Only update if it actually changes
                String sanitized = text.replaceAll("\\D", ""); // remove non-digit
                if (!sanitized.equals(text)) {
                    emptyWeight.setValue(sanitized); // safe now
                }
            }
        });
        this.addRenderableWidget(emptyWeight);
        y += 20 + spacing;

        tableWeight = new EditBox(this.font, centerX - 100, y, 200, 20, Component.literal("Table Weight"));
        tableWeight.setResponder(text -> {
            if (!text.matches("\\d*")) {
                // Only update if it actually changes
                String sanitized = text.replaceAll("\\D", ""); // remove non-digit
                if (!sanitized.equals(text)) {
                    tableWeight.setValue(sanitized); // safe now
                }
            }
        });
        this.addRenderableWidget(tableWeight);
        y += 40 + spacing;

        int invX = this.width - 9*18 - 10;
        int invY = 10;

        lootableNameField = new EditBox(this.font, 20, y, LENGTH, 20, Component.literal("Lootable Name"));
        this.addRenderableWidget(lootableNameField);

        lootableDescField = new EditBox(this.font, 20 + LENGTH + 20, y, LENGTH, 20, Component.literal("Lootable Description"));
        this.addRenderableWidget(lootableDescField);

        y += spacing + 5;

        textColorPicker = new ColorPickerWidget(19, y,
                (colorInt) -> lootableNameField.setTextColor(colorInt));
        this.addRenderableWidget(textColorPicker);

        descriptionColorPicker = new ColorPickerWidget(20 + 170, y,
                (colorInt) -> lootableDescField.setTextColor(colorInt));
        this.addRenderableWidget(descriptionColorPicker);
        y += 70 + spacing;

        slotY = 4*18 + 30;

        maxAmount = new IntegerPickerWidget(
                20, y,
                () -> minAmountValue,
                () -> 64,
                value -> maxAmountValue = value
        );
        addRenderableWidget(maxAmount);

        minAmount = new IntegerPickerWidget(
                200, y,
                () -> 1,
                () -> maxAmountValue,
                value -> minAmountValue = value
        );
        this.addRenderableWidget(minAmount);
        y += 40 + spacing;

        lootableWeight = new EditBox(this.font, 20, y, LENGTH, 20, Component.literal("Lootable Weight"));
        lootableWeight.setValue(String.valueOf(1));
        lootableWeight.setResponder(text -> {
            if (!text.matches("\\d*")) {
                // Only update if it actually changes
                String sanitized = text.replaceAll("\\D", ""); // remove non-digit
                if (!sanitized.equals(text)) {
                    lootableWeight.setValue(sanitized); // safe now
                }
            }
        });
        this.addRenderableWidget(lootableWeight);

        this.addRenderableWidget(new PlayerInventoryWidget(invX, invY, clickedStack -> {
            inputStack = clickedStack;
            lootableNameField.setValue(getItemName(inputStack));
            textColorPicker.setValue(getItemColor(inputStack));
            maxAmount.setValue(inputStack.getCount());
            maxAmountValue = inputStack.getCount();
            maxAmount.setValue(maxAmountValue);
        }));
        this.addRenderableWidget(Button.builder(Component.literal("+"), btn -> {
            if (!inputStack.isEmpty()) {
                LootableHolder.Lootable lootable
                        = new LootableHolder.Lootable();
                lootable.id = ForgeRegistries.ITEMS.getKey(inputStack.getItem()).toString();
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
                initItemGrid();
            }
        }).bounds(width - 30, 4*18 + 30, 20, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Confirm"), btn -> {
            confirmTable();
        }).bounds(width - 9*18, height - 30, 9*18 - 10, 20).build());

        initItemGrid();
    }

    public static String getItemName(ItemStack stack) {
        Component name = stack.getHoverName(); // gets the display name
        return name.getString();               // plain string without formatting
    }

    public static int getItemColor(ItemStack stack) {
        Component name = stack.getHoverName();
        Style style = name.getStyle();

        // getTextColor() returns TextColor, which has a packed RGB value
        TextColor color = style.getColor();
        return color != null ? color.getValue() : 0xFFFFFF; // default to white if no color
    }


    public void initItemGrid() {
        // remove existing widgets first
        for (AbstractWidget w : lootableWidgets) removeWidget(w);
        lootableWidgets.clear();

        int slotSize = 18;
        int padding = 4;
        int itemsPerRow = 9;
        int startX = 10;
        int startY = 10;

        int rows = (lootables.size() + itemsPerRow - 1) / itemsPerRow;
        rows = Math.max(rows, 1);

        for (int i = 0; i < lootables.size(); i++) {
            LootableHolder.Lootable lootable = lootables.get(i);           // keep ref
            int row = i / itemsPerRow;
            int col = i % itemsPerRow;

            int x = startX + col * slotSize;
            int y = startY + row * slotSize;

            ItemStack s = lootable.createStack(mc.level.random); // or mc.font.random if you have it

            // Use Consumer<ClickActionableItem> ideally; here we accept a widget param
            ClickActionableItem widget = new ClickActionableItem(x, y, s, (thiz) -> {
                // remove the Lootable object (safe) instead of removing by index
                lootables.remove(lootable);
                // remove this exact widget instance
                removeWidget(thiz);
                lootableWidgets.remove(thiz);

                // rebuild grid to re-layout and re-create widgets
                initItemGrid();
            });

            lootableWidgets.add(widget);
            addRenderableWidget(widget);
        }
    }

    private void rebuildGrid() {
        initItemGrid();
    }

    private void confirmTable() {
        String id = idField.getValue().trim();
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
        boolean handled = super.mouseClicked(mouseX, mouseY, button); // this will call widgets
        return handled || super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(@NotNull GuiGraphics stack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(stack);
        assert mc.screen != null;
        stack.fill(lootableNameField.getX() - 30, lootableNameField.getY() - 30, lootableDescField.getX() + LENGTH + 30, lootableWeight.getY() + 30, 0x77000CAA); // slot border

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
    }
}
