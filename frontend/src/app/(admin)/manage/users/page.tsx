"use client";

import { UserManagementTable } from "@/features/user-management";

export default function UsersPage() {
  return (
    <div className="flex flex-col flex-1 gap-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">Users</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          View and manage registered users, roles, and account status.
        </p>
      </div>
      <UserManagementTable />
    </div>
  );
}
