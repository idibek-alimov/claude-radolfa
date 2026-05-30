"use client";

import { useParams } from "next/navigation";
import { ReturnReviewPage } from "@/features/warehouse-returns";

export default function Page() {
  const { id } = useParams<{ id: string }>();
  return <ReturnReviewPage id={Number(id)} />;
}
