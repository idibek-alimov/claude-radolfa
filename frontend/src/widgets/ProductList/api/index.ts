import { fetchListings } from "@/entities/product";
import type { PaginatedListings } from "@/entities/product";
import type { ListingFilters, ListingSort } from "@/entities/product/api";

export type { PaginatedListings, ListingFilters, ListingSort };

/** Paginated listing catalog — delegates to entities layer. */
export { fetchListings };
