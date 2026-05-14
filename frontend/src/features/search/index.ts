// ── Public API of the search feature slice ──────────────────────
export type { SearchParams, SearchResult } from "./model/types";
export type { ReindexResult } from "./api";
export { default as SearchBar } from "./ui/SearchBar";
export { searchProducts, reindexSearch } from "./api";
