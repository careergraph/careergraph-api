package com.hcmute.careergraph.entities.graph;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.*;

import java.util.HashSet;
import java.util.Set;

@Node("Director")
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class Director {

    @Id
    @GeneratedValue
    private Long id;

    @Property("name")
    private String name;

    @Relationship(type = "DIRECTED", direction = Relationship.Direction.OUTGOING)
    private Set<Movie> movies = new HashSet<>();
}
