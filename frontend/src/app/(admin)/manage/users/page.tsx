"use client";

import { useSearchParams, useRouter, usePathname } from "next/navigation";
import { Tabs, TabsList, TabsTrigger, TabsContent } from "@/shared/ui/tabs";
import { UserManagementTable } from "@/features/user-management";
import type { UserTableVariant } from "@/features/user-management";

const TABS: { value: UserTableVariant; label: string }[] = [
  { value: "customers", label: "Customers" },
  { value: "managers", label: "Managers" },
  { value: "couriers", label: "Couriers" },
  { value: "pickpoint-staff", label: "Pickpoint Staff" },
];

export default function UsersPage() {
  const searchParams = useSearchParams();
  const router = useRouter();
  const pathname = usePathname();

  const activeTab = (searchParams.get("tab") as UserTableVariant) ?? "customers";

  function handleTabChange(tab: string) {
    const params = new URLSearchParams(searchParams.toString());
    params.set("tab", tab);
    router.replace(`${pathname}?${params.toString()}`);
  }

  return (
    <div className="flex flex-col flex-1 gap-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">Users</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          View and manage registered users, roles, and account status.
        </p>
      </div>
      <Tabs value={activeTab} onValueChange={handleTabChange} className="flex flex-col flex-1 min-h-0">
        <TabsList className="w-fit">
          {TABS.map((t) => (
            <TabsTrigger key={t.value} value={t.value}>
              {t.label}
            </TabsTrigger>
          ))}
        </TabsList>
        {TABS.map((t) => (
          <TabsContent key={t.value} value={t.value} className="flex flex-col flex-1 min-h-0 mt-4">
            {activeTab === t.value && <UserManagementTable variant={t.value} />}
          </TabsContent>
        ))}
      </Tabs>
    </div>
  );
}
