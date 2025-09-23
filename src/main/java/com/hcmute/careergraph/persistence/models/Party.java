package com.hcmute.careergraph.persistence.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "parties")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "party_type")
@EntityListeners(AuditingEntityListener.class)
@JsonIgnoreProperties(ignoreUnknown = true)
@SuperBuilder
public abstract class Party extends BaseEntity {

    @Column(name = "name")
    private String name;

    @Column(name = "tagname")
    private String tagname;

    @Column(name = "avatar")
    private String avatar;

    @Column(name = "cover")
    private String cover;

    @Column(name = "no_of_followers", columnDefinition = "int default 0")
    private int noOfFollowers;

    @Column(name = "no_of_following", columnDefinition = "int default 0")
    private int noOfFollowing;

    @Column(name = "no_of_connections", columnDefinition = "int default 0")
    private int noOfConnections;

    // Quan hệ với Contact
    @OneToMany(mappedBy = "party", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<Contact> contacts = new HashSet<>();

    // Quan hệ với Address
    @OneToMany(mappedBy = "party", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<Address> addresses = new HashSet<>();

    public Party() {}

    // Helper methods
    public void addContact(Contact contact) {
        contacts.add(contact);
        contact.setParty(this);
    }

    public void removeContact(Contact contact) {
        contacts.remove(contact);
        contact.setParty(null);
    }

    public void addAddress(Address address) {
        addresses.add(address);
        address.setParty(this);
    }

    public void removeAddress(Address address) {
        addresses.remove(address);
        address.setParty(null);
    }
}
