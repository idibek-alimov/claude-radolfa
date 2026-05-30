import { apiClient } from "@/shared/api";

export async function uploadProductImage(file: File): Promise<{ url: string }> {
  const form = new FormData();
  form.append("image", file);
  const { data } = await apiClient.post<{ url: string }>(
    "/api/v1/admin/images/upload",
    form,
    { headers: { "Content-Type": "multipart/form-data" } }
  );
  return data;
}
