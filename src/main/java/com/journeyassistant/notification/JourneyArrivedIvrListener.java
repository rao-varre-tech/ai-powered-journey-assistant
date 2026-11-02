package com.journeyassistant.notification;

import com.journeyassistant.journey.JourneyArrivedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class JourneyArrivedIvrListener {
    private final IvrCallService calls;

    public JourneyArrivedIvrListener(IvrCallService calls) { this.calls = calls; }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onJourneyArrived(JourneyArrivedEvent event) { calls.placeCall(event.journeyId()); }
}
