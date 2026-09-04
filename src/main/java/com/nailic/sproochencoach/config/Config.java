package com.nailic.sproochencoach.config;

import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class Config {
  @Bean
  public static ModelMapper modelMapper() {
    return new ModelMapper();
  }

  @Bean
  public Clock clock() {
    return Clock.systemDefaultZone();
  }
}
