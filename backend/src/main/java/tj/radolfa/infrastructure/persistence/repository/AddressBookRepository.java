package tj.radolfa.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tj.radolfa.infrastructure.persistence.entity.AddressBookEntity;

import java.util.Optional;

public interface AddressBookRepository extends JpaRepository<AddressBookEntity, Long> {
    Optional<AddressBookEntity> findByUserId(Long userId);
}
