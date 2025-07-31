package com.hcmute.careergraph.repository.sql;

import com.hcmute.careergraph.entities.mysql.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AddressRepository extends JpaRepository<Address, String> {

    @Query("SELECT COUNT(a) > 0 FROM Address a WHERE " +
            "LOWER(a.country) = LOWER(:country) AND " +
            "LOWER(a.city) = LOWER(:city) AND " +
            "LOWER(a.district) = LOWER(:district) AND " +
            "LOWER(a.special) = LOWER(:special)")
    boolean existsByLocationIgnoreCase(
            @Param("country") String country,
            @Param("city") String city,
            @Param("district") String district,
            @Param("special") String special);

    Optional<Address> findByCountryAndCityAndDistrictAndSpecial(String country,
                                                                String city,
                                                                String district,
                                                                String special);
}
