package com.remagic.remagicmod.diary;

import net.minecraft.network.chat.Component;
import java.util.List;

public record BookEntry(
        String id,
        String categoryId,
        Component title,
        List<BookPage> pages,
        int sortOrder
) {}
