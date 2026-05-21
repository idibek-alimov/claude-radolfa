import type { Metadata } from "next";
import { CustomerReturnsListPage } from "@/features/profile/ui/CustomerReturnsListPage";

export const metadata: Metadata = { title: "My Returns" };

export default function ProfileReturnsPage() {
  return <CustomerReturnsListPage />;
}
