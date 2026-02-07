import apiClient from "@/shared/api/axios";
import { Product, CreateProductRequest, UpdateProductRequest, PaginatedProducts, ImageUploadResponse } from "./types";

const MAX_IMAGE_SIZE = 5 * 1024 * 1024; // 5MB

export async function getProducts(page = 1, limit = 12, search?: string): Promise<PaginatedProducts> {
    const params = new URLSearchParams({ page: String(page), limit: String(limit) });
    if (search) params.set("search", search);
    const response = await apiClient.get<PaginatedProducts>(`/api/v1/products?${params}`);
    return response.data;
}

export async function createProduct(data: CreateProductRequest): Promise<Product> {
    const response = await apiClient.post<Product>("/api/v1/products", data);
    return response.data;
}

export async function updateProduct(erpId: string, data: UpdateProductRequest): Promise<Product> {
    const response = await apiClient.put<Product>(`/api/v1/products/${erpId}`, data);
    return response.data;
}

export async function deleteProduct(erpId: string): Promise<void> {
    await apiClient.delete(`/api/v1/products/${erpId}`);
}

export async function uploadProductImage(erpId: string, file: File): Promise<ImageUploadResponse> {
    if (!file.type.startsWith("image/")) {
        throw new Error("File must be an image");
    }
    if (file.size > MAX_IMAGE_SIZE) {
        throw new Error("Image must be under 5MB");
    }
    const form = new FormData();
    form.append("image", file);
    const response = await apiClient.post<ImageUploadResponse>(
        `/api/v1/products/${erpId}/images`,
        form,
        { headers: { "Content-Type": "multipart/form-data" } }
    );
    return response.data;
}
