import Link from "next/link";
import { useTranslations } from "next-intl";
import {
  Breadcrumb,
  BreadcrumbItem,
  BreadcrumbLink,
  BreadcrumbList,
  BreadcrumbPage,
  BreadcrumbSeparator,
} from "@radolfa/shared/ui/breadcrumb";

interface ProductBreadcrumbProps {
  categoryName: string | null;
  categorySlug?: string | null;
  productName: string;
  className?: string;
}

/** Single-level breadcrumb: Home / {categoryName?} / {productName}. Rendered once for
 * desktop (above the gallery/buy-box grid) and once for mobile (under the gallery,
 * above the title) — see `ProductDetailView`. When `categorySlug` is provided the
 * category segment becomes a real link to `/categories/{slug}/products`. */
export default function ProductBreadcrumb({
  categoryName,
  categorySlug,
  productName,
  className,
}: ProductBreadcrumbProps) {
  const t = useTranslations("productDetail");

  return (
    <Breadcrumb className={className}>
      <BreadcrumbList className="gap-1.5 text-[12px] text-ink/55 sm:gap-1.5">
        <BreadcrumbItem>
          <BreadcrumbLink asChild className="hover:text-mag">
            <Link href="/">{t("home")}</Link>
          </BreadcrumbLink>
        </BreadcrumbItem>
        {categoryName && (
          <>
            <BreadcrumbSeparator>
              <span className="opacity-50">/</span>
            </BreadcrumbSeparator>
            <BreadcrumbItem>
              {categorySlug ? (
                <BreadcrumbLink asChild className="hover:text-mag">
                  <Link href={`/categories/${categorySlug}/products`}>{categoryName}</Link>
                </BreadcrumbLink>
              ) : (
                <span>{categoryName}</span>
              )}
            </BreadcrumbItem>
          </>
        )}
        <BreadcrumbSeparator>
          <span className="opacity-50">/</span>
        </BreadcrumbSeparator>
        <BreadcrumbItem>
          <BreadcrumbPage className="text-ink/80 font-semibold">{productName}</BreadcrumbPage>
        </BreadcrumbItem>
      </BreadcrumbList>
    </Breadcrumb>
  );
}
