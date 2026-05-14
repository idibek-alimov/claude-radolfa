import type { LucideIcon } from "lucide-react";

interface StatCardProps {
  icon: LucideIcon;
  label: string;
  value: number | undefined;
  iconBg: string;
  iconColor: string;
}

export function StatCard({ icon: Icon, label, value, iconBg, iconColor }: StatCardProps) {
  return (
    <div className="bg-card rounded-xl border shadow-sm p-5 flex items-center gap-4 hover:shadow-md hover:-translate-y-0.5 transition-all duration-200 cursor-default">
      <div className={`flex h-11 w-11 shrink-0 items-center justify-center rounded-full ${iconBg}`}>
        <Icon className={`h-5 w-5 ${iconColor}`} />
      </div>
      <div className="min-w-0">
        <p className="text-xs text-muted-foreground font-medium truncate">{label}</p>
        {value !== undefined ? (
          <p className="text-2xl font-bold tabular-nums leading-tight">
            {value.toLocaleString()}
          </p>
        ) : (
          <div className="mt-1 h-7 w-16 rounded bg-muted animate-pulse" />
        )}
      </div>
    </div>
  );
}
