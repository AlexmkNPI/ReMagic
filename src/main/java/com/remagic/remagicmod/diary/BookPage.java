package com.remagic.remagicmod.diary;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public sealed interface BookPage permits BookPage.Text, BookPage.Image, BookPage.Crafting {

    record Text(Component text) implements BookPage {}

    record Image(
            Component title,
            ResourceLocation image,
            int width,
            int height,
            int textureWidth,
            int textureHeight,
            Component text
    ) implements BookPage {}

    record Crafting(
            Component title,
            ResourceLocation recipeId,
            Component text
    ) implements BookPage {}
}
