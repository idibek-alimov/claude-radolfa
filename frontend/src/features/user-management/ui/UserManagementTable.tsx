"use client";

import { useState, useCallback, useRef } from "react";
import {
  useQuery,
  useMutation,
  useQueryClient,
  keepPreviousData,
} from "@tanstack/react-query";
import { useAuth } from "@/features/auth";
import { fetchUsers, toggleUserStatus } from "../api";
import type { UserDto } from "../types";

const ROLE_RANK: Record<string, number> = { USER: 0, MANAGER: 1, SYSTEM: 2 };

function canToggleStatus(callerRole: string, callerId: number | null, target: UserDto): boolean {
  if (callerId === target.id) return false;
  if (target.role === "SYSTEM") return false;
  return (ROLE_RANK[callerRole] ?? 0) > (ROLE_RANK[target.role] ?? 0);
}
import {
  Table,
  TableHeader,
  TableBody,
  TableHead,
  TableRow,
  TableCell,
} from "@/shared/ui/table";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";
import { Badge } from "@/shared/ui/badge";
import { Skeleton } from "@/shared/ui/skeleton";
import { Search, ShieldCheck, ShieldOff, ChevronLeft, ChevronRight } from "lucide-react";
import { toast } from "sonner";

export function UserManagementTable() {
  const { user: currentUser } = useAuth();
  const queryClient = useQueryClient();

  const [page, setPage] = useState(1);
  const [searchQuery, setSearchQuery] = useState("");
  const [debouncedSearch, setDebouncedSearch] = useState("");
  const debounceRef = useRef<NodeJS.Timeout>();

  const handleSearchChange = useCallback((value: string) => {
    setSearchQuery(value);
    clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(() => {
      setDebouncedSearch(value.trim());
      setPage(1);
    }, 300);
  }, []);

  const { data, isLoading } = useQuery({
    queryKey: ["admin-users", page, debouncedSearch],
    queryFn: () => fetchUsers(debouncedSearch, page),
    placeholderData: keepPreviousData,
  });

  const users = data?.items ?? [];

  const toggleMutation = useMutation({
    mutationFn: toggleUserStatus,
    onSuccess: (updatedUser) => {
      queryClient.invalidateQueries({ queryKey: ["admin-users"] });
      toast.success(
        updatedUser.enabled
          ? `${updatedUser.phone} has been unblocked`
          : `${updatedUser.phone} has been blocked`
      );
    },
    onError: (err: any) => {
      toast.error(err.response?.data?.message || "Failed to update user status");
    },
  });

  const handleToggleStatus = (user: UserDto) => {
    toggleMutation.mutate({ userId: user.id, enabled: !user.enabled });
  };

  const roleBadgeVariant = (role: string) => {
    switch (role) {
      case "MANAGER":
        return "default" as const;
      case "SYSTEM":
        return "destructive" as const;
      default:
        return "secondary" as const;
    }
  };

  return (
    <div>
      {/* Search */}
      <div className="mb-4 relative max-w-sm">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
        <Input
          value={searchQuery}
          onChange={(e) => handleSearchChange(e.target.value)}
          placeholder="Search by phone or name..."
          className="pl-9"
        />
      </div>

      {/* Table */}
      <div className="bg-card rounded-xl border shadow-sm">
        {isLoading && users.length === 0 ? (
          <div className="p-6 space-y-4">
            {Array.from({ length: 5 }).map((_, i) => (
              <Skeleton key={i} className="h-12 w-full" />
            ))}
          </div>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead className="pl-4">ID</TableHead>
                <TableHead>Phone</TableHead>
                <TableHead>Name</TableHead>
                <TableHead>Email</TableHead>
                <TableHead>Role</TableHead>
                <TableHead>Points</TableHead>
                <TableHead>Status</TableHead>
                <TableHead className="text-right pr-4">Actions</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {users.map((user) => (
                <TableRow key={user.id} className={!user.enabled ? "opacity-60" : ""}>
                  <TableCell className="pl-4 font-mono text-xs">
                    {user.id}
                  </TableCell>
                  <TableCell className="font-medium text-sm">
                    {user.phone}
                  </TableCell>
                  <TableCell className="text-sm">
                    {user.name || <span className="text-muted-foreground">—</span>}
                  </TableCell>
                  <TableCell className="text-sm">
                    {user.email || <span className="text-muted-foreground">—</span>}
                  </TableCell>
                  <TableCell>
                    <Badge variant={roleBadgeVariant(user.role)}>
                      {user.role}
                    </Badge>
                  </TableCell>
                  <TableCell className="text-sm">{user.loyaltyPoints}</TableCell>
                  <TableCell>
                    {user.enabled ? (
                      <Badge variant="success">Active</Badge>
                    ) : (
                      <Badge variant="destructive">Blocked</Badge>
                    )}
                  </TableCell>
                  <TableCell className="text-right pr-4">
                    {canToggleStatus(currentUser?.role ?? "", currentUser?.id ?? null, user) && (
                      <Button
                        variant={user.enabled ? "outline" : "default"}
                        size="sm"
                        className="gap-1.5"
                        disabled={toggleMutation.isPending}
                        onClick={() => handleToggleStatus(user)}
                      >
                        {user.enabled ? (
                          <>
                            <ShieldOff className="h-3.5 w-3.5" />
                            Block
                          </>
                        ) : (
                          <>
                            <ShieldCheck className="h-3.5 w-3.5" />
                            Unblock
                          </>
                        )}
                      </Button>
                    )}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}

        {users.length === 0 && !isLoading && (
          <div className="p-12 text-center text-muted-foreground">
            {debouncedSearch
              ? `No users matching "${debouncedSearch}"`
              : "No users found."}
          </div>
        )}
      </div>

      {/* Pagination */}
      {data && data.totalElements > 0 && (
        <div className="flex items-center justify-between mt-4">
          <p className="text-sm text-muted-foreground">
            {data.totalElements} user{data.totalElements !== 1 ? "s" : ""} total
          </p>
          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              size="sm"
              disabled={page <= 1}
              onClick={() => setPage((p) => p - 1)}
            >
              <ChevronLeft className="h-4 w-4" />
            </Button>
            <span className="text-sm text-muted-foreground">Page {page}</span>
            <Button
              variant="outline"
              size="sm"
              disabled={!data.hasMore}
              onClick={() => setPage((p) => p + 1)}
            >
              <ChevronRight className="h-4 w-4" />
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}
