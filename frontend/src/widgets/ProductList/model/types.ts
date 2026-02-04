import type { Product } from "@/entities/product";

export interface ProductListProps {
  /** The current page of products to render. */
  products: Product[];
  /** True while the next page is being fetched. */
  loading?: boolean;
  /** Whether another page exists on the server. */
  hasMore?: boolean;
  /** Callback wired to the "Load More" button. */
  onLoadMore?: () => void;
}
