import { PickSessionPage } from "@/features/warehouse-pick";

interface Props {
  params: Promise<{ orderId: string }>;
}

export default async function PickSessionRoute({ params }: Props) {
  const { orderId } = await params;
  return <PickSessionPage orderId={Number(orderId)} />;
}
