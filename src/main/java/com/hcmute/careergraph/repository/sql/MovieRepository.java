package com.hcmute.careergraph.repository.sql;


import com.hcmute.careergraph.entities.mysql.Movie;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovieRepository extends JpaRepository<Movie, String> {

    boolean existsByTitle(String title);

    void deleteByTitle(String title);
}
