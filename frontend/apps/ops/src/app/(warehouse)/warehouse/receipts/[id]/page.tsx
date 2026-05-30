import { StockReceiptDetailPage } from "@/features/warehouse-receipts";

interface Props {
  params: Promise<{ id: string }>;
}

export default async function ReceiptDetailPage({ params }: Props) {
  const { id } = await params;
  return <StockReceiptDetailPage id={Number(id)} />;
}
