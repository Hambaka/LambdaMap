/*
 * Copyright (c) 2020 LambdAurora <aurora42lambda@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package dev.lambdaurora.lambdamap.gui;

import dev.lambdaurora.lambdamap.LambdaMap;
import dev.lambdaurora.lambdamap.map.marker.MarkerManager;
import me.lambdaurora.spruceui.Position;
import me.lambdaurora.spruceui.background.EmptyBackground;
import me.lambdaurora.spruceui.screen.SpruceScreen;
import me.lambdaurora.spruceui.widget.container.SpruceContainerWidget;
import me.lambdaurora.spruceui.widget.container.SpruceOptionListWidget;
import me.lambdaurora.spruceui.widget.container.tabbed.SpruceTabbedWidget;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

public class WorldMapScreen extends SpruceScreen {
    private final LambdaMap mod = LambdaMap.get();
    private final WorldMapRenderer renderer = this.mod.getRenderer();

    public WorldMapScreen() {
        super(new LiteralText("World Map"));
    }

    @Override
    public void removed() {
        super.removed();
    }

    @Override
    protected void init() {
        super.init();

        SpruceTabbedWidget tabs = this.addChild(new SpruceTabbedWidget(Position.origin(), this.width, this.height, new LiteralText("LambdaMap")));
        tabs.getList().setBackground(RandomPrideFlagBackground.random());
        tabs.addTabEntry(new TranslatableText("lambdamap.tabs.world_map"), new TranslatableText("lambdamap.tabs.world_map.description").formatted(Formatting.GRAY),
                (width, height) -> new WorldMapWidget(Position.origin(), width, height));
        tabs.addTabEntry(new TranslatableText("lambdamap.tabs.markers"), new TranslatableText("lambdamap.tabs.markers.description").formatted(Formatting.GRAY),
                (width, height) -> {
                    SpruceContainerWidget containerWidget = new SpruceContainerWidget(Position.origin(), width, height);
                    MarkerManager markers = this.mod.getMap().getMarkerManager();

                    int newMarkerFormHeight = width < 480 ? 80 : 40;

                    MarkerListWidget list = new MarkerListWidget(Position.origin(), width, height - newMarkerFormHeight, markers);
                    containerWidget.addChild(list);
                    containerWidget.addChild(new NewMarkerFormWidget(Position.of(containerWidget, 0, list.getHeight()), width, newMarkerFormHeight, markers, list));
                    return containerWidget;
                });
        tabs.addTabEntry(new TranslatableText("lambdamap.tabs.config"), new TranslatableText("lambdamap.tabs.config.description").formatted(Formatting.GRAY),
                this::buildConfigTab);
    }

    private SpruceOptionListWidget buildConfigTab(int width, int height) {
        SpruceOptionListWidget list = new SpruceOptionListWidget(Position.origin(), width, height);
        list.setBackground(EmptyBackground.EMPTY_BACKGROUND);

        list.addSingleOptionEntry(this.mod.getConfig().getRenderBiomeColorsOption());
        return list;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }
}
