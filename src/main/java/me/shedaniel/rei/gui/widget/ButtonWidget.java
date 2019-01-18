package me.shedaniel.rei.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public abstract class ButtonWidget extends Gui implements IWidget {
    
    protected static final ResourceLocation WIDGET_TEX = new ResourceLocation("textures/gui/widgets.png");
    public int x;
    public int y;
    public String text;
    public boolean enabled;
    public boolean visible;
    protected int width;
    protected int height;
    protected boolean hovered;
    private boolean pressed;
    private Rectangle bounds;
    
    public ButtonWidget(Rectangle rectangle, String text) {
        this(rectangle.x, rectangle.y, rectangle.width, rectangle.height, text);
    }
    
    public ButtonWidget(int x, int y, int width, int height, String text) {
        this.width = 200;
        this.height = 20;
        this.enabled = true;
        this.visible = true;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.text = text;
        this.bounds = new Rectangle(x, this.y, this.width, this.height);
    }
    
    public Rectangle getBounds() {
        return bounds;
    }
    
    protected int getTextureId(boolean boolean_1) {
        int int_1 = 1;
        if (!this.enabled) {
            int_1 = 0;
        } else if (boolean_1) {
            int_1 = 2;
        }
        
        return int_1;
    }
    
    @Override
    public List<IWidget> getListeners() {
        return new ArrayList<>();
    }
    
    @Override
    public void draw(int mouseX, int mouseY, float partialTicks) {
        
        if (this.visible) {
            Minecraft minecraftClient_1 = Minecraft.getInstance();
            FontRenderer fontRenderer_1 = minecraftClient_1.fontRenderer;
            minecraftClient_1.getTextureManager().bindTexture(WIDGET_TEX);
            GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
            this.hovered = bounds.contains(mouseX, mouseY);
            int textureOffset = this.getTextureId(this.hovered);
            GlStateManager.enableBlend();
            GlStateManager.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            //Four Corners
            this.drawTexturedModalRect(this.x, this.y, 0, 46 + textureOffset * 20, 4, 4);
            this.drawTexturedModalRect(this.x + this.width - 4, this.y, 196, 46 + textureOffset * 20, 4, 4);
            this.drawTexturedModalRect(this.x, this.y + this.height - 4, 0, 62 + textureOffset * 20, 4, 4);
            this.drawTexturedModalRect(this.x + this.width - 4, this.y + this.height - 4, 196, 62 + textureOffset * 20, 4, 4);
            
            //Sides
            this.drawTexturedModalRect(this.x + 4, this.y, 4, 46 + textureOffset * 20, this.width - 8, 4);
            this.drawTexturedModalRect(this.x + 4, this.y + this.height - 4, 4, 62 + textureOffset * 20, this.width - 8, 4);
            
            for(int i = this.y + 4; i < this.y + this.height - 4; i += 4) {
                this.drawTexturedModalRect(this.x, i, 0, 50 + textureOffset * 20, this.width / 2, MathHelper.clamp(this.y + this.height - 4 - i, 0, 4));
                this.drawTexturedModalRect(this.x + this.width / 2, i, 200 - this.width / 2, 50 + textureOffset * 20, this.width / 2, MathHelper.clamp(this.y + this.height - 4 - i, 0, 4));
            }
            
            int colour = 14737632;
            if (!this.enabled) {
                colour = 10526880;
            } else if (this.hovered) {
                colour = 16777120;
            }
            
            this.drawCenteredString(fontRenderer_1, this.text, this.x + this.width / 2, this.y + (this.height - 8) / 2, colour);
        }
    }
    
    @Override
    public boolean onMouseClick(int button, double mouseX, double mouseY) {
        if (bounds.contains(mouseX, mouseY) && enabled) {
            Minecraft.getInstance().getSoundHandler().play(SimpleSound.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            onPressed(button, mouseX, mouseY);
            return true;
        }
        return false;
    }
    
    public abstract void onPressed(int button, double mouseX, double mouseY);
    
}
