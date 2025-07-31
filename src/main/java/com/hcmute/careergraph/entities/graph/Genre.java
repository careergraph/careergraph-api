package com.hcmute.careergraph.entities.graph;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Transient;
import org.springframework.data.neo4j.core.schema.*;

import java.util.ArrayList;
import java.util.List;

@Node("Genre")
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class Genre {

    @Id
    @GeneratedValue
    private Long id;

    @Property("name")
    private String name;

    @Relationship(value = "INCLUDE", direction = Relationship.Direction.OUTGOING)
    private List<Movie> movies = new ArrayList<>();

    @Transient
    private boolean isPopular;

    public boolean getIsPopular() {
        return this.movies.size() > 10;
    }
}
