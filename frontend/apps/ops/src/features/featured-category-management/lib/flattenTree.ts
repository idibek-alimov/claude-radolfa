import type { CategoryTree } from "@/entities/product/model/types";

export interface FlatCategory {
  id: number;
  name: string;
  depth: number;
}

/**
 * Flattens the nested category tree into a depth-annotated list, used to
 * populate the category `<Select>` (indented options) and to resolve
 * `categoryId → name` for display in the admin table.
 *
 * Local copy of `features/category-management/lib/flattenTree.ts` — FSD
 * forbids cross-feature imports at the same layer, so each feature owns
 * its own copy of this small helper.
 */
export function flattenTree(nodes: CategoryTree[], depth = 0): FlatCategory[] {
  return nodes.flatMap((node) => [
    { id: node.id, name: node.name, depth },
    ...flattenTree(node.children, depth + 1),
  ]);
}
