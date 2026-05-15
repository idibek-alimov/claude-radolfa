package tj.radolfa.domain.model;

/**
 * Authorised roles.
 */
public enum UserRole {
    USER,
    MANAGER,
    ADMIN,           // full platform administration — price, stock, orders, user management
    COURIER,         // field delivery staff — can view/update their assigned orders
    PICKPOINT_STAFF  // pickup point operator — can confirm customer pickups at their location
}
