"use client";

import { useState, useCallback, useRef, useEffect } from "react";
import {
  useQuery,
  useMutation,
  useQueryClient,
  keepPreviousData,
} from "@tanstack/react-query";
import { useAuth } from "@/features/auth";
import { fetchUsers, toggleUserStatus } from "../api";
import type { UserDto } from "../types";

const ROLE_RANK: Record<string, number> = { USER: 0, MANAGER: 1, ADMIN: 2 };

function canToggleStatus(callerRole: string, callerId: number | null, target: UserDto): boolean {
  if (callerId === target.id) return false;
  if (target.role === "ADMIN") return false;
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
import { getErrorMessage, useDynamicPageSize } from "@/shared/lib";
import { Search, ShieldCheck, ShieldOff, ChevronLeft, ChevronRight, Lock } from "lucide-react";
import { ManageUserDialog } from "./ManageUserDialog";
import { toast } from "sonner";
import { useTranslations } from "next-intl";

export function UserManagementTable() {
  const t = useTranslations("userManagement");
  const { user: currentUser } = useAuth();
  const queryClient = useQueryClient();

  const [page, setPage] = useState(1);
  const [managingUser, setManagingUser] = useState<UserDto | null>(null);
  const [searchQuery, setSearchQuery] = useState("");
  const [debouncedSearch, setDebouncedSearch] = useState("");
  const debounceRef = useRef<NodeJS.Timeout | undefined>(undefined);
  const cardRef = useRef<HTMLDivElement>(null);
  const pageSize = useDynamicPageSize(cardRef, 49);

  useEffect(() => { setPage(1); }, [pageSize]);

  const handleSearchChange = useCallback((value: string) => {
    setSearchQuery(value);
    clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(() => {
      setDebouncedSearch(value.trim());
      setPage(1);
    }, 300);
  }, []);

  const { data, isLoading } = useQuery({
    queryKey: ["admin-users", page, debouncedSearch, pageSize],
    queryFn: () => fetchUsers(debouncedSearch, page, pageSize),
    placeholderData: keepPreviousData,
  });

  const users = data?.content ?? [];

  const toggleMutation = useMutation({
    mutationFn: toggleUserStatus,
    onSuccess: (updatedUser) => {
      queryClient.invalidateQueries({ queryKey: ["admin-users"] });
      toast.success(
        updatedUser.enabled
          ? t("unblocked", { phone: updatedUser.phone })
          : t("blocked", { phone: updatedUser.phone })
      );
    },
    onError: (err: unknown) => {
      toast.error(getErrorMessage(err, t("failedToUpdateStatus")));
    },
  });

  const handleToggleStatus = (user: UserDto) => {
    toggleMutation.mutate({ userId: user.id, enabled: !user.enabled });
  };

  const roleBadgeVariant = (role: string) => {
    switch (role) {
      case "MANAGER":
        return "default" as const;
      case "ADMIN":
        return "destructive" as const;
      default:
        return "secondary" as const;
    }
  };

  return (
    <div className="flex flex-col flex-1 min-h-0">
      {/* Search */}
      <div className="mb-4 relative max-w-sm">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
        <Input
          value={searchQuery}
          onChange={(e) => handleSearchChange(e.target.value)}
          placeholder={t("searchPlaceholder")}
          className="pl-9"
        />
      </div>

      {/* Table */}
      <div ref={cardRef} className="flex-1 min-h-0 overflow-auto bg-card rounded-xl border shadow-sm">
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
                <TableHead className="pl-4">{t("tableId")}</TableHead>
                <TableHead>{t("tablePhone")}</TableHead>
                <TableHead>{t("tableName")}</TableHead>
                <TableHead>{t("tableEmail")}</TableHead>
                <TableHead>{t("tableRole")}</TableHead>
                <TableHead>{t("tablePoints")}</TableHead>
                <TableHead>{t("tableStatus")}</TableHead>
                <TableHead className="text-right pr-4">{t("tableActions")}</TableHead>
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
                  <TableCell className="text-sm">
                    <div className="flex flex-col gap-0.5">
                      <span>{user.loyalty.points} pts</span>
                      {user.loyalty.tier && (
                        <span className="text-xs text-muted-foreground flex items-center gap-1">
                          {user.loyalty.permanent && <Lock className="h-3 w-3 text-amber-500" />}
                          {user.loyalty.tier.name}
                        </span>
                      )}
                    </div>
                  </TableCell>
                  <TableCell>
                    {user.enabled ? (
                      <Badge variant="success">{t("statusActive")}</Badge>
                    ) : (
                      <Badge variant="destructive">{t("statusBlocked")}</Badge>
                    )}
                  </TableCell>
                  <TableCell className="text-right pr-4">
                    <div className="flex items-center justify-end gap-2">
                      {currentUser && canToggleStatus(currentUser.role, currentUser.id, user) && (
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
                              {t("actionBlock")}
                            </>
                          ) : (
                            <>
                              <ShieldCheck className="h-3.5 w-3.5" />
                              {t("actionUnblock")}
                            </>
                          )}
                        </Button>
                      )}
                      {currentUser &&
                        (ROLE_RANK[currentUser.role] ?? 0) >= ROLE_RANK["MANAGER"] &&
                        currentUser.id !== user.id && (
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => setManagingUser(user)}
                          >
                            Manage
                          </Button>
                        )}
                    </div>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}

        {users.length === 0 && !isLoading && (
          <div className="p-12 text-center text-muted-foreground">
            {debouncedSearch
              ? t("noUsersMatching", { search: debouncedSearch })
              : t("noUsersFound")}
          </div>
        )}
      </div>

      {managingUser && currentUser && (
        <ManageUserDialog
          open={managingUser !== null}
          onClose={() => setManagingUser(null)}
          target={managingUser}
          callerRole={currentUser.role}
          callerId={currentUser.id}
        />
      )}

      {/* Pagination */}
      {data && data.totalElements > 0 && (
        <div className="flex items-center justify-between mt-4">
          <p className="text-sm text-muted-foreground">
            {t("usersTotal", { count: data.totalElements })}
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
            <span className="text-sm text-muted-foreground">{t("page", { page })}</span>
            <Button
              variant="outline"
              size="sm"
              disabled={data.last}
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
