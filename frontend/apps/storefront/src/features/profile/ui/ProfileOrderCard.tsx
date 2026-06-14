"use client";

import Link from "next/link";
import Image from "next/image";
import { useTranslations } from "next-intl";
import { Check } from "lucide-react";
import { QRCodeSVG } from "qrcode.react";
import { cn } from "@radolfa/shared/lib/utils";
import { formatDate, formatPrice } from "@radolfa/shared/lib/format";
import { OrderStatusBadge } from "@/entities/order";
import type { MyOrder } from "@/entities/order";
import { orderRenderKind, buildOrderSteps } from "@/features/profile/lib/orderSteps";
import { OrderStepper } from "./OrderStepper";

type IconProps = { className?: string; size?: number };

/** Home-delivery badge icon — verbatim from the B-Magenta order-card header. */
export function HomeDeliveryIcon({ className, size = 13 }: IconProps) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className={className} aria-hidden>
      <rect x="1" y="3" width="15" height="13" rx="1" />
      <path d="M16 8h4l3 3v5h-7z" />
      <circle cx="5.5" cy="18.5" r="2.5" />
      <circle cx="18.5" cy="18.5" r="2.5" />
    </svg>
  );
}

/** Pickup-point badge icon — verbatim from the B-Magenta order-card header. */
export function PickupIcon({ className, size = 13 }: IconProps) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className={className} aria-hidden>
      <path d="M3 9l1-5h16l1 5" />
      <path d="M4 9v11h16V9" />
      <rect x="9" y="13" width="6" height="7" />
    </svg>
  );
}

/** A single label/value row inside the delivery `<dl>`. */
function DetailRow({ label, value, mono }: { label: string; value: string; mono?: boolean }) {
  return (
    <div className="flex gap-2">
      <dt className="text-ink/45 w-20 lg:w-20 shrink-0">{label}</dt>
      <dd className={cn("font-semibold flex-1", mono && "font-mono text-mag")}>{value}</dd>
    </div>
  );
}

/**
 * The rich B-Magenta order card — verbatim structure from the decoded
 * `<!-- ORDER n -->` blocks, merged into one responsive component.
 *
 * Render kind (design-faithful, Phase 8):
 * - "stepper"   — in-progress, happy-path orders → 5-step `OrderStepper` +
 *                 confirmation-code / QR delivery panel (when `deliveryCode`
 *                 is present).
 * - "delivered" — `DELIVERED` orders → handover-confirmation line + per-item
 *                 "Write a review" buttons.
 * - "exception" — CANCELLED/REFUNDED/RETURN_INITIATED/RETURNED_TO_WAREHOUSE/
 *                 RECALL_REQUESTED → muted status note + struck item price.
 */
export function ProfileOrderCard({ order }: { order: MyOrder }) {
  const t = useTranslations("profile");
  const kind = orderRenderKind(order);
  const isPickup = order.deliveryType === "PICKPOINT";
  const isReadyForPickup = order.status === "READY_FOR_PICKUP";
  const isMuted = kind !== "stepper";

  const firstItem = order.items[0];
  const canBuyAgain = !!firstItem?.slug;

  // Delivery panel (code/QR) only ever exists for in-progress orders with a code.
  const showDeliveryPanel = kind === "stepper" && !!order.deliveryCode;

  // The single timestamp that drives the "delivered" / "exception" note line.
  const exceptionDate =
    order.cancelledAt ??
    order.refundedAt ??
    order.returnedToWarehouseAt ??
    order.returnInitiatedAt ??
    order.recallRequestedAt ??
    null;

  const deliveryAddress =
    order.deliveryAddress ?? ([order.pickpointName, order.pickpointAddress].filter(Boolean).join(" · ") || null);

  return (
    <article
      className={cn(
        "rounded-2xl lg:rounded-3xl bg-white overflow-hidden",
        isReadyForPickup ? "border-2 border-emerald/30" : "border border-ink/8"
      )}
    >
      {/* Header */}
      <header
        className={cn(
          "flex flex-wrap items-center gap-x-3 lg:gap-x-5 gap-y-2 px-4 lg:px-6 py-3 lg:py-4 border-b",
          isReadyForPickup ? "bg-emerald/[0.06] border-emerald/15" : "bg-soft border-ink/8"
        )}
      >
        <span className="font-mono font-bold text-[12px] lg:text-[13px]">
          {t("orderNumber", { id: order.id })}
        </span>
        <span
          className={cn(
            "inline-flex items-center gap-1.5 px-2 py-1 rounded-full text-[11px] font-bold",
            isMuted ? "bg-ink/5 text-ink/55" : "bg-mag/8 text-mag"
          )}
        >
          {isPickup ? <PickupIcon /> : <HomeDeliveryIcon />}
          {isPickup ? t("pickupPoint") : t("homeDelivery")}
        </span>
        <div className="text-[12px] text-ink/55">
          {t("orderPlaced")} <span className="font-semibold text-ink/75">{formatDate(order.createdAt)}</span>
        </div>
        <OrderStatusBadge status={order.status} namespace="profile.status" />
        <div className="ml-auto text-right">
          <div className="text-[10px] text-ink/45 font-bold uppercase tracking-wider">
            {kind === "exception" ? t("refundedLabel") : t("orderTotalLabel")}
          </div>
          <div
            className={cn(
              "font-black tabular-nums text-[15px] lg:text-[17px] leading-none",
              kind === "exception" ? "text-emerald" : "text-mag"
            )}
          >
            {formatPrice(order.totalAmount)}
          </div>
        </div>
      </header>

      {/* Stepper / delivered confirmation / exception note */}
      {kind === "stepper" && (
        <div className="px-4 lg:px-6 pt-4 lg:pt-5">
          <OrderStepper steps={buildOrderSteps(order)} />
        </div>
      )}
      {kind === "delivered" && order.deliveredAt && (
        <div className="px-4 lg:px-6 pt-3 lg:pt-3.5 text-[11px] lg:text-[12px] text-emerald font-semibold inline-flex items-center gap-1.5">
          <Check className="h-3.5 w-3.5 shrink-0" strokeWidth={2.5} />
          {t("orderDelivered")} {formatDate(order.deliveredAt)}
        </div>
      )}
      {kind === "exception" && exceptionDate && (
        <div className="px-4 lg:px-6 pt-3 text-[12px] text-ink/55">{formatDate(exceptionDate)}</div>
      )}

      <div className="grid md:grid-cols-12 gap-0">
        {/* Items */}
        <div className={cn("px-4 lg:px-6 py-3 lg:py-4 divide-y divide-ink/8", showDeliveryPanel ? "md:col-span-7" : "md:col-span-12")}>
          {order.items.map((item, i) => (
            <div key={i} className="flex items-center gap-3 lg:gap-4 py-2.5 lg:py-3">
              <div className="w-12 h-12 lg:w-14 lg:h-14 rounded-xl lg:rounded-2xl bg-plum overflow-hidden shrink-0 relative">
                {item.imageUrl && (
                  <Image src={item.imageUrl} alt={item.productName} fill className="object-cover" unoptimized />
                )}
              </div>
              <div className="min-w-0 flex-1">
                <div className="text-[12px] lg:text-[14px] font-bold truncate">{item.productName}</div>
                <div className="text-[10px] lg:text-[12px] text-ink/55">
                  {t("quantity")} {item.quantity}
                  {item.sizeLabel ? ` · ${item.sizeLabel}` : ""}
                </div>
              </div>
              <div className="text-right shrink-0">
                <div
                  className={cn(
                    "font-black tabular-nums text-[12px] lg:text-[14px]",
                    kind === "exception" && "text-ink/40 line-through"
                  )}
                >
                  {formatPrice(item.price)}
                </div>
                {kind === "delivered" && !item.hasReviewed && item.slug && (
                  <Link
                    href={`/products/${item.slug}`}
                    className="mt-1 inline-flex h-6 lg:h-7 px-2 lg:px-3 rounded-full border-2 border-ink/15 text-[10px] lg:text-[11px] font-bold items-center"
                  >
                    {t("writeReview")}
                  </Link>
                )}
              </div>
            </div>
          ))}
        </div>

        {/* Delivery panel — confirmation code (HOME) or QR (PICKPOINT) */}
        {showDeliveryPanel && (
          <aside
            className={cn(
              "md:col-span-5 md:border-l border-ink/8 px-4 lg:px-6 py-3 lg:py-4",
              isPickup ? "bg-emerald/[0.04]" : "bg-mag/[0.03]"
            )}
          >
            {isPickup ? (
              <div className="flex items-center gap-4">
                <div className="bg-white border border-ink/10 rounded-xl lg:rounded-[14px] p-2 w-[84px] h-[84px] lg:w-[104px] lg:h-[104px] shrink-0 flex items-center justify-center">
                  <QRCodeSVG value={order.deliveryCode!} size={84} level="M" />
                </div>
                <div className="min-w-0">
                  <div className="text-[10px] lg:text-[11px] uppercase tracking-wider text-emerald font-bold">
                    {t("pickupCodeLabel")}
                  </div>
                  <div className="font-mono font-black text-[22px] lg:text-[26px] tabular-nums leading-none mt-1">
                    {order.deliveryCode}
                  </div>
                  <p className="text-[10px] lg:text-[11.5px] text-ink/55 mt-1.5 lg:mt-2 leading-snug">
                    {t("showQrAtCounter")}
                  </p>
                </div>
              </div>
            ) : (
              <div className="rounded-2xl bg-white border-2 border-mag/25 p-3 lg:p-4">
                <div className="text-[10px] lg:text-[11px] uppercase tracking-wider text-mag font-bold">
                  {t("confirmationCodeLabel")}
                </div>
                <div className="mt-2 inline-flex gap-1.5">
                  {order.deliveryCode!.split("").map((ch, i) => (
                    <b
                      key={i}
                      className="inline-flex items-center justify-center w-8 h-10 lg:w-[38px] lg:h-[46px] rounded-[9px] lg:rounded-[10px] bg-white border-2 border-mag/30 text-[20px] lg:text-[24px] font-black tabular-nums text-mag"
                    >
                      {ch}
                    </b>
                  ))}
                </div>
                <p className="text-[10px] lg:text-[11.5px] text-ink/55 mt-2 lg:mt-2.5 leading-snug">
                  {t("showCodeAtDoor")}
                </p>
              </div>
            )}

            <dl className="mt-3 lg:mt-4 space-y-1.5 lg:space-y-2 text-[11px] lg:text-[12px]">
              {isPickup ? (
                <>
                  {(order.pickpointName || order.pickpointAddress) && (
                    <DetailRow
                      label={t("location")}
                      value={[order.pickpointName, order.pickpointAddress].filter(Boolean).join(" · ")}
                    />
                  )}
                  {order.estimatedDeliveryDate && (
                    <DetailRow label={t("holdUntil")} value={formatDate(order.estimatedDeliveryDate)} />
                  )}
                </>
              ) : (
                <>
                  {order.deliveryAddress && <DetailRow label={t("deliverTo")} value={order.deliveryAddress} />}
                  {order.preferredTimeWindow && <DetailRow label={t("timeWindow")} value={order.preferredTimeWindow} />}
                  {order.courierName && <DetailRow label={t("courierLabel")} value={order.courierName} />}
                  {order.trackingNumber && <DetailRow label={t("trackingLabel")} value={order.trackingNumber} mono />}
                </>
              )}
            </dl>
          </aside>
        )}

        {/* Delivered summary — handover address */}
        {kind === "delivered" && deliveryAddress && (
          <aside className="md:col-span-5 md:border-l border-ink/8 px-4 lg:px-6 py-3 lg:py-4">
            <dl className="space-y-1.5 lg:space-y-2 text-[11px] lg:text-[12px]">
              <DetailRow label={t("deliverTo")} value={deliveryAddress} />
            </dl>
          </aside>
        )}
      </div>

      {/* Footer actions */}
      <footer className="flex flex-wrap items-center gap-2 lg:gap-3 px-4 lg:px-6 py-3 lg:py-4 bg-soft/60 border-t border-ink/8">
        <span className="text-[12px] text-ink/55">{t("itemCount", { count: order.items.length })}</span>
        <div className="ml-auto flex gap-2">
          {kind === "stepper" && (
            <>
              <Link
                href={`/orders/${order.id}`}
                className="h-9 px-4 rounded-full bg-mag text-white text-[12px] font-bold hover:bg-maglo inline-flex items-center"
              >
                {t("track")}
              </Link>
              <button
                disabled
                className="h-9 px-4 rounded-full border-2 border-ink/15 text-[12px] font-bold text-ink/40 cursor-not-allowed"
              >
                {t("invoice")}
              </button>
              <button
                disabled
                className="h-9 px-4 rounded-full border-2 border-ink/15 text-[12px] font-bold text-ink/40 cursor-not-allowed"
              >
                {t("help")}
              </button>
            </>
          )}
          {kind === "delivered" && (
            <>
              {canBuyAgain && (
                <Link
                  href={`/products/${firstItem!.slug}`}
                  className="h-9 px-4 rounded-full bg-mag text-white text-[12px] font-bold hover:bg-maglo inline-flex items-center"
                >
                  {t("buyAgain")}
                </Link>
              )}
              <Link
                href="/profile/returns"
                className="h-9 px-4 rounded-full border-2 border-ink/15 text-[12px] font-bold inline-flex items-center"
              >
                {t("return")}
              </Link>
              <button
                disabled
                className="h-9 px-4 rounded-full border-2 border-ink/15 text-[12px] font-bold text-ink/40 cursor-not-allowed"
              >
                {t("invoice")}
              </button>
            </>
          )}
          {kind === "exception" && (
            <>
              <Link
                href={`/orders/${order.id}`}
                className="h-9 px-4 rounded-full border-2 border-ink/15 text-[12px] font-bold inline-flex items-center"
              >
                {t("viewDetails")}
              </Link>
              {canBuyAgain && (
                <Link
                  href={`/products/${firstItem!.slug}`}
                  className="h-9 px-4 rounded-full bg-mag text-white text-[12px] font-bold hover:bg-maglo inline-flex items-center"
                >
                  {t("buyAgain")}
                </Link>
              )}
            </>
          )}
        </div>
      </footer>
    </article>
  );
}
