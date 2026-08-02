package com.nailic.sproochencoach.model;

import jakarta.persistence.Column;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class BaseModel {
  @Column
  private LocalDateTime createdAt;
  @Column
  private LocalDateTime updatedAt;
  @Column
  private Long createdBy;
  @Column
  private Long updatedBy;

}
