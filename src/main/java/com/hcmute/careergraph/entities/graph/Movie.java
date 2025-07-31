package com.hcmute.careergraph.entities.graph;

import com.hcmute.careergraph.utils.ThresholdUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Transient;
import org.springframework.data.neo4j.core.schema.*;

import java.util.ArrayList;
import java.util.List;

@Node("Movie")
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class Movie {

    @Id
    @GeneratedValue
    private Long id;

    @Property("title")
    private String title;

    @Property("total_view")
    private int totalView;

    @Property("total_rating")
    private int totalRating;

    @Property("total_start")
    private int totalStart;

    @Relationship(type = "RELATED_WITH", direction = Relationship.Direction.OUTGOING)
    private List<Movie> movies = new ArrayList<>();

    @Relationship(type = "BELONGS_TO", direction = Relationship.Direction.OUTGOING)
    private List<Genre> genres = new ArrayList<>();

    @Relationship(type = "ACTED_IN", direction = Relationship.Direction.INCOMING)
    private List<Actor> actors = new ArrayList<>();

    @Relationship(type = "DIRECTED", direction = Relationship.Direction.INCOMING)
    private List<Director> directors = new ArrayList<>();

    @Relationship(type = "RATED", direction = Relationship.Direction.INCOMING)
    private List<User> users = new ArrayList<>();

    @Transient
    private boolean isTrending;


    @Transient
    private double averageRating;

    public boolean getIsTrending() {
        return this.totalRating >= ThresholdUtil.RATING_THRESHOLD
                && this.totalView >= ThresholdUtil.VIEW_THRESHOLD;
    }

    public double getAverageRating() {

        if (this.totalRating == 0) {
            return 0;
        }

        this.averageRating = Math.round((double) this.totalStart / this.totalRating);
        return this.averageRating;
    }
}
