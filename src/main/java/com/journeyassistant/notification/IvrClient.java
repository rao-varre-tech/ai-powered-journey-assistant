package com.journeyassistant.notification;

import java.util.UUID;

public interface IvrClient {
    String placeCall(UUID journeyId, String toPhone);
}
