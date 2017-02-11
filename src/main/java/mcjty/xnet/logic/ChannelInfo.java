package mcjty.xnet.logic;

import mcjty.xnet.XNet;
import mcjty.xnet.api.channels.IChannelSettings;
import mcjty.xnet.api.channels.IChannelType;
import mcjty.xnet.multiblock.ConsumerId;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.util.Constants;

import java.util.HashMap;
import java.util.Map;

public class ChannelInfo {

    public static final int MAX_CHANNELS = 8;

    private final IChannelType type;
    private final IChannelSettings channelSettings;

    private final Map<SidedConsumer, ConnectorInfo> connectors = new HashMap<>();

    public ChannelInfo(IChannelType type) {
        this.type = type;
        channelSettings = type.createChannel();
    }

    public IChannelType getType() {
        return type;
    }

    public IChannelSettings getChannelSettings() {
        return channelSettings;
    }

    public Map<SidedConsumer, ConnectorInfo> getConnectors() {
        return connectors;
    }

    public void writeToNBT(NBTTagCompound tag) {
        channelSettings.writeToNBT(tag);
        NBTTagList conlist = new NBTTagList();
        for (Map.Entry<SidedConsumer, ConnectorInfo> entry : connectors.entrySet()) {
            NBTTagCompound tc = new NBTTagCompound();
            ConnectorInfo connectorInfo = entry.getValue();
            connectorInfo.writeToNBT(tc);
            tc.setInteger("consumerId", entry.getKey().getConsumerId().getId());
            tc.setInteger("side", entry.getKey().getSide().ordinal());
            tc.setString("type", connectorInfo.getType().getID());
            conlist.appendTag(tc);
        }
        tag.setTag("connectors", conlist);
    }

    public void readFromNBT(NBTTagCompound tag) {
        channelSettings.readFromNBT(tag);
        NBTTagList conlist = tag.getTagList("connectors", Constants.NBT.TAG_COMPOUND);
        for (int i = 0 ; i < conlist.tagCount() ; i++) {
            NBTTagCompound tc = conlist.getCompoundTagAt(i);
            String id = tc.getString("type");
            IChannelType type = XNet.xNetApi.findType(id);
            if (type == null) {
                XNet.logger.warn("Unsupported type " + id + "!");
                continue;
            }
            if (!getType().equals(type)) {
                XNet.logger.warn("Trying to load a connector with non-matching type " + type + "!");
                continue;
            }
            ConsumerId consumerId = new ConsumerId(tc.getInteger("consumerId"));
            EnumFacing side = EnumFacing.values()[tc.getInteger("side")];
            SidedConsumer key = new SidedConsumer(consumerId, side);
            ConnectorInfo connectorInfo = new ConnectorInfo(type, key);
            connectorInfo.readFromNBT(tc);
            connectors.put(key, connectorInfo);
        }
    }
}