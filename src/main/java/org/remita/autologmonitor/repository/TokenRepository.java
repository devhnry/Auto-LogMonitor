package org.remita.autologmonitor.repository;

import org.remita.autologmonitor.entity.Token;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TokenRepository extends JpaRepository<Token, Long> {
    @Query("""
    select t from Token t where (t.users.id = :userId or t.admin.id = :userId) 
    and (t.expired = false or t.revoked = false)
    """)
    List<Token> findValidTokenByUserEntity(long userId);
    Optional<Token> findByToken(String token);
}
