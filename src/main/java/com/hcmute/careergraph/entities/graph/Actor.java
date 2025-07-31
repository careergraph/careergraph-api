package com.hcmute.careergraph.entities.graph;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.*;

import java.util.HashSet;
import java.util.Set;

@Node("Actor")
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class Actor {

    @Id
    @GeneratedValue
    private Long id;

    @Property("name")
    private String name;

    @Relationship(type = "ACTED_IN", direction = Relationship.Direction.OUTGOING)
    private Set<Movie> movies = new HashSet<>();

}
