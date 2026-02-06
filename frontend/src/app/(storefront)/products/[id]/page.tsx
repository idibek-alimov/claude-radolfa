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
 * Navbar and Footer are provided by the (storefront) layout.
 */
export default function DetailPage({ params }: DetailPageProps) {
  return <ProductDetail erpId={params.id} />;
}
