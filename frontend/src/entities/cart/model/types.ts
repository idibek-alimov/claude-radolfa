export interface CartItem {
  skuId: number;
  listingSlug: string;
  productName: string;
  sizeLabel: string;
  imageUrl: string | null;
  priceSnapshot: number;
  quantity: number;
  itemSubtotal: number;
}

export interface Cart {
  userId: number;
  items: CartItem[];
  subtotal: number;
  itemCount: number;
}
