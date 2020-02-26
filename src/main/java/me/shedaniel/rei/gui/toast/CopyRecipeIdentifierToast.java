/*
 * This file is licensed under the MIT License, part of Roughly Enough Items.
 * Copyright (c) 2018, 2019, 2020 shedaniel
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package me.shedaniel.rei.gui.toast;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Internal
public class CopyRecipeIdentifierToast implements Toast {
    
    protected static final Identifier TOASTS_TEX = new Identifier("roughlyenoughitems", "textures/gui/toasts.png");
    private String title;
    private String subtitle;
    private long startTime;
    
    public CopyRecipeIdentifierToast(String title, @Nullable String subtitleNullable) {
        this.title = title;
        this.subtitle = subtitleNullable;
    }
    
    public static void addToast(String title, @Nullable String subtitleNullable) {
        MinecraftClient.getInstance().getToastManager().add(new CopyRecipeIdentifierToast(title, subtitleNullable));
    }
    
    @Override
    public Visibility draw(ToastManager toastManager, long var2) {
        toastManager.getGame().getTextureManager().bindTexture(TOASTS_TEX);
        RenderSystem.color3f(1.0F, 1.0F, 1.0F);
        toastManager.blit(0, 0, 0, 0, 160, 32);
        if (this.subtitle == null) {
            toastManager.getGame().textRenderer.draw(this.title, 18.0F, 12.0F, 11141120);
        } else {
            toastManager.getGame().textRenderer.draw(this.title, 18.0F, 7.0F, 11141120);
            toastManager.getGame().textRenderer.draw(this.subtitle, 18.0F, 18.0F, -16777216);
        }
        
        return var2 - this.startTime < 5000L ? Visibility.SHOW : Visibility.HIDE;
    }
    
    @Override
    public Object getType() {
        return Type.THIS_IS_SURE_A_TYPE;
    }
    
    public enum Type {
        THIS_IS_SURE_A_TYPE
    }
    
}
