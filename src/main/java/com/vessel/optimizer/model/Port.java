package com.vessel.optimizer.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ports", indexes = {
    @Index(name = "idx_port_code", columnList = "code", unique = true),
    @Index(name = "idx_port_coords", columnList = "latitude, longitude")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Port {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", length = 5, nullable = false, unique = true)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "country", length = 2)
    private String country;

    @Column(name = "latitude", nullable = false)
    private Double latitude;

    @Column(name = "longitude", nullable = false)
    private Double longitude;

    @Column(name = "is_active")
    private Boolean isActive = true;

    public boolean hasValidCoordinates() {
        return latitude != null && longitude != null
            && latitude >= -90 && latitude <= 90
            && longitude >= -180 && longitude <= 180;
    }
}