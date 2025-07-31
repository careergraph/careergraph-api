package com.hcmute.careergraph.repository.graph;

import com.hcmute.careergraph.entities.graph.Genre;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GenreNodeRepository extends Neo4jRepository<Genre, Long> {

    @Query("MATCH (g:Genre) WHERE g.name = $name RETURN id(g)")
    Long findIdByName(@Param("name") String name);

    @Query("MATCH (g:Genre) WHERE id(g) = $genreId " +
            "MATCH (m:Movie) WHERE id(m) = $movieId " +
            "MERGE (g)-[:INCLUDE]->(m) " +
            "RETURN g")
    void linkGenreToMovie(@Param("genreId") Long genreId,
                          @Param("movieId") Long movieId);
}
