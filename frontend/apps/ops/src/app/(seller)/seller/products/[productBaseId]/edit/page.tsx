import { SellerProductEditPage } from "@/features/seller-products/ui/SellerProductEditPage";

export default function Page({
  params,
}: {
  params: { productBaseId: string };
}) {
  return <SellerProductEditPage productBaseId={Number(params.productBaseId)} />;
}
