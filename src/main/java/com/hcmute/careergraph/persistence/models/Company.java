package com.hcmute.careergraph.persistence.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.util.Set;

@Entity
@Table(name = "companies")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString(callSuper = true, exclude = {"account", "jobs", "companyConnections"})
@EqualsAndHashCode(callSuper = true, exclude = {"account", "jobs", "companyConnections"})
public class Company extends Party {

    @Column(name = "size")
    private String size;

    @Column(name = "name")
    private String name;

    @Column(name = "website")
    private String website;

    @Column(name = "ceo_name")
    private String ceoName;

    @Column(name = "no_of_members", columnDefinition = "int default 0")
    private Integer noOfMembers;

    @Column(name = "year_founded")
    private Integer yearFounded;

    // Account
    @OneToOne(mappedBy = "company", cascade = CascadeType.ALL, orphanRemoval = true)
    private Account account;

    // One-to-Many relationship with Job
    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Job> jobs;

    // Connections with companies
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "connected_company_id")
    private Set<Connection> companyConnections;
}