package com.journeyassistant.notification;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.UUID;

public interface JourneyMessageRepository extends JpaRepository<JourneyMessage, UUID> {
    @Query("select message from JourneyMessage message where message.providerMessageId = :messageSid")
    Optional<JourneyMessage> findByProviderMessageId(@Param("messageSid") String messageSid);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select message from JourneyMessage message where message.journey.id = :journeyId and message.channel = :channel")
    Optional<JourneyMessage> findByJourneyAndChannelForUpdate(@Param("journeyId") UUID journeyId,
                                                              @Param("channel") MessageChannelType channel);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select message from JourneyMessage message where message.providerMessageId = :messageSid")
    Optional<JourneyMessage> findByProviderMessageIdForUpdate(@Param("messageSid") String messageSid);
}
