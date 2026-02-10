import { fetchListings } from "@/entities/product";
import type { PaginatedListings } from "@/entities/product";

export type { PaginatedListings };

/** Paginated listing catalog — delegates to entities layer. */
export { fetchListings };
