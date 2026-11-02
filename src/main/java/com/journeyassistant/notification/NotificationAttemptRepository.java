package com.journeyassistant.notification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface NotificationAttemptRepository extends JpaRepository<NotificationAttempt, UUID> {

    List<NotificationAttempt> findAllByJourney_IdOrderByAttemptNumberAsc(UUID journeyId);

    Optional<NotificationAttempt> findByProviderCallId(String providerCallId);

    long countByJourney_Id(UUID journeyId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select attempt from NotificationAttempt attempt where attempt.providerCallId = :callSid")
    Optional<NotificationAttempt> findByProviderCallIdForUpdate(@Param("callSid") String callSid);
}
