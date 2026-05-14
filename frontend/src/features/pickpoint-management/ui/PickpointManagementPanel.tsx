"use client";

import { useState } from "react";
import { MapPin, Pencil, Plus, Search, Clock } from "lucide-react";
import { useAdminPickpoints, type Pickpoint } from "@/entities/pickpoint";
import { useDebounce, getErrorMessage } from "@/shared/lib";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";
import { Skeleton } from "@/shared/ui/skeleton";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/shared/ui/table";
import { PickpointFormDialog } from "./PickpointFormDialog";
import { PickpointHoursDialog } from "./PickpointHoursDialog";

export function PickpointManagementPanel() {
  const [search, setSearch] = useState("");
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editingPickpoint, setEditingPickpoint] = useState<Pickpoint | null>(null);
  const [hoursDialogOpen, setHoursDialogOpen] = useState(false);
  const [hoursPickpoint, setHoursPickpoint] = useState<Pickpoint | null>(null);

  const debouncedSearch = useDebounce(search, 300);
  const { data = [], isLoading, isError, error } = useAdminPickpoints(debouncedSearch);

  function openCreateDialog() {
    setEditingPickpoint(null);
    setDialogOpen(true);
  }

  function openEditDialog(pickpoint: Pickpoint) {
    setEditingPickpoint(pickpoint);
    setDialogOpen(true);
  }

  return (
    <div className="flex flex-col flex-1 min-h-0">
      {/* Search + Add */}
      <div className="flex items-center gap-3 mb-4">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
          <Input
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search by name or address…"
            className="pl-9"
          />
        </div>
        <Button onClick={openCreateDialog}>
          <Plus className="h-4 w-4 mr-2" />
          Add Pickpoint
        </Button>
      </div>

      {/* Table card */}
      <div className="flex-1 min-h-0 overflow-auto bg-card rounded-xl border shadow-sm">
        {isLoading ? (
          <div className="p-6 space-y-4">
            {Array.from({ length: 4 }).map((_, i) => (
              <Skeleton key={i} className="h-12 w-full" />
            ))}
          </div>
        ) : isError ? (
          <div className="p-6 text-sm text-destructive">{getErrorMessage(error)}</div>
        ) : data.length === 0 ? (
          <div className="border border-dashed rounded-xl p-12 m-6 flex flex-col items-center text-center gap-3">
            <MapPin className="h-10 w-10 text-muted-foreground/40" />
            {debouncedSearch ? (
              <p className="text-sm text-muted-foreground">
                No pickpoints match your search.
              </p>
            ) : (
              <>
                <p className="text-sm text-muted-foreground">No pickpoints yet.</p>
                <Button variant="outline" size="sm" onClick={openCreateDialog}>
                  <Plus className="h-4 w-4 mr-2" />
                  Add Pickpoint
                </Button>
              </>
            )}
          </div>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead className="pl-4">ID</TableHead>
                <TableHead>Name</TableHead>
                <TableHead>Address</TableHead>
                <TableHead>Status</TableHead>
                <TableHead className="text-right pr-4">Actions</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {data.map((pickpoint) => (
                <TableRow key={pickpoint.id}>
                  <TableCell className="pl-4 tabular-nums text-sm text-muted-foreground">
                    {pickpoint.id}
                  </TableCell>
                  <TableCell className="font-medium">{pickpoint.name}</TableCell>
                  <TableCell className="text-sm text-muted-foreground max-w-md truncate">
                    {pickpoint.address}
                  </TableCell>
                  <TableCell>
                    <div className="flex flex-col items-start gap-0.5">
                      <span
                        className={`inline-flex items-center gap-1.5 px-2 py-0.5 rounded-full text-xs font-medium border ${
                          pickpoint.active
                            ? "bg-green-50 text-green-700 border-green-200"
                            : "bg-zinc-100 text-zinc-600 border-zinc-200"
                        }`}
                      >
                        <span
                          className={`h-1.5 w-1.5 rounded-full ${
                            pickpoint.active ? "bg-green-500" : "bg-zinc-400"
                          }`}
                        />
                        {pickpoint.active ? "Active" : "Inactive"}
                      </span>
                      {pickpoint.active && (
                        <span
                          className={`flex items-center gap-1 text-xs ${
                            pickpoint.isOpenNow ? "text-green-600" : "text-zinc-400"
                          }`}
                        >
                          <span
                            className={`h-1.5 w-1.5 rounded-full ${
                              pickpoint.isOpenNow ? "bg-green-500" : "bg-zinc-300"
                            }`}
                          />
                          {pickpoint.isOpenNow ? "Open now" : "Closed now"}
                        </span>
                      )}
                    </div>
                  </TableCell>
                  <TableCell className="text-right pr-4">
                    <div className="flex items-center justify-end gap-1">
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => {
                          setHoursPickpoint(pickpoint);
                          setHoursDialogOpen(true);
                        }}
                      >
                        <Clock className="h-4 w-4 mr-1" />
                        Hours
                      </Button>
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => openEditDialog(pickpoint)}
                      >
                        <Pencil className="h-4 w-4 mr-1" />
                        Edit
                      </Button>
                    </div>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </div>

      <PickpointFormDialog
        open={dialogOpen}
        onOpenChange={(open) => {
          setDialogOpen(open);
          if (!open) setEditingPickpoint(null);
        }}
        initialData={editingPickpoint ?? undefined}
      />

      {hoursPickpoint && (
        <PickpointHoursDialog
          open={hoursDialogOpen}
          onOpenChange={(open) => {
            setHoursDialogOpen(open);
            if (!open) setHoursPickpoint(null);
          }}
          pickpoint={hoursPickpoint}
        />
      )}
    </div>
  );
}
