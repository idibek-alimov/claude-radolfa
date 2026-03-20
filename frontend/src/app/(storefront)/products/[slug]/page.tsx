import type { Metadata } from "next";
import { ProductDetail } from "@/entities/product";

interface DetailPageProps {
    params: Promise<{
        /** The dynamic segment value — maps to the listing variant's slug. */
        slug: string;
    }>;
}

export async function generateMetadata({ params }: DetailPageProps): Promise<Metadata> {
    const { slug } = await params;
    const title = slug
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
export default async function DetailPage({ params }: DetailPageProps) {
    const { slug } = await params;
    return <ProductDetail slug={slug} />;
}
