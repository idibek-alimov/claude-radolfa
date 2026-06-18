import type { Metadata } from "next";
import { ProductDetailView } from "@/widgets/ProductDetail";
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

    // Fallback: derive a human-readable label from the slug in case the fetch fails.
    const fallback = slug
        .replace(/-/g, " ")
        .replace(/\b\w/g, (c) => c.toUpperCase());

    let title = fallback;
    try {
        // Use BACKEND_INTERNAL_URL for server-side fetches — apiClient uses a relative
        // baseURL which has no host when running on the server inside Docker.
        const base = process.env.BACKEND_INTERNAL_URL ?? "";
        const res = await fetch(`${base}/api/v1/listings/${slug}`, { cache: "no-store" });
        if (res.ok) {
            const listing = (await res.json()) as { colorDisplayName?: string };
            if (listing.colorDisplayName) title = listing.colorDisplayName;
        }
    } catch {
        // Keep the slug-based fallback — never block metadata generation.
    }

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
            <ProductDetailView slug={slug} />
        </>
    );
}
