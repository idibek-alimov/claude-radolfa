export interface Product {
    id: number;
    erpId: string;
    name?: string;
    price?: number;
    stock?: number;
    webDescription?: string;
    topSelling: boolean;
    images?: string[];
    lastErpSyncAt?: string;
}

export interface CreateProductRequest {
    erpId: string;
    name?: string;
    price?: number;
    stock?: number;
    webDescription?: string;
    topSelling: boolean;
    images?: string[];
}

export interface UpdateProductRequest {
    name?: string;
    price?: number;
    stock?: number;
    webDescription?: string;
    topSelling: boolean;
    images?: string[];
}

export interface PaginatedProducts {
    products: Product[];
    total: number;
    page: number;
    hasMore: boolean;
}
