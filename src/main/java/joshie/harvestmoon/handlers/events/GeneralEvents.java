package joshie.harvestmoon.handlers.events;

import static joshie.harvestmoon.HarvestMoon.handler;
import static joshie.harvestmoon.helpers.CalendarHelper.getSeason;
import joshie.harvestmoon.calendar.Season;
import joshie.harvestmoon.handlers.ServerHandler;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiSelectWorld;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.event.terraingen.BiomeEvent.GetFoliageColor;
import net.minecraftforge.event.world.WorldEvent;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class GeneralEvents {    
    @SubscribeEvent
    public void onLoad(WorldEvent.Load event) {
        if (event.world.provider.dimensionId == 0) {
            if(FMLCommonHandler.instance().getEffectiveSide() == Side.SERVER) {
                handler.setServer(new ServerHandler(event.world));
            }
        }
    }
    
    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void onOpenGUI(GuiOpenEvent event) {
        if(event.gui instanceof GuiSelectWorld || event.gui instanceof GuiMultiplayer) {            
            handler.resetClient();
        }
    }
    
    @SubscribeEvent
    public void getFoliageColor(GetFoliageColor event) {
        if(getSeason() == Season.AUTUMN) {
            event.newColor = 0xFF9900;
        }
    }
}
