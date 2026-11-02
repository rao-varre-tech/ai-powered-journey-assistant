package com.journeyassistant.config;

import com.journeyassistant.journey.JourneyStateMachine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JourneyStateMachineConfiguration {

    @Bean
    JourneyStateMachine journeyStateMachine() {
        return new JourneyStateMachine();
    }
}
