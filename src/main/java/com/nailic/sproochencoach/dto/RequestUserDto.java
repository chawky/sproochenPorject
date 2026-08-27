package com.nailic.sproochencoach.dto;

import jakarta.validation.constraints.Size;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class RequestUserDto {
  private String username;
  private String password;
  private String email;
  @Size(max = 80)
  private String firstName;
  @Size(max = 80)
  private String lastName;
  @Size(max = 120)
  private String street;
  @Size(max = 20)
  private String streetNumber;
  @Size(max = 20)
  private String postalCode;
  @Size(max = 80)
  private String city;
  @Size(max = 255)
  private String addressInfo;
  private Set<String> roles = new HashSet<>();
}
