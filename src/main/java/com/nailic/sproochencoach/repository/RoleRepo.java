package com.nailic.sproochencoach.repository;

import com.nailic.sproochencoach.model.AppRole;
import java.util.List;
import java.util.Set;
import org.springframework.data.repository.CrudRepository;

public interface RoleRepo extends CrudRepository<AppRole, Integer> {

  AppRole findByName(String name);

  List<AppRole> findAllByName(Set<String> roles);
}
