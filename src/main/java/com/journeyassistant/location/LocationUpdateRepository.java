package com.journeyassistant.location;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LocationUpdateRepository extends JpaRepository<LocationUpdate, UUID> {

    List<LocationUpdate> findAllByJourney_IdOrderByTimestampDesc(UUID journeyId);
}
