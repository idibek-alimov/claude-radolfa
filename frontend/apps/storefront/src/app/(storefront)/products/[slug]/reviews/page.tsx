import type { Metadata } from "next";
import { ProductReviewsPage } from "@/views/product-reviews/ProductReviewsPage";

interface ReviewsPageProps {
  params: Promise<{ slug: string }>;
}

export async function generateMetadata({ params }: ReviewsPageProps): Promise<Metadata> {
  const { slug } = await params;
  const title = slug
    .replace(/-/g, " ")
    .replace(/\b\w/g, (c) => c.toUpperCase());
  return {
    title: `Reviews — ${title} — Radolfa`,
    description: `Customer reviews for ${title} on Radolfa.`,
  };
}

export default async function ReviewsPage({ params }: ReviewsPageProps) {
  const { slug } = await params;
  return <ProductReviewsPage slug={slug} />;
}
