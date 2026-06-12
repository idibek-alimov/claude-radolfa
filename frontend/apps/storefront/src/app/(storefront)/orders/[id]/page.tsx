import type { Metadata } from "next";
import { CustomerOrderDetailPage } from "@/features/order-detail";

export const metadata: Metadata = { title: "Order Details" };

export default function OrderDetailPage() {
  return <CustomerOrderDetailPage />;
}
