package com.journeyassistant.notification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationAttemptRepository extends JpaRepository<NotificationAttempt, UUID> {

    List<NotificationAttempt> findAllByJourney_IdOrderByAttemptNumberAsc(UUID journeyId);

    Optional<NotificationAttempt> findByProviderCallId(String providerCallId);
}
