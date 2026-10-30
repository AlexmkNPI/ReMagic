package com.remagic.remagicmod.screens;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;

public class DiaryBackButton extends Button {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("remagic", "textures/gui/diary.png");
    private final boolean playTurnSound;

    public DiaryBackButton(int x, int y, Button.OnPress onPress, boolean playTurnSound) {
        super(x, y, 25, 14, CommonComponents.EMPTY, onPress, DEFAULT_NARRATION);
        this.playTurnSound = playTurnSound;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Белая (не нажата) = 281, Красная (нажата / наведена) = 300
        // Используем isHovered(), чтобы кнопка не "залипала" в фокусе после клика
        int u = this.isHovered() ? 300 : 281;
        guiGraphics.blit(TEXTURE, this.getX(), this.getY(), this.width, this.height, (float) u, (float) 23, 18, 10, 512, 256);
    }

    @Override
    public void playDownSound(SoundManager handler) {
        if (this.playTurnSound) {
            handler.play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
        }
    }
}
