package com.remagic.remagicmod.diary;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;

import java.io.Reader;
import java.util.*;

public class BookLoader implements ResourceManagerReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final List<BookCategory> CATEGORIES = new ArrayList<>();
    private static final List<BookEntry> ENTRIES = new ArrayList<>();

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        CATEGORIES.clear();
        ENTRIES.clear();

        loadCategories(resourceManager);
        loadEntries(resourceManager);

        LOGGER.info("ReMagic Guide: Loaded {} categories and {} entries from JSON", CATEGORIES.size(), ENTRIES.size());
    }

    private void loadCategories(ResourceManager resourceManager) {
        FileToIdConverter converter = FileToIdConverter.json("guide/categories");
        Map<ResourceLocation, Resource> resources = converter.listMatchingResources(resourceManager);

        for (Map.Entry<ResourceLocation, Resource> resourceEntry : resources.entrySet()) {
            ResourceLocation fileId = converter.fileToId(resourceEntry.getKey());
            String categoryId = fileId.getPath();

            try (Reader reader = resourceEntry.getValue().openAsReader()) {
                JsonObject json = GsonHelper.parse(reader);

                String name = GsonHelper.getAsString(json, "name", "remagic.gui.category." + categoryId + ".name");
                String iconName = GsonHelper.getAsString(json, "icon", "minecraft:book");
                String description = GsonHelper.getAsString(json, "description", "remagic.gui.category." + categoryId + ".desc");
                int sortOrder = GsonHelper.getAsInt(json, "sort_order", 0);

                Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(iconName));
                ItemStack icon = new ItemStack(item != null ? item : Items.BOOK);

                CATEGORIES.add(new BookCategory(
                        categoryId,
                        Component.translatable(name),
                        icon,
                        Component.translatable(description),
                        sortOrder
                ));
            } catch (Exception e) {
                LOGGER.error("Failed to load book category: {}", resourceEntry.getKey(), e);
            }
        }

        CATEGORIES.sort(Comparator.comparingInt(BookCategory::sortOrder));
    }

    private void loadEntries(ResourceManager resourceManager) {
        FileToIdConverter converter = FileToIdConverter.json("guide/entries");
        Map<ResourceLocation, Resource> resources = converter.listMatchingResources(resourceManager);

        for (Map.Entry<ResourceLocation, Resource> resourceEntry : resources.entrySet()) {
            ResourceLocation fileId = converter.fileToId(resourceEntry.getKey());
            String entryId = fileId.getPath();

            try (Reader reader = resourceEntry.getValue().openAsReader()) {
                JsonObject json = GsonHelper.parse(reader);

                String category = GsonHelper.getAsString(json, "category", "basics");
                String name = GsonHelper.getAsString(json, "name", "remagic.gui.entry." + entryId + ".name");
                int sortOrder = GsonHelper.getAsInt(json, "sort_order", 0);

                List<BookPage> pages = new ArrayList<>();
                if (json.has("pages") && json.get("pages").isJsonArray()) {
                    for (JsonElement element : json.getAsJsonArray("pages")) {
                        if (element.isJsonPrimitive()) {
                            // Обычная текстовая страница
                            pages.add(new BookPage.Text(Component.translatable(element.getAsString())));
                        } else if (element.isJsonObject()) {
                            JsonObject pageObj = element.getAsJsonObject();
                            String type = GsonHelper.getAsString(pageObj, "type", "text");
                            Component title = pageObj.has("title") ? Component.translatable(GsonHelper.getAsString(pageObj, "title")) : null;
                            Component text = pageObj.has("text") ? Component.translatable(GsonHelper.getAsString(pageObj, "text")) : null;

                            switch (type) {
                                case "image" -> {
                                    String imgStr = GsonHelper.getAsString(pageObj, "image", "");
                                    ResourceLocation imgLoc = ResourceLocation.tryParse(imgStr);
                                    if (imgLoc == null) {
                                        imgLoc = ResourceLocation.fromNamespaceAndPath("remagic", "textures/gui/diary.png");
                                    }
                                    int width = GsonHelper.getAsInt(pageObj, "width", 100);
                                    int height = GsonHelper.getAsInt(pageObj, "height", 60);
                                    int texW = GsonHelper.getAsInt(pageObj, "texture_width", width);
                                    int texH = GsonHelper.getAsInt(pageObj, "texture_height", height);

                                    pages.add(new BookPage.Image(title, imgLoc, width, height, texW, texH, text));
                                }
                                case "crafting", "recipe" -> {
                                    String recipeStr = GsonHelper.getAsString(pageObj, "recipe", "");
                                    ResourceLocation recipeLoc = ResourceLocation.tryParse(recipeStr);
                                    pages.add(new BookPage.Crafting(title, recipeLoc, text));
                                }
                                default -> {
                                    String textStr = GsonHelper.getAsString(pageObj, "text", "");
                                    pages.add(new BookPage.Text(Component.translatable(textStr)));
                                }
                            }
                        }
                    }
                }

                ENTRIES.add(new BookEntry(
                        entryId,
                        category,
                        Component.translatable(name),
                        pages,
                        sortOrder
                ));
            } catch (Exception e) {
                LOGGER.error("Failed to load book entry: {}", resourceEntry.getKey(), e);
            }
        }

        ENTRIES.sort(Comparator.comparingInt(BookEntry::sortOrder));
    }

    public static List<BookCategory> getCategories() {
        return Collections.unmodifiableList(CATEGORIES);
    }

    public static List<BookEntry> getEntriesForCategory(String categoryId) {
        return ENTRIES.stream()
                .filter(e -> e.categoryId().equals(categoryId))
                .toList();
    }
}
