package com.hcmute.careergraph.repository.graph;

import com.hcmute.careergraph.entities.graph.Movie;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MovieNodeRepository extends Neo4jRepository<Movie, Long> {

    boolean existsByTitle(String title);

    @Query("""
            MATCH(m:Movie)
            WHERE m.title = $title
            SET m.total_start = $totalStart,
                m.total_rating = $totalRating,
                m.total_view = $totalView
            RETURN m
            """)
    Optional<Movie> updateMovieNode(@Param("title") String title,
                                    @Param("totalStart") int totalStart,
                                    @Param("totalView") int totalView,
                                    @Param("totalRating") int totalRating);


    @Query("""
            CREATE (m:Movie 
                {title: $title, 
                totalStart: $totalStart,
                totalView: $totalView,
                totalRating: $totalRating})
            RETURN m
            """)
    Optional<Movie> createMovieNode(@Param("title") String title,
                                    @Param("totalStart") int totalStart,
                                    @Param("totalView") int totalView,
                                    @Param("totalRating") int totalRating);

    @Query("""
            MATCH (m:Movie {title: 'Superman P3'})
            OPTIONAL MATCH (m)-[r:INCLUDE]-()
            DELETE r, m
            """)
    void deleteMovieNode(@Param("title") String title);


    @Query("""
            MATCH (ms:Movie) WHERE id(ms) = $sourceId
            MATCH (mt:Movie) WHERE id(mt) = $targetId
            MERGE (ms)-[RELATED_WITH]->(mt)
            """)
    void linkMovieToMovie(@Param("sourceId") String sourceId,
                          @Param("targetId") String targetId);


    @Query(
            value = """
            MATCH (m:Movie)
            WHERE toLower(m.title) CONTAINS toLower($titlePattern)
            MATCH (m)-[:RELATED_WITH]-(related:Movie)
            RETURN DISTINCT related
            ORDER BY related.average_rating DESC
            SKIP $skip
            LIMIT $limit
            """,
            countQuery = """
            MATCH (m:Movie)
            WHERE toLower(m.title) CONTAINS toLower($titlePattern)
            MATCH (m)-[:RELATED_WITH]-(related:Movie)
            RETURN count(DISTINCT related)
            """
    )
    Page<Movie> findRelatedMoviesByTitlePattern(@Param("titlePattern") String titlePattern,
                                                Pageable pageable);

    @Query(
            value = """
            MATCH (user:User {username: $username})-[:FRIEND_OF]-(friend:User)
            MATCH (friend)-[follow:FOLLOW_MOVIE]->(movie:Movie)
            WHERE EXISTS((user)-[:LIKED_GENRE]->(:Genre))
            RETURN movie
            ORDER BY movie.average_rating DESC
            SKIP $skip
            LIMIT $limit
            """,
            countQuery = """
            MATCH (user:User {username: $username})-[:FRIEND_OF]-(friend:User)
            MATCH (friend)-[follow:FOLLOW_MOVIE]->(movie:Movie)
            WHERE EXISTS((user)-[:LIKED_GENRE]->(:Genre))
            RETURN count(movie)
            """
    )
    Page<Movie> findMoviesByFollowOfFriendAndLiked(@Param("username") String username,
                                                   Pageable pageable);
}
