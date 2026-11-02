package com.journeyassistant.journey;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.UUID;

public interface JourneyRepository extends JpaRepository<Journey, UUID> {

    List<Journey> findAllByState(JourneyState state);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select journey from Journey journey where journey.id = :id")
    java.util.Optional<Journey> findByIdForUpdate(@Param("id") UUID id);
}
