export default function ManageLayout({ children }: { children: React.ReactNode }) {
  return <div className="p-6 lg:p-8 flex flex-col flex-1">{children}</div>;
}
