import { Navbar } from "@/widgets/Navbar";
import { CatalogSection } from "@/widgets/ProductList";

/**
 * Catalog page — server component.
 *
 * Infinite-scroll pagination and search interaction live in
 * CatalogSection (a "use client" component).
 */
export default function CatalogPage() {
  return (
    <div>
      <Navbar />
      <main>
        <CatalogSection />
      </main>
    </div>
  );
}
