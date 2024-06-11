package org.remita.autologmonitor.repository;

import org.remita.autologmonitor.entity.LogError;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LogErrorRepository extends JpaRepository<LogError, Long> {
}
