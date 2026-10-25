package com.lopez.l2j.game.event;

import com.lopez.l2j.game.event.pvp.CtfEventService;
import com.lopez.l2j.game.event.pvp.DmEventService;
import com.lopez.l2j.game.event.pvp.LastManStandingService;
import com.lopez.l2j.game.event.pvp.TvtEventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Coordenador Central de Eventos Globais (EventEngineManager) - #491.
 * Garante exclusao mutua (mutex) entre TvT, CTF, DM e LMS para impedir que dois
 * eventos globais de arena abram inscricoes simultaneamente ou colidam.
 */
@Service
public class EventEngineManager {

    private static final Logger log = LoggerFactory.getLogger(EventEngineManager.class);
    private static volatile EventEngineManager instance;

    private final TvtEventService tvtService;
    private final CtfEventService ctfService;
    private final DmEventService dmService;
    private final LastManStandingService lmsService;
    private final AtomicReference<String> activeEventLock = new AtomicReference<>(null);

    public EventEngineManager() {
        this(null, null, null, null);
    }

    @Autowired
    public EventEngineManager(
            @Autowired(required = false) TvtEventService tvtService,
            @Autowired(required = false) CtfEventService ctfService,
            @Autowired(required = false) DmEventService dmService,
            @Autowired(required = false) LastManStandingService lmsService) {
        this.tvtService = tvtService;
        this.ctfService = ctfService;
        this.dmService = dmService;
        this.lmsService = lmsService;
        instance = this;
    }

    public static EventEngineManager getInstance() {
        return instance;
    }

    public synchronized boolean tryAcquireEventLock(String eventName) {
        if (eventName == null || eventName.isBlank()) {
            return false;
        }
        if (isAnyGlobalEventActive() && !eventName.equalsIgnoreCase(activeEventLock.get())) {
            log.warn("EventEngineManager: Bloqueada abertura de {} pois outro evento ja esta em curso ({}).",
                    eventName, getActiveEventName());
            return false;
        }
        activeEventLock.set(eventName.toUpperCase());
        return true;
    }

    public synchronized void releaseEventLock(String eventName) {
        if (eventName != null && eventName.equalsIgnoreCase(activeEventLock.get())) {
            activeEventLock.set(null);
        }
    }

    public boolean isAnyGlobalEventActive() {
        if (tvtService != null && tvtService.getState() != TvtEventService.EventState.INACTIVE) {
            return true;
        }
        if (ctfService != null && ctfService.getState() != CtfEventService.EventState.INACTIVE) {
            return true;
        }
        if (dmService != null && dmService.getState() != DmEventService.EventState.INACTIVE) {
            return true;
        }
        if (lmsService != null && lmsService.getState() != LastManStandingService.EventState.INACTIVE) {
            return true;
        }
        return activeEventLock.get() != null;
    }

    public String getActiveEventName() {
        if (tvtService != null && tvtService.getState() != TvtEventService.EventState.INACTIVE) {
            return "TVT";
        }
        if (ctfService != null && ctfService.getState() != CtfEventService.EventState.INACTIVE) {
            return "CTF";
        }
        if (dmService != null && dmService.getState() != DmEventService.EventState.INACTIVE) {
            return "DM";
        }
        if (lmsService != null && lmsService.getState() != LastManStandingService.EventState.INACTIVE) {
            return "LMS";
        }
        return activeEventLock.get();
    }

    public boolean isPlayerRegisteredInAnyEvent(int playerId) {
        if (tvtService != null && tvtService.isParticipant(playerId)) return true;
        if (ctfService != null && ctfService.isParticipant(playerId)) return true;
        if (dmService != null && dmService.isParticipant(playerId)) return true;
        return false;
    }
}
