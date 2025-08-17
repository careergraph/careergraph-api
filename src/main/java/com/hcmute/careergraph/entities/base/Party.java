package com.hcmute.careergraph.entities.base;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hcmute.careergraph.constant.EntityLabels;
import com.hcmute.careergraph.entities.graph.nodes.Address;
import com.hcmute.careergraph.entities.graph.nodes.Contact;
import lombok.Data;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.Transient;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.HashSet;
import java.util.Set;

@Data
@SuperBuilder
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class Party extends BaseGraph implements UUIDEntity {

    private String name;
    private String tagname;
    private String avatar;
    private String cover;
    private int noOfFollowers;
    private int noOfFollowing;
    private int noOfConnections;

    @Relationship(type = EntityLabels.CONTACT, direction = Relationship.Direction.OUTGOING)
    private Set<Contact> contacts;

    @Relationship(type = EntityLabels.ADDRESS, direction = Relationship.Direction.OUTGOING)
    private Set<Address> addresses;

    @Transient
    private Contact primaryContact;

    @Transient
    private Address primaryAddress;

    public Contact getPrimaryContact() {
        if (primaryContact == null) {
            primaryContact = contacts.stream()
                    .filter(contact -> contact.getIsPrimary() != null && contact.getIsPrimary())
                    .findFirst()
                    .orElse(null);
        }
        return primaryContact;
    }

    public Address getPrimaryAddress() {
        if (primaryAddress == null) {
            primaryAddress = addresses.stream()
                    .filter(address -> address.getIsPrimary() != null && address.getIsPrimary())
                    .findFirst()
                    .orElse(null);
        }
        return primaryAddress;
    }

    public void addContact(Contact contact) {
        if (contacts == null) {
            contacts = new HashSet<>();
        }
        contacts.add(contact);

        if (contact.getIsPrimary() != null && contact.getIsPrimary()) {
            this.primaryContact = contact;
        }
    }

    public void removeContact(Contact contact) {
        if (contacts != null) {
            contacts.remove(contact);

            // Find new contact primary
            if (contact.equals(primaryContact)) {
                primaryContact = null;
                getPrimaryContact();
            }
        }
    }

    public Contact getContactByType(String type) {
        return contacts.stream()
                .filter(contact -> type.toUpperCase().equals(contact.getType()))
                .findFirst()
                .orElse(null);
    }
}
