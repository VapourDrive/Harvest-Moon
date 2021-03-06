package joshie.harvestmoon.network;

import static joshie.harvestmoon.HarvestMoon.handler;
import io.netty.buffer.ByteBuf;
import joshie.harvestmoon.crops.CropData;
import joshie.harvestmoon.crops.CropLocation;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

public class PacketSyncCrop implements IMessage, IMessageHandler<PacketSyncCrop, IMessage> {
    private boolean isRemoval;
    private CropLocation location;
    private CropData data;

    public PacketSyncCrop() {}

    public PacketSyncCrop(CropLocation location, CropData data) {
        this.isRemoval = false;
        this.location = location;
        this.data = data;
    }

    public PacketSyncCrop(CropLocation location) {
        this.isRemoval = true;
        this.location = location;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(isRemoval);
        location.toBytes(buf);
        if (!isRemoval) {
            data.toBytes(buf);
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        isRemoval = buf.readBoolean();
        location = new CropLocation();
        location.fromBytes(buf);
        if (!isRemoval) {
            data = new CropData();
            data.fromBytes(buf);
        }
    }

    @Override
    public IMessage onMessage(PacketSyncCrop msg, MessageContext ctx) {
        handler.getClient().getCropTracker().sync(msg.isRemoval, msg.location, msg.data);
        joshie.lib.helpers.ClientHelper.refresh(msg.location.dimension, msg.location.x, msg.location.y, msg.location.z);

        return null;
    }
}