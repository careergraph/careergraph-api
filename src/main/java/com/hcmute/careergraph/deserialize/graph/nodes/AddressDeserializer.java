package com.hcmute.careergraph.deserialize.graph.nodes;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.hcmute.careergraph.entities.graph.nodes.Address;

import java.io.IOException;

public class AddressDeserializer extends StdDeserializer<Address> {

    public AddressDeserializer() {
        this(null);
    }

    public AddressDeserializer(Class<?> t) {
        super(t);
    }

    @Override
    public Address deserialize(JsonParser jp, DeserializationContext dt)
            throws IOException {

        JsonNode node = jp.getCodec().readTree(jp);

        String id = node.has("id") ? node.get("id").asText() : null;
        String uuid = node.has("uuid") ? node.get("uuid").asText() : null;
        Long createDate = node.has("createDate") ? node.get("createDate").asLong() : null;
        String createdBy = node.has("createdBy") ? node.get("createdBy").asText() : null;

        String name = node.has("name") ? node.get("name").asText() : null;
        String country = node.has("country") ? node.get("country").asText() : null;
        String province = node.has("province") ? node.get("province").asText() : null;
        String district = node.has("district") ? node.get("district").asText() : null;
        String ward = node.has("ward") ? node.get("ward").asText() : null;

        Address address = Address.builder()
                .name(name)
                .country(country)
                .province(province)
                .district(district)
                .ward(ward)
                .build();

        if (id != null) address.setId(id);
        if (uuid != null) address.setUUID(uuid);
        if (createDate != null) address.setCreateDate(createDate);
        if (createdBy != null) address.setCreatedBy(createdBy);

        return address;
    }
}