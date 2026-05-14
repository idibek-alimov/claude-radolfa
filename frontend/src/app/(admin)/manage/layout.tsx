export default function ManageLayout({ children }: { children: React.ReactNode }) {
  return <div className="p-6 lg:p-8 flex flex-col flex-1 min-h-0">{children}</div>;
}
