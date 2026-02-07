import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  getProducts,
  createProduct,
  updateProduct,
  deleteProduct,
  uploadProductImage,
} from "../api";
import type {
  Product,
  CreateProductRequest,
  UpdateProductRequest,
} from "../types";

const PRODUCTS_KEY = ["products"] as const;

export function useProducts(page = 1, limit = 100) {
  return useQuery({
    queryKey: [...PRODUCTS_KEY, page, limit],
    queryFn: () => getProducts(page, limit),
    staleTime: 30_000, // 30s — background revalidation, no spinner on mount
  });
}

export function useCreateProduct() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateProductRequest) => createProduct(data),
    onSuccess: () => qc.invalidateQueries({ queryKey: PRODUCTS_KEY }),
  });
}

export function useUpdateProduct() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ erpId, data }: { erpId: string; data: UpdateProductRequest }) =>
      updateProduct(erpId, data),
    onSuccess: () => qc.invalidateQueries({ queryKey: PRODUCTS_KEY }),
  });
}

export function useDeleteProduct() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (erpId: string) => deleteProduct(erpId),
    onSuccess: () => qc.invalidateQueries({ queryKey: PRODUCTS_KEY }),
  });
}

export function useUploadProductImage() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ erpId, file }: { erpId: string; file: File }) =>
      uploadProductImage(erpId, file),
    onSuccess: () => qc.invalidateQueries({ queryKey: PRODUCTS_KEY }),
  });
}
