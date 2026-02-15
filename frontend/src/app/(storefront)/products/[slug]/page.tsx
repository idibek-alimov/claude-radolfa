import type { Metadata } from "next";
import { ProductDetail } from "@/entities/product";

interface DetailPageProps {
    params: {
        /** The dynamic segment value — maps to the listing variant's slug. */
        slug: string;
    };
}

export function generateMetadata({ params }: DetailPageProps): Metadata {
    const title = params.slug
        .replace(/-/g, " ")
        .replace(/\b\w/g, (c) => c.toUpperCase());
    return {
        title: `${title} — Radolfa`,
        description: `View details for ${title} on Radolfa.`,
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
