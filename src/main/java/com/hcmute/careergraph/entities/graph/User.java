package com.hcmute.careergraph.entities.graph;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.*;

import java.util.HashSet;
import java.util.Set;

@Node("User")
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class User {

    @Id
    @GeneratedValue
    private Long id;

    @Property("username")
    private String username;

    @Relationship(type = "FRIEND_OF", direction = Relationship.Direction.OUTGOING)
    private Set<User> friends = new HashSet<>();

    @Relationship(type = "FOLLOW_MOVIE", direction = Relationship.Direction.OUTGOING)
    private Set<Movie> movies = new HashSet<>();

    @Relationship(type = "LIKED_GENRE", direction = Relationship.Direction.OUTGOING)
    private Set<Genre> genres = new HashSet<>();

    // Do not save relationship into movie
}
