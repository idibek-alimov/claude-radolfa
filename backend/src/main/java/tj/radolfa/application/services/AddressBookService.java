package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.AddAddressUseCase;
import tj.radolfa.application.ports.in.GetAddressBookUseCase;
import tj.radolfa.application.ports.in.RemoveAddressUseCase;
import tj.radolfa.application.ports.in.SetDefaultAddressUseCase;
import tj.radolfa.application.ports.in.UpdateAddressUseCase;
import tj.radolfa.application.ports.out.LoadAddressBookPort;
import tj.radolfa.application.ports.out.SaveAddressBookPort;
import tj.radolfa.domain.model.AddressBook;

import java.util.ArrayList;

/**
 * Single service bean implementing all five address-book use-cases.
 *
 * <p>Follows the same "inner static adapter" pattern as {@link CartService}.
 * Each use-case interface is implemented by a dedicated package-private
 * static adapter that delegates to this shared service bean.
 */
@Service
@Transactional
public class AddressBookService {

    private final LoadAddressBookPort loadPort;
    private final SaveAddressBookPort savePort;

    public AddressBookService(LoadAddressBookPort loadPort,
                              SaveAddressBookPort savePort) {
        this.loadPort = loadPort;
        this.savePort = savePort;
    }

    // ---- Use-case implementations ----

    @Transactional(readOnly = true)
    public AddressBook getAddressBook(Long userId) {
        return loadPort.load(userId)
                .orElseGet(() -> new AddressBook(userId, new ArrayList<>()));
    }

    public AddressBook addAddress(Long userId,
                                  String label,
                                  String recipientName,
                                  String phone,
                                  String street,
                                  String city,
                                  String region,
                                  String country,
                                  boolean isDefault) {
        AddressBook book = loadPort.load(userId)
                .orElseGet(() -> new AddressBook(userId, new ArrayList<>()));
        book.addAddress(label, recipientName, phone, street, city, region, country, isDefault);
        return savePort.save(book);
    }

    public AddressBook updateAddress(Long userId,
                                     Long addressId,
                                     String label,
                                     String recipientName,
                                     String phone,
                                     String street,
                                     String city,
                                     String region,
                                     String country,
                                     boolean isDefault) {
        AddressBook book = loadPort.load(userId)
                .orElseThrow(() -> new IllegalArgumentException("No address book found for user: " + userId));
        book.updateAddress(addressId, label, recipientName, phone, street, city, region, country, isDefault);
        return savePort.save(book);
    }

    public AddressBook removeAddress(Long userId, Long addressId) {
        AddressBook book = loadPort.load(userId)
                .orElseThrow(() -> new IllegalArgumentException("No address book found for user: " + userId));
        book.removeAddress(addressId);
        return savePort.save(book);
    }

    public AddressBook setDefault(Long userId, Long addressId) {
        AddressBook book = loadPort.load(userId)
                .orElseThrow(() -> new IllegalArgumentException("No address book found for user: " + userId));
        book.setDefault(addressId);
        return savePort.save(book);
    }

    // ======================================================================
    // Port adapter beans — one Spring bean per use-case interface.
    // ======================================================================

    @Service
    public static class GetAddressBookAdapter implements GetAddressBookUseCase {
        private final AddressBookService service;
        public GetAddressBookAdapter(AddressBookService service) { this.service = service; }

        @Override
        public AddressBook execute(Long userId) { return service.getAddressBook(userId); }
    }

    @Service
    public static class AddAddressAdapter implements AddAddressUseCase {
        private final AddressBookService service;
        public AddAddressAdapter(AddressBookService service) { this.service = service; }

        @Override
        public AddressBook execute(Long userId, String label, String recipientName, String phone,
                                   String street, String city, String region, String country,
                                   boolean isDefault) {
            return service.addAddress(userId, label, recipientName, phone, street, city, region, country, isDefault);
        }
    }

    @Service
    public static class UpdateAddressAdapter implements UpdateAddressUseCase {
        private final AddressBookService service;
        public UpdateAddressAdapter(AddressBookService service) { this.service = service; }

        @Override
        public AddressBook execute(Long userId, Long addressId, String label, String recipientName,
                                   String phone, String street, String city, String region,
                                   String country, boolean isDefault) {
            return service.updateAddress(userId, addressId, label, recipientName, phone, street, city, region, country, isDefault);
        }
    }

    @Service
    public static class RemoveAddressAdapter implements RemoveAddressUseCase {
        private final AddressBookService service;
        public RemoveAddressAdapter(AddressBookService service) { this.service = service; }

        @Override
        public AddressBook execute(Long userId, Long addressId) {
            return service.removeAddress(userId, addressId);
        }
    }

    @Service
    public static class SetDefaultAddressAdapter implements SetDefaultAddressUseCase {
        private final AddressBookService service;
        public SetDefaultAddressAdapter(AddressBookService service) { this.service = service; }

        @Override
        public AddressBook execute(Long userId, Long addressId) {
            return service.setDefault(userId, addressId);
        }
    }
}
