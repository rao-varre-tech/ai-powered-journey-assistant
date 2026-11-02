package com.journeyassistant.journey;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JourneyRepository extends JpaRepository<Journey, UUID> {

    List<Journey> findAllByState(JourneyState state);
}
