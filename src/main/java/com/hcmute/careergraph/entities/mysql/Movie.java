package com.hcmute.careergraph.entities.mysql;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;
import java.time.LocalDate;


@Entity
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table(name = "tbl_movie")
public class Movie extends Abstract<String> implements Serializable {

    @Column(name = "title")
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "release_year")
    private int releaseYear;

    @Column(name = "release_date")
    private LocalDate releaseDate;

    @Column(name = "duration")
    private int duration;

    @Column(name = "language")
    private String language;

    @Column(name = "country")
    private String country;

    @Column(name = "slug")
    private String slug;
}
