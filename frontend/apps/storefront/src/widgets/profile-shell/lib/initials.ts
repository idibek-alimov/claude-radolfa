/** Avatar initials: first letters of each word in `name`, falling back to the
 *  last two digits of `phone`. Shared by `ProfileSidebar` and `ProfileMobileHeader`
 *  (extracted from the previous combined `/profile` page). */
export function getInitials(name?: string | null, phone?: string | null): string {
  if (name) {
    return name
      .split(" ")
      .map((n) => n[0])
      .join("")
      .toUpperCase()
      .slice(0, 2);
  }
  return phone?.slice(-2) ?? "?";
}
