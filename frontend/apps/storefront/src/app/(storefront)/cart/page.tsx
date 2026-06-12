import type { Metadata } from "next";
import { CartPage } from "@/views/cart";

export const metadata: Metadata = {
  title: "Your Cart — Radolfa",
  description: "Review the items in your cart and proceed to checkout.",
};

export default function Page() {
  return <CartPage />;
}
