package com.nailic.sproochencoach.dto;

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
public class AppUserDto {
  private Integer id;
  private String username;
  private String password;
  private String email;
  private Set<String> roles = new HashSet<>();
}
