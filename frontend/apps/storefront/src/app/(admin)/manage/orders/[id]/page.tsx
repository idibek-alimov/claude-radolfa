"use client";

import { useParams } from "next/navigation";
import { AdminOrderDetailView } from "@/features/order-management";

export default function Page() {
  const { id } = useParams<{ id: string }>();
  return <AdminOrderDetailView orderId={Number(id)} />;
}
