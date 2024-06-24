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
            select t from Token t inner join User u on t.users.userid = u.userId
            where u.userId = :userId and (t.expired = false or t.revoked = false)
        """
    )
    List<Token> findValidTokenByCustomer(Long userId);
    @Query("""
            select t from Token t inner join Admin a on t.admin.adminId = a.adminId
            where a.adminId = :adminId and (t.expired = false or t.revoked = false)
        """
    )
    List<Token> findValidTokenByAdmin(Long adminId);
    Optional<Token> findByToken(String token);
}