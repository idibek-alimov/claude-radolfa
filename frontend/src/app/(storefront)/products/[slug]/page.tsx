import { ProductDetail } from "@/entities/product";

interface DetailPageProps {
    params: {
        /** The dynamic segment value — maps to the listing variant's slug. */
        slug: string;
    };
}

/**
 * Product detail page — server component.
 *
 * Navbar and Footer are provided by the (storefront) layout.
 */
export default function DetailPage({ params }: DetailPageProps) {
    return <ProductDetail slug={params.slug} />;
}
