package tj.radolfa.infrastructure.persistence.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import tj.radolfa.domain.model.Review;
import tj.radolfa.infrastructure.persistence.entity.ReviewEntity;
import tj.radolfa.infrastructure.persistence.entity.ReviewPhotoEntity;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ReviewMapper {

    @Mapping(target = "photos", expression = "java(toPhotoUrls(entity.getPhotos()))")
    Review toReview(ReviewEntity entity);

    @Mapping(target = "photos", ignore = true)   // rebuilt by adapter (back-reference required)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    ReviewEntity toEntity(Review review);

    // ---- Photo helpers -----------------------------------------------

    default List<String> toPhotoUrls(List<ReviewPhotoEntity> photos) {
        if (photos == null) return List.of();
        return photos.stream()
                .sorted(java.util.Comparator.comparingInt(ReviewPhotoEntity::getSortOrder))
                .map(ReviewPhotoEntity::getUrl)
                .toList();
    }
}
