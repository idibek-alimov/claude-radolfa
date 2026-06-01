import { SellerProductEditPage } from "@/features/seller-products/ui/SellerProductEditPage";

export default async function Page({
  params,
}: {
  params: Promise<{ productBaseId: string }>;
}) {
  const { productBaseId } = await params;
  return <SellerProductEditPage productBaseId={Number(productBaseId)} />;
}
