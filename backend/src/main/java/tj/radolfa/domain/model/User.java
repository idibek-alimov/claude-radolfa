package tj.radolfa.domain.model;

import java.math.BigDecimal;

/**
 * Immutable domain representation of an application user.
 */
public record User(
                Long id,
                PhoneNumber phone,
                UserRole role,
                String name,
                String email,
                LoyaltyProfile loyalty,
                boolean enabled,
                Long version,
                // Courier-specific fields (null for non-COURIER roles)
                VehicleType vehicleType,
                BigDecimal maxPayloadKg,
                Integer maxLengthCm,
                Integer maxWidthCm,
                Integer maxHeightCm,
                // Pickpoint staff field (null for non-PICKPOINT_STAFF roles)
                Long pickpointId,
                // Reserved for future zone assignment feature
                Long deliveryZoneId) {

    /** Convenience constructor preserving backward compatibility for existing callers. */
    public User(Long id, PhoneNumber phone, UserRole role, String name, String email,
                LoyaltyProfile loyalty, boolean enabled, Long version) {
        this(id, phone, role, name, email, loyalty, enabled, version,
             null, null, null, null, null, null, null);
    }
}
