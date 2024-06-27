package org.remita.autologmonitor.repository;

import org.remita.autologmonitor.entity.OTP;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OTPRepository extends JpaRepository<Long, OTP> {
    @Query("""
    select t from OTP t where (t.user.id = :userId or t.admin.id = :userId) 
    """)
    Optional<OTP> findByUserEntity(long userId);
}
