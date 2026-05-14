import type { Metadata } from "next";
import { ProductQuestionsPage } from "@/views/product-questions/ProductQuestionsPage";

interface QuestionsPageProps {
  params: Promise<{ slug: string }>;
}

export async function generateMetadata({ params }: QuestionsPageProps): Promise<Metadata> {
  const { slug } = await params;
  const title = slug
    .replace(/-/g, " ")
    .replace(/\b\w/g, (c) => c.toUpperCase());
  return {
    title: `Questions — ${title} — Radolfa`,
    description: `Customer questions for ${title} on Radolfa.`,
  };
}

export default async function QuestionsPage({ params }: QuestionsPageProps) {
  const { slug } = await params;
  return <ProductQuestionsPage slug={slug} />;
}
