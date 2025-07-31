package com.hcmute.careergraph.repository.sql;


import com.hcmute.careergraph.entities.mysql.Token;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TokenRepository extends JpaRepository<Token, String> {

}
