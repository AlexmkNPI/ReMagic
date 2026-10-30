package com.remagic.remagicmod.events;

import com.remagic.remagicmod.ReMagic;
import com.remagic.remagicmod.items.ModItems;
import com.remagic.remagicmod.screens.Diary;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(value = Dist.CLIENT, modid = ReMagic.MODID)
public class ClientEvents {

    @SubscribeEvent
    public static void OpenDiary(PlayerInteractEvent.RightClickItem event) {
        if (!event.getLevel().isClientSide())
            return;

        if (event.getItemStack().getItem() == ModItems.DIARY.get()) {
            net.minecraft.client.Minecraft.getInstance().setScreen(new Diary());
        }
    }
}
