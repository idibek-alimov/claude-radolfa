import { fetchListings } from "@/entities/product";
import type { PaginatedResponse } from "@/shared/api/types";
import type { ListingVariant } from "@/entities/product";

export type { PaginatedResponse };

/** Paginated listing catalog — delegates to entities layer. */
export { fetchListings };
export type PaginatedListings = PaginatedResponse<ListingVariant>;
