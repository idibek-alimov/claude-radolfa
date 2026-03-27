/**
 * Generic Spring Page wrapper returned by all paginated endpoints.
 * Backend is 1-based; frontend sends page as-is (starting from 1).
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
