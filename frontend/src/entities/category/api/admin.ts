import { apiClient } from "@/shared/api";
import type { CategoryTree } from "@/entities/product/model/types";

export async function createCategory(
  name: string,
  parentId: number | null
): Promise<CategoryTree> {
  const { data } = await apiClient.post<CategoryTree>("/api/v1/admin/categories", {
    name,
    parentId,
  });
  return data;
}

export async function updateCategory(
  id: number,
  body: { name: string; parentId: number | null }
): Promise<void> {
  await apiClient.patch(`/api/v1/admin/categories/${id}`, body);
}

export async function deleteCategory(categoryId: number): Promise<void> {
  await apiClient.delete(`/api/v1/admin/categories/${categoryId}`);
}
