import type { CategoryTree } from "@/entities/product/model/types";

export interface FlatCategory {
  id: number;
  name: string;
  depth: number;
}

export function flattenTree(nodes: CategoryTree[], depth = 0): FlatCategory[] {
  return nodes.flatMap((node) => [
    { id: node.id, name: node.name, depth },
    ...flattenTree(node.children, depth + 1),
  ]);
}
