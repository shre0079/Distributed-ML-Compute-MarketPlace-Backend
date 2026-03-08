package com.dcm.backend.demo.repository;

import com.dcm.backend.demo.dto.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, String> {}
