import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import apiClient from "@radolfa/shared/api/axios";
import type { NotificationPreferences } from "@radolfa/shared/user";

const NOTIFICATION_PREFS_KEY = ["notification-prefs"] as const;

/** GET /api/v1/users/notification-prefs */
export async function getNotificationPrefs(): Promise<NotificationPreferences> {
  const response = await apiClient.get<NotificationPreferences>(
    "/api/v1/users/notification-prefs"
  );
  return response.data;
}

/** PUT /api/v1/users/notification-prefs */
export async function updateNotificationPrefs(
  prefs: NotificationPreferences
): Promise<NotificationPreferences> {
  const response = await apiClient.put<NotificationPreferences>(
    "/api/v1/users/notification-prefs",
    prefs
  );
  return response.data;
}

export function useNotificationPrefs() {
  return useQuery({
    queryKey: NOTIFICATION_PREFS_KEY,
    queryFn: getNotificationPrefs,
  });
}

/** Optimistic update: flips the switch immediately, rolls back on error. */
export function useUpdateNotificationPrefs() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: updateNotificationPrefs,
    onMutate: async (next) => {
      await queryClient.cancelQueries({ queryKey: NOTIFICATION_PREFS_KEY });
      const previous = queryClient.getQueryData<NotificationPreferences>(NOTIFICATION_PREFS_KEY);
      queryClient.setQueryData(NOTIFICATION_PREFS_KEY, next);
      return { previous };
    },
    onError: (_err, _next, context) => {
      if (context?.previous) {
        queryClient.setQueryData(NOTIFICATION_PREFS_KEY, context.previous);
      }
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: NOTIFICATION_PREFS_KEY });
    },
  });
}
