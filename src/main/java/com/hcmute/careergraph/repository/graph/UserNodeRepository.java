package com.hcmute.careergraph.repository.graph;

import com.hcmute.careergraph.entities.graph.User;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserNodeRepository extends Neo4jRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    // Lấy data của nhìu loại node khác nhau

    @Query("""
            MATCH (u:User {username: $username})
            CALL apoc.path.subgraphNodes(u, {
                relationshipFilter: 'FRIEND_OF',
                minLevel: 1,
                maxLevel: $depth
            }) YIELD node
            WHERE node <> u
            RETURN DISTINCT node as friend
            """)
    List<User> findAllFriendsWithDepth(@Param("username") String username,
                                       @Param("depth") int depth);
}
