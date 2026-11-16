package net.soundvibe.hasio.danfoss.protocol;

import net.soundvibe.hasio.danfoss.data.IconMaster;
import net.soundvibe.hasio.danfoss.data.IconRoom;
import net.soundvibe.hasio.danfoss.protocol.config.Dominion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static net.soundvibe.hasio.danfoss.protocol.config.DanfossBindingConstants.ICON_MAX_ROOMS;
import static net.soundvibe.hasio.danfoss.protocol.config.Icon.MsgClass.*;
import static net.soundvibe.hasio.danfoss.protocol.config.Icon.MsgCode.*;


public class IconMasterHandler implements PacketHandler {
    private final IconRoomHandler[] rooms = new IconRoomHandler[ICON_MAX_ROOMS];
    private final Map<String, IconRoomHandler> roomsByName = new ConcurrentHashMap<>(ICON_MAX_ROOMS * 2);

    private static final Logger logger = LoggerFactory.getLogger(IconMasterHandler.class);
    private final ChangeJournal changeJournal = new ChangeJournal();

    private static class IconMasterControllerState {
        private String houseName;
        private double vacationSetPoint;
        private double pauseSetPoint;

        private String hardwareRevision;
        private String softwareRevision;
        private String serialNumber;
        private int softwareBuildRevision;
        private int connectionCount;
        private Instant productionDate;

        public IconMaster toIconMaster() {
            return new IconMaster(houseName, vacationSetPoint, pauseSetPoint, hardwareRevision, softwareRevision, serialNumber,
                    softwareBuildRevision, connectionCount, productionDate);
        }
    }

    private final IconMasterControllerState state = new IconMasterControllerState();

    private final ReadWriteLock lock = new ReentrantReadWriteLock(true);
    private final SDGPeerConnector connector;

    public IconMasterHandler(byte[] privateKey, ScheduledExecutorService executorService) {
        this.connector = new SDGPeerConnector(this, privateKey, executorService);
        for (int i = 0; i < ICON_MAX_ROOMS; i++) {
            this.rooms[i] = new IconRoomHandler(this.connector, i);
        }
    }

    public void scanRooms(String housePeerId) {
        connector.initialize(housePeerId);
        // Request names for all the rooms
        for (int msgClass = ROOM_FIRST; msgClass <= ROOM_LAST; msgClass++) {
            connector.SendPacket(new Dominion.Packet(msgClass, ROOMNAME));
        }
    }

    public List<IconRoom> listRooms() {
        return this.roomsByName.values().stream()
                .map(IconRoomHandler::toIconRoom)
                .toList();
    }

    public Optional<IconRoomHandler> roomHandlerByNumber(int number) {
        return this.roomsByName.values().stream()
                .filter(room -> room.roomNumber == number)
                .findAny();
    }

    public IconMaster iconMaster() {
        lock.readLock().lock();
        try {
            return state.toIconMaster();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void handlePacket(Dominion.Packet pkt) {
        int msgClass = pkt.getMsgClass();

        // Always on (info level): what the controller changes by itself, or when the Danfoss app changes something.
        // Only changes of rarely-changing parameters are logged, see ChangeJournal.
        changeJournal.record(msgClass, pkt.getMsgCode(), pkt.payloadHex())
                .ifPresent(line -> logger.info("house={} {}", state.houseName, line));

        // Debug only: every packet from the controller, except the clock (SYSTEM) and the Wi-Fi scan list (WIFI),
        // which arrive constantly and aren't interesting.
        // The payload of MDG (connection management) and SOFTWAREUPDATE packets can contain key material,
        // so only class and code are logged for those.
        if (logger.isDebugEnabled() && msgClass != SYSTEM && msgClass != WIFI) {
            var payload = (msgClass == MDG || msgClass == SOFTWAREUPDATE) ? "<hidden>" : pkt.payloadHex();
            logger.debug("packet house={} class={} code={} payload={}",
                    state.houseName, msgClass, pkt.getMsgCode(), payload);
        }

        if (msgClass >= ROOM_FIRST && msgClass <= ROOM_LAST) {
            var room = rooms[msgClass - ROOM_FIRST];
            room.handlePacket(pkt);
            var name = room.nameOrEmpty();
            if (!name.isEmpty()) {
                this.roomsByName.putIfAbsent(name, room);
            }
        } else {
            switch (pkt.getMsgCode()) {
                case HOUSE_NAME: {
                    lock.writeLock().lock();
                    state.houseName = pkt.getString();
                    lock.writeLock().unlock();
                    break;
                }
                case VACATION_SETPOINT:
                    lock.writeLock().lock();
                    state.vacationSetPoint = pkt.getDecimal();
                    lock.writeLock().unlock();
                    break;
                case PAUSE_SETPOINT:
                    lock.writeLock().lock();
                    state.pauseSetPoint = pkt.getDecimal();
                    lock.writeLock().unlock();
                    break;
                case GLOBAL_HARDWAREREVISION:
                    lock.writeLock().lock();
                    state.hardwareRevision = pkt.getVersion().toString();
                    lock.writeLock().unlock();
                    break;
                case GLOBAL_SOFTWAREREVISION:
                    lock.writeLock().lock();
                    state.softwareRevision = pkt.getVersion().toString();
                    lock.writeLock().unlock();
                    break;
                case GLOBAL_SOFTWAREBUILDREVISION:
                    lock.writeLock().lock();
                    state.softwareBuildRevision = Short.toUnsignedInt(pkt.getShort());
                    lock.writeLock().unlock();
                    break;
                case GLOBAL_SERIALNUMBER:
                    lock.writeLock().lock();
                    state.serialNumber = String.valueOf(pkt.getInt());
                    lock.writeLock().unlock();
                    break;
                case GLOBAL_PRODUCTIONDATE:
                    lock.writeLock().lock();
                    state.productionDate = pkt.getDate(0).toInstant();
                    lock.writeLock().unlock();
                    break;
                case MDG_CONNECTION_COUNT:
                    lock.writeLock().lock();
                    state.connectionCount = pkt.getByte();
                    lock.writeLock().unlock();
                    break;
            }
        }
    }

    @Override
    public void ping() {
        this.connector.SendPacket(new Dominion.Packet(ALL_ROOMS, VACATION_SETPOINT));
    }
}
