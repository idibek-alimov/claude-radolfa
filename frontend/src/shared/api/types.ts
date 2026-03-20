/**
 * Generic Spring Page wrapper returned by all paginated endpoints.
 * Backend is 0-based; frontend converts before sending (page - 1).
 */
export interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;   // 0-based current page
  size: number;
  first: boolean;
  last: boolean;
}
