package com.remagic.remagicmod.screens;

import com.remagic.remagicmod.diary.BookCategory;
import com.remagic.remagicmod.diary.BookEntry;
import com.remagic.remagicmod.diary.BookLoader;
import com.remagic.remagicmod.diary.BookPage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.ShapedRecipe;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class Diary extends Screen {

    private static final ResourceLocation MAIN_TEXTURE = ResourceLocation.fromNamespaceAndPath("remagic", "textures/gui/diary.png");

    private final int imageWidth = 392;
    private final int imageHeight = 252;
    private int leftPos;
    private int topPos;

    private DiaryPageButton forwardButton;
    private DiaryPageButton backButton;
    private DiaryBackButton homeButton;

    private ItemStack hoveredTooltipStack = ItemStack.EMPTY;

    public Diary() {
        super(Component.translatable("remagic.gui.diary.title"));
    }

    public enum BookState {
        CATEGORIES,
        ENTRIES,
        READING
    }

    // Текущее состояние книги
    private BookState currentState = BookState.CATEGORIES;
    private BookCategory currentCategory = null;
    private BookEntry currentEntry = null;

    // Номер текущей левой страницы (0, 2, 4...)
    private int pageIndex = 0;

    private List<BookEntry> getEntriesForCurrentCategory() {
        if (this.currentCategory == null) return List.of();
        return BookLoader.getEntriesForCategory(this.currentCategory.id());
    }

    @Override
    protected void init() {
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;

        int buttonY = this.topPos + this.imageHeight - 8;

        // Кнопка "Назад по страницам" (слева)
        this.backButton = this.addRenderableWidget(
                new DiaryPageButton(this.width / 2 - 55, buttonY, false, btn -> pageBack(), true)
        );

        // Кнопка "Домой / Назад" (посередине между стрелками)
        this.homeButton = this.addRenderableWidget(
                new DiaryBackButton(this.width / 2 - 12, buttonY + 8, btn -> pageHome(), true)
        );

        // Кнопка "Вперед по страницам" (справа)
        this.forwardButton = this.addRenderableWidget(
                new DiaryPageButton(this.width / 2 + 30, buttonY, true, btn -> pageForward(), true)
        );

        updateButtonVisibility();
    }

    private void pageForward() {
        this.setFocused(null);
        if (this.currentEntry != null && this.pageIndex + 2 < this.currentEntry.pages().size()) {
            this.pageIndex += 2;
            updateButtonVisibility();
        }
    }

    private void pageBack() {
        this.setFocused(null);
        if (this.pageIndex >= 2) {
            this.pageIndex -= 2;
            updateButtonVisibility();
        }
    }

    private void pageHome() {
        this.setFocused(null);
        switch (this.currentState) {
            case READING -> {
                this.currentState = BookState.ENTRIES;
                this.currentEntry = null;
                this.pageIndex = 0;
            }
            case ENTRIES -> {
                this.currentState = BookState.CATEGORIES;
                this.currentCategory = null;
                this.pageIndex = 0;
            }
            default -> {}
        }
        updateButtonVisibility();
    }

    private void openCategory(BookCategory category) {
        this.currentCategory = category;
        this.currentState = BookState.ENTRIES;
        this.pageIndex = 0;
        playPageTurnSound();
        updateButtonVisibility();
    }

    private void openEntry(BookEntry entry) {
        this.currentEntry = entry;
        this.currentState = BookState.READING;
        this.pageIndex = 0;
        playPageTurnSound();
        updateButtonVisibility();
    }

    private void playPageTurnSound() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
    }

    private void updateButtonVisibility() {
        boolean isReading = (this.currentState == BookState.READING && this.currentEntry != null);

        this.backButton.visible = isReading && (this.pageIndex > 0);
        this.forwardButton.visible = isReading && (this.pageIndex + 2 < this.currentEntry.pages().size());

        // Кнопка "Домой" скрыта только на главном экране категорий
        this.homeButton.visible = (this.currentState != BookState.CATEGORIES);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Сначала проверяем нажатия по кнопкам стрелок и возврата
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        // Проверяем клики по спискам на правой странице (левый клик = 0)
        if (button == 0) {
            int rightPageX = this.leftPos + 215;
            int maxTextWidth = 120;

            if (this.currentState == BookState.CATEGORIES) {
                int y = this.topPos + 35;
                for (BookCategory category : BookLoader.getCategories()) {
                    if (mouseX >= rightPageX && mouseX <= rightPageX + maxTextWidth && mouseY >= y && mouseY < y + 20) {
                        openCategory(category);
                        return true;
                    }
                    y += 24;
                }
            } else if (this.currentState == BookState.ENTRIES && this.currentCategory != null) {
                int y = this.topPos + 35;
                for (BookEntry entry : getEntriesForCurrentCategory()) {
                    if (mouseX >= rightPageX && mouseX <= rightPageX + maxTextWidth && mouseY >= y && mouseY < y + 14) {
                        openEntry(entry);
                        return true;
                    }
                    y += 16;
                }
            }
        }

        return false;
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTransparentBackground(guiGraphics);
        guiGraphics.blit(MAIN_TEXTURE, this.leftPos, this.topPos, this.imageWidth, this.imageHeight, 0.0f, 0.0f, 280, 180, 512, 256);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.hoveredTooltipStack = ItemStack.EMPTY;

        super.render(guiGraphics, mouseX, mouseY, partialTick);

        switch (this.currentState) {
            case CATEGORIES -> renderCategoriesView(guiGraphics, mouseX, mouseY);
            case ENTRIES -> renderEntriesView(guiGraphics, mouseX, mouseY);
            case READING -> {
                if (this.currentEntry != null) {
                    renderPages(guiGraphics, mouseX, mouseY);
                }
            }
        }

        // Рендерим всплывающую подсказку поверх всего интерфейса
        if (!this.hoveredTooltipStack.isEmpty()) {
            guiGraphics.renderTooltip(this.font, this.hoveredTooltipStack, mouseX, mouseY);
        }
    }

    private void renderCategoriesView(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int leftPageX = this.leftPos + 58;
        int rightPageX = this.leftPos + 215;
        int titleY = this.topPos + 12;
        int maxTextWidth = 120;

        // Левая страница: Заголовок и описание книги
        guiGraphics.drawString(this.font, Component.translatable("remagic.gui.diary.about_title"), leftPageX, titleY, 0x000000, false);
        Component intro = Component.translatable("remagic.gui.diary.about_text");
        guiGraphics.drawWordWrap(this.font, intro, leftPageX, this.topPos + 32, maxTextWidth, 0x444444);

        // Правая страница: Заголовок и список глав (разделов)
        guiGraphics.drawString(this.font, Component.translatable("remagic.gui.diary.categories_title"), rightPageX, titleY, 0x000000, false);

        int y = this.topPos + 35;
        for (BookCategory category : BookLoader.getCategories()) {
            boolean hovered = mouseX >= rightPageX && mouseX <= rightPageX + maxTextWidth && mouseY >= y && mouseY < y + 20;

            // Иконка предмета
            guiGraphics.renderFakeItem(category.icon(), rightPageX, y);

            // Название категории (подсвечивается бордовым при наведении)
            int textColor = hovered ? 0x9E2A2B : 0x000000;
            guiGraphics.drawString(this.font, category.title(), rightPageX + 20, y + 4, textColor, false);

            y += 24;
        }
    }

    private void renderEntriesView(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int leftPageX = this.leftPos + 58;
        int rightPageX = this.leftPos + 215;
        int titleY = this.topPos + 12;
        int maxTextWidth = 120;

        if (this.currentCategory != null) {
            // Левая страница: Заголовок раздела и описание
            guiGraphics.drawString(this.font, this.currentCategory.title(), leftPageX, titleY, 0x000000, false);

            Component desc = this.currentCategory.description();
            guiGraphics.drawWordWrap(this.font, desc, leftPageX, this.topPos + 32, maxTextWidth, 0x444444);

            // Правая страница: Заголовок списка и список статей
            guiGraphics.drawString(this.font, Component.translatable("remagic.gui.diary.entries_title"), rightPageX, titleY, 0x000000, false);

            int y = this.topPos + 35;
            for (BookEntry entry : getEntriesForCurrentCategory()) {
                boolean hovered = mouseX >= rightPageX && mouseX <= rightPageX + maxTextWidth && mouseY >= y && mouseY < y + 14;

                int textColor = hovered ? 0x9E2A2B : 0x222222;
                String prefix = hovered ? "▶ " : "• ";
                Component itemText = Component.literal(prefix).append(entry.title());
                guiGraphics.drawString(this.font, itemText, rightPageX, y, textColor, false);

                y += 16;
            }
        }
    }

    private void renderPages(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int leftPageX = this.leftPos + 58;
        int rightPageX = this.leftPos + 215;
        int textY = this.topPos + 32;
        int maxTextWidth = 120;

        // --- ЛЕВАЯ СТРАНИЦА (pageIndex) ---
        if (this.pageIndex < this.currentEntry.pages().size()) {
            BookPage leftPage = this.currentEntry.pages().get(this.pageIndex);
            boolean isFirstPage = (this.pageIndex == 0);
            renderSinglePage(guiGraphics, leftPage, leftPageX, textY, maxTextWidth, isFirstPage, mouseX, mouseY);

            String pageNum = String.valueOf(this.pageIndex + 1);
            guiGraphics.drawString(this.font, pageNum, leftPageX + maxTextWidth / 2 - this.font.width(pageNum) / 2, this.topPos + this.imageHeight - 60, 0x555555, false);
        }

        // --- ПРАВАЯ СТРАНИЦА (pageIndex + 1) ---
        int nextPageIndex = this.pageIndex + 1;
        if (nextPageIndex < this.currentEntry.pages().size()) {
            BookPage rightPage = this.currentEntry.pages().get(nextPageIndex);
            renderSinglePage(guiGraphics, rightPage, rightPageX, textY, maxTextWidth, false, mouseX, mouseY);

            String pageNum = String.valueOf(nextPageIndex + 1);
            guiGraphics.drawString(this.font, pageNum, rightPageX + maxTextWidth / 2 - this.font.width(pageNum) / 2, this.topPos + this.imageHeight - 60, 0x555555, false);
        }
    }

    private void renderSinglePage(GuiGraphics guiGraphics, BookPage page, int pageX, int pageY, int maxTextWidth, boolean isFirstPage, int mouseX, int mouseY) {
        int currentY = pageY;

        if (isFirstPage) {
            int titleY = this.topPos + 12;
            guiGraphics.drawString(this.font, this.currentEntry.title(), pageX, titleY, 0x000000, false);
        }

        switch (page) {
            case BookPage.Text textPage -> {
                guiGraphics.drawWordWrap(this.font, textPage.text(), pageX, currentY, maxTextWidth, 0x000000);
            }
            case BookPage.Image imagePage -> {
                if (imagePage.title() != null) {
                    int titleX = pageX + (maxTextWidth - this.font.width(imagePage.title())) / 2;
                    guiGraphics.drawString(this.font, imagePage.title(), titleX, currentY, 0x000000, false);
                    currentY += 14;
                }

                int imgX = pageX + (maxTextWidth - imagePage.width()) / 2;
                guiGraphics.blit(
                        imagePage.image(),
                        imgX,
                        currentY,
                        imagePage.width(),
                        imagePage.height(),
                        0.0f,
                        0.0f,
                        imagePage.textureWidth(),
                        imagePage.textureHeight(),
                        imagePage.textureWidth(),
                        imagePage.textureHeight()
                );
                currentY += imagePage.height() + 8;

                if (imagePage.text() != null) {
                    guiGraphics.drawWordWrap(this.font, imagePage.text(), pageX, currentY, maxTextWidth, 0x000000);
                }
            }
            case BookPage.Crafting craftingPage -> {
                if (craftingPage.title() != null) {
                    int titleX = pageX + (maxTextWidth - this.font.width(craftingPage.title())) / 2;
                    guiGraphics.drawString(this.font, craftingPage.title(), titleX, currentY, 0x000000, false);
                    currentY += 14;
                }

                RecipeManager recipeManager = this.minecraft != null && this.minecraft.level != null ? this.minecraft.level.getRecipeManager() : null;
                Optional<RecipeHolder<?>> recipeOpt = (recipeManager != null && craftingPage.recipeId() != null)
                        ? recipeManager.byKey(craftingPage.recipeId())
                        : Optional.empty();

                if (recipeOpt.isPresent()) {
                    Recipe<?> recipe = recipeOpt.get().value();

                    int startGridX = pageX + (maxTextWidth - 112) / 2;
                    int startGridY = currentY + 2;

                    NonNullList<Ingredient> ingredients = recipe.getIngredients();
                    ItemStack[] gridStacks = new ItemStack[9];
                    Arrays.fill(gridStacks, ItemStack.EMPTY);

                    if (recipe instanceof ShapedRecipe shaped) {
                        int w = shaped.getWidth();
                        int h = shaped.getHeight();
                        for (int r = 0; r < h && r < 3; r++) {
                            for (int c = 0; c < w && c < 3; c++) {
                                int idx = c + r * w;
                                if (idx < ingredients.size()) {
                                    Ingredient ing = ingredients.get(idx);
                                    ItemStack[] items = ing.getItems();
                                    if (items.length > 0) {
                                        int cycle = (int) ((System.currentTimeMillis() / 1000) % items.length);
                                        gridStacks[c + r * 3] = items[cycle];
                                    }
                                }
                            }
                        }
                    } else {
                        for (int i = 0; i < ingredients.size() && i < 9; i++) {
                            Ingredient ing = ingredients.get(i);
                            ItemStack[] items = ing.getItems();
                            if (items.length > 0) {
                                int cycle = (int) ((System.currentTimeMillis() / 1000) % items.length);
                                gridStacks[i] = items[cycle];
                            }
                        }
                    }

                    // Отрисовка цельной сетки 3x3 (54x54 из diary.png)
                    guiGraphics.blit(MAIN_TEXTURE, startGridX, startGridY, 54, 54, 281.0f, 53.0f, 54, 54, 512, 256);

                    // Отрисовка предметов в сетке крафта
                    for (int r = 0; r < 3; r++) {
                        for (int c = 0; c < 3; c++) {
                            int slotX = startGridX + c * 18;
                            int slotY = startGridY + r * 18;

                            ItemStack stack = gridStacks[c + r * 3];
                            if (!stack.isEmpty()) {
                                guiGraphics.renderItem(stack, slotX + 1, slotY + 1);
                                guiGraphics.renderItemDecorations(this.font, stack, slotX + 1, slotY + 1);

                                if (mouseX >= slotX && mouseX < slotX + 18 && mouseY >= slotY && mouseY < slotY + 18) {
                                    this.hoveredTooltipStack = stack;
                                }
                            }
                        }
                    }

                    // Стрелка (24x16, выровнена по центру средней линии сетки)
                    int arrowX = startGridX + 54 + 4;
                    int arrowY = startGridY + (54 - 16) / 2;
                    guiGraphics.blit(MAIN_TEXTURE, arrowX, arrowY, 24, 16, 281.0f, 34.0f, 24, 16, 512, 256);

                    // Слот результата (26x26, выровнен по центру)
                    int resultSlotX = arrowX + 24 + 4;
                    int resultSlotY = startGridY + (54 - 26) / 2;
                    guiGraphics.blit(MAIN_TEXTURE, resultSlotX, resultSlotY, 26, 26, 319.0f, 7.0f, 26, 26, 512, 256);

                    if (this.minecraft.level != null) {
                        ItemStack resultStack = recipe.getResultItem(this.minecraft.level.registryAccess());
                        if (!resultStack.isEmpty()) {
                            guiGraphics.renderItem(resultStack, resultSlotX + 5, resultSlotY + 5);
                            guiGraphics.renderItemDecorations(this.font, resultStack, resultSlotX + 5, resultSlotY + 5);

                            if (mouseX >= resultSlotX && mouseX < resultSlotX + 26 && mouseY >= resultSlotY && mouseY < resultSlotY + 26) {
                                this.hoveredTooltipStack = resultStack;
                            }
                        }
                    }

                    currentY = startGridY + 54 + 8;
                } else {
                    String missingMsg = "Рецепт не найден:\n" + (craftingPage.recipeId() != null ? craftingPage.recipeId() : "null");
                    guiGraphics.drawWordWrap(this.font, Component.literal(missingMsg), pageX, currentY, maxTextWidth, 0x990000);
                    currentY += 28;
                }

                if (craftingPage.text() != null) {
                    guiGraphics.drawWordWrap(this.font, craftingPage.text(), pageX, currentY, maxTextWidth, 0x000000);
                }
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
