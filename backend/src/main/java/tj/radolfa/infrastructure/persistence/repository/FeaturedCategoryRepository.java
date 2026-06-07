package tj.radolfa.infrastructure.persistence.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tj.radolfa.infrastructure.persistence.entity.FeaturedCategoryEntity;

import java.util.List;

public interface FeaturedCategoryRepository extends JpaRepository<FeaturedCategoryEntity, Long> {

    List<FeaturedCategoryEntity> findByActiveTrueOrderByDisplayOrderAsc();

    @Query("""
            SELECT f FROM FeaturedCategoryEntity f
            WHERE :search = ''
               OR LOWER(f.title) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(f.subtitle) LIKE LOWER(CONCAT('%', :search, '%'))
            """)
    Page<FeaturedCategoryEntity> search(@Param("search") String search, Pageable pageable);
}
