package com.remagic.remagicmod.items;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import static com.remagic.remagicmod.ReMagic.MODID;

public class ModItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);

    public static final DeferredItem<Item> DIARY = ITEMS.registerSimpleItem("diary", new Item.Properties().stacksTo(1));
}
