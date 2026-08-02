package com.nailic.sproochencoach.repository;

import com.nailic.sproochencoach.model.AppRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Set;

public interface RoleRepo extends JpaRepository<AppRole, Integer> {

  AppRole findByName(String name);

  List<AppRole> findByNameIn(Set<String> roles);
}
