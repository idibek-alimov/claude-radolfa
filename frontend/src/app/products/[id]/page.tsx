import { Navbar } from "@/widgets/Navbar";
import { ProductDetail } from "@/entities/product";

interface DetailPageProps {
  params: {
    /** The dynamic segment value — maps 1-to-1 to the ERP identifier. */
    id: string;
  };
}

/**
 * Product detail page — server component.
 *
 * The [id] segment is the erpId.  All client-side fetching and rendering
 * is delegated to ProductDetail (a "use client" component).
 */
export default function DetailPage({ params }: DetailPageProps) {
  return (
    <div>
      <Navbar />
      <main>
        <ProductDetail erpId={params.id} />
      </main>
    </div>
  );
}
