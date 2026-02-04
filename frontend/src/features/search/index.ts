// ── Public API of the search feature slice ──────────────────────
export type { SearchParams, SearchResult } from "./model/types";
export { default as SearchBar } from "./ui/SearchBar";
export { searchProducts } from "./api";
