package com.lowdragmc.lowdraglib.gui.widget;

import com.google.common.collect.Lists;
import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.gui.editor.annotation.ConfigSetter;
import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.editor.configurator.IConfigurableWidget;
import com.lowdragmc.lowdraglib.gui.ingredient.IGhostIngredientTarget;
import com.lowdragmc.lowdraglib.gui.ingredient.Target;
import com.lowdragmc.lowdraglib.gui.util.DrawerHelper;
import com.lowdragmc.lowdraglib.gui.util.TextFormattingUtil;
import com.lowdragmc.lowdraglib.misc.ItemStackTransfer;
import com.lowdragmc.lowdraglib.side.fluid.FluidTransferHelper;
import com.lowdragmc.lowdraglib.side.fluid.FluidStack;
import com.lowdragmc.lowdraglib.side.fluid.IFluidTransfer;
import com.lowdragmc.lowdraglib.utils.Position;
import com.lowdragmc.lowdraglib.utils.Size;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.emi.emi.api.stack.EmiStack;
import lombok.Getter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

import javax.annotation.Nonnull;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

@LDLRegister(name = "phantom_fluid_slot", group = "widget.container")
public class PhantomFluidWidget extends TankWidget implements IGhostIngredientTarget, IConfigurableWidget {

    private final Supplier<FluidStack> phantomFluidGetter;
    private final Consumer<FluidStack> phantomFluidSetter;

    @Nullable
    @Getter
    protected FluidStack lastPhantomStack;

    public PhantomFluidWidget(@Nullable IFluidTransfer fluidTank, int tank, int x, int y, int width, int height,
                              Supplier<FluidStack> phantomFluidGetter, Consumer<FluidStack> phantomFluidSetter) {
        super(fluidTank, tank, x, y, width, height, false, false);
        this.phantomFluidGetter = phantomFluidGetter;
        this.phantomFluidSetter = phantomFluidSetter;
    }

    @ConfigSetter(field = "allowClickFilled")
    public PhantomFluidWidget setAllowClickFilled(boolean v) {
        // you cant modify it
        return this;
    }

    @ConfigSetter(field = "allowClickDrained")
    public PhantomFluidWidget setAllowClickDrained(boolean v) {
        // you cant modify it
        return this;
    }

    protected void setLastPhantomStack(FluidStack fluid) {
        if (fluid != null) {
            this.lastPhantomStack = fluid.copy();
            this.lastPhantomStack.setAmount(1);
        } else {
            this.lastPhantomStack = null;
        }
    }

    public static FluidStack drainFrom(Object ingredient) {
         if (ingredient instanceof Ingredient ing) {
            var items = ing.getItems();
            if (items.length > 0) {
                ingredient = items[0];
            }
        }
        if (ingredient instanceof ItemStack itemStack) {
            var handler = FluidTransferHelper.getFluidTransfer(new ItemStackTransfer(itemStack), 0);
            if (handler != null) {
                return handler.drain(Long.MAX_VALUE, true);
            }
        }
        return FluidStack.empty();
    }

    @Override
    @Environment(EnvType.CLIENT)
    public List<Target> getPhantomTargets(Object ingredient) {
        if (LDLib.isReiLoaded() && ingredient instanceof dev.architectury.fluid.FluidStack fluidStack) {
            ingredient = FluidStack.create(fluidStack.getFluid(), fluidStack.getAmount(), fluidStack.getTag());
        }
        if (LDLib.isEmiLoaded() && ingredient instanceof EmiStack fluidEmiStack) {
            Fluid fluid = fluidEmiStack.getKeyOfType(Fluid.class);
            if (fluid == null) {
                Item item = fluidEmiStack.getKeyOfType(Item.class);
                ingredient = item == null ? null : new ItemStack(item, (int)fluidEmiStack.getAmount());
                if (ingredient instanceof ItemStack itemStack) {
                    itemStack.setTag(fluidEmiStack.getNbt());
                }
            } else {
                ingredient = FluidStack.create(fluid, fluidEmiStack.getAmount() == 0L ? 1000L : fluidEmiStack.getAmount(), fluidEmiStack.getNbt());
            }
        }
        if (LDLib.isJeiLoaded() && ingredient.getClass().getName().equals("mezz.jei.library.ingredients.TypedIngredient")) {
            try {
                ingredient = ingredient.getClass().getMethod("getIngredient").invoke(ingredient);
                Class<?> clazz = ingredient.getClass();
                Method fluidMethod =  clazz.getMethod("getFluid");
                Method amountMethod = clazz.getMethod("getAmount");
                Method tagMethod = clazz.getMethod("getTag");
                ingredient = FluidStack.create((Fluid) fluidMethod.invoke(ingredient),
                        (int) amountMethod.invoke(ingredient), (CompoundTag) tagMethod.invoke(ingredient));
            } catch (Exception ignored) { }
        }
        if (!(ingredient instanceof FluidStack) && drainFrom(ingredient) == FluidStack.empty()) {
            return Collections.emptyList();
        }

        Rect2i rectangle = toRectangleBox();
        return Lists.newArrayList(new Target() {
            @Nonnull
            @Override
            public Rect2i getArea() {
                return rectangle;
            }

            @Override
            public void accept(@Nonnull Object ingredient) {
                FluidStack ingredientStack;
                if (LDLib.isReiLoaded() && ingredient instanceof dev.architectury.fluid.FluidStack fluidStack) {
                    ingredient = FluidStack.create(fluidStack.getFluid(), fluidStack.getAmount(), fluidStack.getTag());
                }
                if (LDLib.isEmiLoaded() && ingredient instanceof EmiStack fluidEmiStack) {
                    var fluid = fluidEmiStack.getKeyOfType(Fluid.class);
                    if (fluid == null) {
                        Item item = fluidEmiStack.getKeyOfType(Item.class);
                        ingredient = item == null ? null : new ItemStack(item, (int)fluidEmiStack.getAmount());
                        if (ingredient instanceof ItemStack itemStack) {
                            itemStack.setTag(fluidEmiStack.getNbt());
                        }
                    } else {
                        ingredient = FluidStack.create(fluid, fluidEmiStack.getAmount() == 0L ? 1000L : fluidEmiStack.getAmount(), fluidEmiStack.getNbt());
                    }
                }
                if (LDLib.isJeiLoaded() && ingredient.getClass().getName().equals("net.minecraftforge.fluids.FluidStack")) {
                    Class<?> clazz = ingredient.getClass();
                    try {
                        Method fluidMethod =  clazz.getMethod("getFluid");
                        Method amountMethod = clazz.getMethod("getAmount");
                        Method tagMethod = clazz.getMethod("getTag");
                        ingredient = FluidStack.create((Fluid) fluidMethod.invoke(ingredient),
                                (int) amountMethod.invoke(ingredient), (CompoundTag) tagMethod.invoke(ingredient));
                    } catch (Exception ignored) { }
                }
                if (ingredient instanceof FluidStack fluidStack)
                    ingredientStack = fluidStack;
                else
                    ingredientStack = drainFrom(ingredient);

                if (ingredientStack != FluidStack.empty()) {
                    writeClientAction(2, ingredientStack::writeToBuf);
                }

                if (isClientSideWidget) {
                    if (phantomFluidSetter != null) {
                        phantomFluidSetter.accept(ingredientStack);
                    }
                }
            }
        });
    }

    @Override
    public void handleClientAction(int id, FriendlyByteBuf buffer) {
        if (id == 1) {
            handlePhantomClick();
        } else if (id == 2) {
            if (phantomFluidSetter != null) {
                phantomFluidSetter.accept(FluidStack.readFromBuf(buffer));
            }
        } else if (id == 4) {
            phantomFluidSetter.accept(FluidStack.empty());
        } else if (id == 5) {
            phantomFluidSetter.accept(FluidStack.readFromBuf(buffer));
        }
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        FluidStack stack = phantomFluidGetter.get();
        if (stack == null || stack.isEmpty()) {
            if (lastPhantomStack != null) {
                setLastPhantomStack(null);
                writeUpdateInfo(4, buf -> {});
            }
        } else if (lastPhantomStack == null || !stack.isFluidEqual(lastPhantomStack)) {
            setLastPhantomStack(stack);
            writeUpdateInfo(5, stack::writeToBuf);
        }
    }

    @Override
    @Environment(EnvType.CLIENT)
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isMouseOverElement(mouseX, mouseY)) {
            if (isClientSideWidget) {
                handlePhantomClick();
            } else {
                writeClientAction(1, buffer -> {});
            }
            return true;
        }
        return false;
    }

    private void handlePhantomClick() {
        ItemStack itemStack = gui.getModularUIContainer().getCarried().copy();
        if (!itemStack.isEmpty()) {
            itemStack.setCount(1);
            var handler = FluidTransferHelper.getFluidTransfer(gui.entityPlayer, gui.getModularUIContainer());
            if (handler != null) {
                FluidStack resultFluid = handler.drain(Integer.MAX_VALUE, true);
                if (phantomFluidSetter != null) {
                    phantomFluidSetter.accept(resultFluid);
                }
            }
        } else {
            if (phantomFluidSetter != null) {
                phantomFluidSetter.accept(FluidStack.empty());
            }
        }
    }

    @Override
    public void drawInBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if (this.lastFluidInTank != null) {
            super.drawInBackground(graphics, mouseX, mouseY, partialTicks);
            return;
        }
        Position pos = getPosition();
        Size size = getSize();
        FluidStack stack = phantomFluidGetter.get();
        if (stack != null && !stack.isEmpty()) {
            RenderSystem.disableBlend();

            double progress = stack.getAmount() * 1.0 / Math.max(Math.max(stack.getAmount(), lastTankCapacity), 1);
            float drawnU = (float) fillDirection.getDrawnU(progress);
            float drawnV = (float) fillDirection.getDrawnV(progress);
            float drawnWidth = (float) fillDirection.getDrawnWidth(progress);
            float drawnHeight = (float) fillDirection.getDrawnHeight(progress);
            int width = size.width - 2;
            int height = size.height - 2;
            int x = pos.x + 1;
            int y = pos.y + 1;
            DrawerHelper.drawFluidForGui(graphics, stack, stack.getAmount(), (int) (x + drawnU * width), (int) (y + drawnV * height), ((int) (width * drawnWidth)), ((int) (height * drawnHeight)));
            if (showAmount) {
                graphics.pose().pushPose();
                graphics.pose().scale(0.5F, 0.5F, 1);
                String s = TextFormattingUtil.formatLongToCompactStringBuckets(stack.getAmount(), 3) + "B";
                Font fontRenderer = Minecraft.getInstance().font;
                graphics.drawString(fontRenderer, s, (int) ((pos.x + (size.width / 3f)) * 2 - fontRenderer.width(s) + 21), (int) ((pos.y + (size.height / 3f) + 6) * 2), 0xFFFFFF, true);
                graphics.pose().popPose();
            }

            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1, 1, 1, 1);
        }

    }
}
