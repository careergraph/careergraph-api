package com.hcmute.careergraph.repository.sql;


import com.hcmute.careergraph.entities.mysql.Genre;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GenreRepository extends JpaRepository<Genre, String> {

    boolean existsBySlug(String slug);
}
