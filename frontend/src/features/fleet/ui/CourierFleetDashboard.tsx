"use client";

import { useState } from "react";
import { Bike, Car, Truck, Users } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/shared/ui/card";
import { Button } from "@/shared/ui/button";
import { Skeleton } from "@/shared/ui/skeleton";
import { useFleetSummary } from "@/features/fleet/api";
import type { CourierFleetEntry } from "@/entities/user";
import { BulkReassignDialog } from "./BulkReassignDialog";

const VEHICLE_ICONS: Record<string, React.ReactNode> = {
  BICYCLE:    <Bike className="h-3.5 w-3.5" />,
  MOTORCYCLE: <Bike className="h-3.5 w-3.5" />,
  CAR:        <Car className="h-3.5 w-3.5" />,
  VAN:        <Truck className="h-3.5 w-3.5" />,
};

function StatCard({ label, value, sub }: { label: string; value: number; sub?: string }) {
  return (
    <div className="rounded-xl border bg-card p-5 shadow-sm">
      <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wide">{label}</p>
      <p className="text-2xl font-bold tabular-nums mt-1">{value}</p>
      {sub && <p className="text-xs text-muted-foreground mt-0.5">{sub}</p>}
    </div>
  );
}

function FleetRow({ entry }: { entry: CourierFleetEntry }) {
  return (
    <tr className="border-b last:border-0 hover:bg-muted/30 transition-colors">
      <td className="py-3 px-4 text-sm font-medium">{entry.name}</td>
      <td className="py-3 px-4">
        {entry.vehicleType ? (
          <span className="inline-flex items-center gap-1 rounded-full bg-muted px-2 py-0.5 text-xs font-medium">
            {VEHICLE_ICONS[entry.vehicleType]}
            {entry.vehicleType}
          </span>
        ) : (
          <span className="text-muted-foreground text-xs">—</span>
        )}
      </td>
      <td className="py-3 px-4 text-sm text-muted-foreground tabular-nums">
        {entry.maxPayloadKg != null ? `${entry.maxPayloadKg} kg` : "—"}
      </td>
      <td className="py-3 px-4 text-sm tabular-nums text-green-600 font-medium">{entry.deliveredToday}</td>
      <td className="py-3 px-4 text-sm tabular-nums text-blue-600 font-medium">{entry.inTransit}</td>
      <td className="py-3 px-4 text-sm tabular-nums text-amber-600 font-medium">{entry.attempted}</td>
    </tr>
  );
}

export function CourierFleetDashboard() {
  const { data: fleet = [], isLoading } = useFleetSummary();
  const [reassignOpen, setReassignOpen] = useState(false);

  const totalCouriers   = fleet.length;
  const totalInTransit  = fleet.reduce((s, c) => s + c.inTransit, 0);
  const totalDelivered  = fleet.reduce((s, c) => s + c.deliveredToday, 0);
  const totalAttempted  = fleet.reduce((s, c) => s + c.attempted, 0);

  const sorted = [...fleet].sort((a, b) => b.inTransit - a.inTransit);

  return (
    <div className="flex flex-col gap-6">
      {/* Header */}
      <div className="flex items-center justify-between flex-wrap gap-3">
        <h1 className="text-2xl font-semibold">Courier Fleet</h1>
        <Button onClick={() => setReassignOpen(true)}>
          <Truck className="h-4 w-4 mr-2" />
          Reassign Orders
        </Button>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard label="Couriers" value={totalCouriers} />
        <StatCard label="In Transit" value={totalInTransit} sub="active deliveries" />
        <StatCard label="Delivered Today" value={totalDelivered} />
        <StatCard label="Attempted" value={totalAttempted} sub="need attention" />
      </div>

      {/* Table */}
      <Card>
        <CardHeader className="pb-3">
          <CardTitle className="text-base">Fleet Status</CardTitle>
        </CardHeader>
        <CardContent className="p-0">
          {isLoading ? (
            <div className="p-5 space-y-3">
              {Array.from({ length: 4 }).map((_, i) => (
                <Skeleton key={i} className="h-10 w-full" />
              ))}
            </div>
          ) : fleet.length === 0 ? (
            <div className="border border-dashed rounded-xl m-5 p-12 flex flex-col items-center gap-3">
              <Users className="h-10 w-10 text-muted-foreground/40" />
              <p className="text-sm text-muted-foreground">
                No couriers yet — create one in{" "}
                <a href="/manage/users" className="text-primary underline underline-offset-2">
                  Users
                </a>
                .
              </p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left">
                <thead>
                  <tr className="border-b bg-muted/30">
                    <th className="py-2.5 px-4 text-xs font-semibold text-muted-foreground uppercase tracking-wide">Name</th>
                    <th className="py-2.5 px-4 text-xs font-semibold text-muted-foreground uppercase tracking-wide">Vehicle</th>
                    <th className="py-2.5 px-4 text-xs font-semibold text-muted-foreground uppercase tracking-wide">Max Load</th>
                    <th className="py-2.5 px-4 text-xs font-semibold text-green-700 uppercase tracking-wide">Delivered</th>
                    <th className="py-2.5 px-4 text-xs font-semibold text-blue-700 uppercase tracking-wide">In Transit</th>
                    <th className="py-2.5 px-4 text-xs font-semibold text-amber-700 uppercase tracking-wide">Attempted</th>
                  </tr>
                </thead>
                <tbody>
                  {sorted.map((entry) => (
                    <FleetRow key={entry.courierId} entry={entry} />
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </CardContent>
      </Card>

      <BulkReassignDialog open={reassignOpen} onClose={() => setReassignOpen(false)} />
    </div>
  );
}
