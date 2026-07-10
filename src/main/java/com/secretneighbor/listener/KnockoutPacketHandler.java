package com.secretneighbor.listener;

import com.secretneighbor.SecretNeighborPlugin;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class KnockoutPacketHandler {

    private final SecretNeighborPlugin plugin;
    private final Set<Integer> knockedEntityIds = ConcurrentHashMap.newKeySet();

    public KnockoutPacketHandler(SecretNeighborPlugin plugin) {
        this.plugin = plugin;
    }

    public void register() {
        ProtocolLibrary.getProtocolManager().addPacketListener(
            new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Server.ENTITY_METADATA) {
                @Override
                public void onPacketSending(PacketEvent event) {
                    PacketContainer packet = event.getPacket();
                    int entityId = packet.getIntegers().read(0);

                    if (knockedEntityIds.contains(entityId)) {
                        List<WrappedDataValue> dataValues = packet.getDataValueCollectionModifier().read(0);
                        if (dataValues != null) {
                            boolean found = false;
                            for (WrappedDataValue val : dataValues) {
                                if (val.getIndex() == 6) {
                                    val.setValue(EnumWrappers.EntityPose.SWIMMING.toNms());
                                    found = true;
                                    break;
                                }
                            }
                            if (!found) {
                                List<WrappedDataValue> newList = new ArrayList<>(dataValues);
                                newList.add(new WrappedDataValue(
                                    6,
                                    WrappedDataWatcher.Registry.get(EnumWrappers.getEntityPoseClass()),
                                    EnumWrappers.EntityPose.SWIMMING.toNms()
                                ));
                                packet.getDataValueCollectionModifier().write(0, newList);
                            }
                        }
                    }
                }
            }
        );
    }

    public void addKnockedPlayer(Player player) {
        knockedEntityIds.add(player.getEntityId());
        sendPoseUpdate(player, EnumWrappers.EntityPose.SWIMMING);
    }

    public void removeKnockedPlayer(Player player) {
        knockedEntityIds.remove(player.getEntityId());
        sendPoseUpdate(player, EnumWrappers.EntityPose.STANDING);
    }

    public void clear() {
        knockedEntityIds.clear();
    }

    private void sendPoseUpdate(Player player, EnumWrappers.EntityPose pose) {
        PacketContainer packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.ENTITY_METADATA);
        packet.getIntegers().write(0, player.getEntityId());

        List<WrappedDataValue> dataValues = new ArrayList<>();
        dataValues.add(new WrappedDataValue(
            6,
            WrappedDataWatcher.Registry.get(EnumWrappers.getEntityPoseClass()),
            pose.toNms()
        ));
        packet.getDataValueCollectionModifier().write(0, dataValues);

        for (Player p : Bukkit.getOnlinePlayers()) {
            try {
                ProtocolLibrary.getProtocolManager().sendServerPacket(p, packet);
            } catch (Exception ignored) {}
        }
    }
}
