export interface RecentOrder {
  orderId: number;
  userPhone: string;
  totalAmount: number;
  status: string;
  createdAt: string; // ISO instant
}

export interface AdminOrderSummary {
  totalOrders: number;
  todayOrders: number;
  revenueToday: number;
  revenueThisMonth: number;
  recentOrders: RecentOrder[];
}
