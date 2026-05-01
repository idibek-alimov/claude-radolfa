import type { Metadata } from "next";
import { ProductDetail } from "@/entities/product";
import { fetchListingBySlug } from "@/entities/product/api";
import { fetchRatingSummary } from "@/entities/review/api";
import { buildProductJsonLd } from "@/shared/seo";

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

    let jsonLd: Record<string, unknown> | null = null;
    try {
        const [listing, rating] = await Promise.all([
            fetchListingBySlug(slug),
            fetchRatingSummary(slug),
        ]);
        jsonLd = buildProductJsonLd({ listing, rating });
    } catch {
        // Never block the page render on a JSON-LD pre-fetch failure.
    }

    return (
        <>
            {jsonLd && (
                <script
                    type="application/ld+json"
                    dangerouslySetInnerHTML={{ __html: JSON.stringify(jsonLd) }}
                />
            )}
            <ProductDetail slug={slug} />
        </>
    );
}
