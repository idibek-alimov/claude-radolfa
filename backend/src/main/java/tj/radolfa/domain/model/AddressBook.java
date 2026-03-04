package tj.radolfa.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The user's address book — a mutable aggregate root.
 *
 * <p>Pure Java — zero Spring / JPA / Jackson / Lombok dependencies.
 *
 * <p>Enforces the invariant: <b>at most one address may be the default at any time</b>.
 * All mutations (add, update, remove, setDefault) go through this aggregate.
 */
public class AddressBook {

    private final Long userId;
    private final List<Address> addresses;

    public AddressBook(Long userId, List<Address> addresses) {
        if (userId == null) {
            throw new IllegalArgumentException("AddressBook userId must not be null");
        }
        this.userId = userId;
        this.addresses = new ArrayList<>(addresses != null ? addresses : List.of());
    }

    /**
     * Adds a new address to the book. If {@code isDefault} is true,
     * the previous default (if any) is cleared first.
     */
    public void addAddress(String label,
                           String recipientName,
                           String phone,
                           String street,
                           String city,
                           String region,
                           String country,
                           boolean isDefault) {
        if (isDefault) {
            clearDefaultFlag();
        }
        addresses.add(new Address(null, label, recipientName, phone, street, city, region, country, isDefault));
    }

    /**
     * Updates all mutable fields of an existing address by its id.
     * If {@code isDefault} is true the previous default is cleared first.
     *
     * @throws IllegalArgumentException if addressId is not found
     */
    public void updateAddress(Long addressId,
                              String label,
                              String recipientName,
                              String phone,
                              String street,
                              String city,
                              String region,
                              String country,
                              boolean isDefault) {
        Address target = findById(addressId);
        if (isDefault && !target.isDefault()) {
            clearDefaultFlag();
        }
        target.setLabel(label);
        target.setRecipientName(recipientName);
        target.setPhone(phone);
        target.setStreet(street);
        target.setCity(city);
        target.setRegion(region);
        target.setCountry(country);
        target.setDefault(isDefault);
    }

    /**
     * Removes an address by its id.
     *
     * @throws IllegalArgumentException if addressId is not found
     */
    public void removeAddress(Long addressId) {
        Address target = findById(addressId);
        addresses.remove(target);
    }

    /**
     * Sets the given address as the default, clearing any previous default.
     *
     * @throws IllegalArgumentException if addressId is not found
     */
    public void setDefault(Long addressId) {
        findById(addressId); // validates existence
        clearDefaultFlag();
        findById(addressId).setDefault(true);
    }

    // ---- Private helpers ----

    private Address findById(Long addressId) {
        return addresses.stream()
                .filter(a -> addressId.equals(a.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Address not found: " + addressId));
    }

    private void clearDefaultFlag() {
        addresses.forEach(a -> a.setDefault(false));
    }

    // ---- Getters ----
    public Long          getUserId()   { return userId; }
    public List<Address> getAddresses() { return Collections.unmodifiableList(addresses); }
}
