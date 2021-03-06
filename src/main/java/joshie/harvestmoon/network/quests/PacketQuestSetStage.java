package joshie.harvestmoon.network.quests;

import static cpw.mods.fml.common.network.ByteBufUtils.readUTF8String;
import static cpw.mods.fml.common.network.ByteBufUtils.writeUTF8String;
import static joshie.harvestmoon.HarvestMoon.handler;
import io.netty.buffer.ByteBuf;
import joshie.harvestmoon.init.HMQuests;
import joshie.harvestmoon.quests.Quest;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

public class PacketQuestSetStage implements IMessage, IMessageHandler<PacketQuestSetStage, IMessage> {
    private Quest quest;
    private boolean isSenderClient;
    private int stage;

    public PacketQuestSetStage() {}

    public PacketQuestSetStage(Quest quest, boolean isSenderClient, int stage) {
        this.quest = quest;
        this.isSenderClient = isSenderClient;
        this.stage = stage;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(isSenderClient);
        buf.writeShort(stage);
        writeUTF8String(buf, quest.getName());
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        isSenderClient = buf.readBoolean();
        stage = buf.readShort();
        quest = HMQuests.get(readUTF8String(buf));
    }

    @Override
    public IMessage onMessage(PacketQuestSetStage message, MessageContext ctx) {
        if (message.isSenderClient) {
            handler.getServer().getPlayerData(ctx.getServerHandler().playerEntity).getQuests().setStage(quest, message.stage);
        } else {
            handler.getClient().getPlayerData().getQuests().setStage(quest, message.stage);
        }

        return null;
    }
}