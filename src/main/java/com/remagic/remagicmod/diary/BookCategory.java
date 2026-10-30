package com.remagic.remagicmod.diary;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public record BookCategory(
        String id,
        Component title,
        ItemStack icon,
        Component description,
        int sortOrder
) {}
