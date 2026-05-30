import type { ListingVariantDetail } from "@/entities/product/model/types";
import type { RatingSummary } from "@/entities/review/model/types";

interface ProductJsonLdInput {
  listing: ListingVariantDetail;
  rating: RatingSummary;
}

export function buildProductJsonLd({ listing, rating }: ProductJsonLdInput): Record<string, unknown> {
  const jsonLd: Record<string, unknown> = {
    "@context": "https://schema.org",
    "@type": "Product",
    name: listing.colorDisplayName,
    sku: listing.productCode ?? undefined,
    description: listing.webDescription ?? undefined,
    image: listing.images.length > 0 ? listing.images : undefined,
    offers: {
      "@type": "Offer",
      price: String(listing.discountPrice ?? listing.originalPrice),
      priceCurrency: "TJS",
      availability: "https://schema.org/InStock",
    },
  };

  if (rating.reviewCount > 0) {
    jsonLd.aggregateRating = {
      "@type": "AggregateRating",
      ratingValue: rating.averageRating,
      reviewCount: rating.reviewCount,
      bestRating: 5,
      worstRating: 1,
    };
  }

  return jsonLd;
}
