import { SlimHeader } from "@/features/checkout";

export default function CheckoutLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <div className="min-h-screen bg-[#F7F2F6]">
      <SlimHeader />
      <main>{children}</main>
    </div>
  );
}
