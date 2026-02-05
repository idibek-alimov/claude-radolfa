import apiClient from "@/shared/api/axios";
import { Product, CreateProductRequest, UpdateProductRequest, PaginatedProducts } from "./types";

export async function getProducts(page = 1, limit = 12): Promise<PaginatedProducts> {
    const response = await apiClient.get<PaginatedProducts>(`/api/v1/products?page=${page}&limit=${limit}`);
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
