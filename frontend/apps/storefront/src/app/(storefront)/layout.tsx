import { Navbar } from "@/widgets/Navbar";
import { Footer } from "@/widgets/Footer";
import { PromoBar } from "@/widgets/PromoBar";
import { BottomNav } from "@/widgets/BottomNav";

export default function StorefrontLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <div className="flex flex-col min-h-screen">
      <PromoBar />
      <Navbar />
      {/* pb-16 md:pb-0 reserves space for the mobile bottom nav */}
      <main className="flex-1 pb-16 md:pb-0">{children}</main>
      <Footer />
      <BottomNav />
    </div>
  );
}
