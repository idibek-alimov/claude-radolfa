export type { FeaturedCategory, FeaturedCategoryRequest } from "./model/types";
export type { FetchFeaturedCategoriesParams } from "./api/admin";
export {
  fetchFeaturedCategories,
  createFeaturedCategory,
  updateFeaturedCategory,
  deleteFeaturedCategory,
  uploadFeaturedCategoryImage,
} from "./api/admin";
