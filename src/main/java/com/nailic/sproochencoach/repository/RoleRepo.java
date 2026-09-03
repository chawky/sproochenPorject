package com.nailic.sproochencoach.repository;

import com.nailic.sproochencoach.model.AppRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;
@Repository
public interface RoleRepo extends JpaRepository<AppRole, Integer> {

  AppRole findByName(String name);

  boolean existsByName(String name);

  List<AppRole> findByNameIn(Set<String> roles);
}
