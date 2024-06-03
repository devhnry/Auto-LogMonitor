package org.remita.autologmonitor.repository;

import org.remita.autologmonitor.entity.LogError;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LogErrorRepository extends JpaRepository<LogError, Long> {
}
