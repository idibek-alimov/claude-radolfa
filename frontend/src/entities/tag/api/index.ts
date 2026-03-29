import { apiClient } from "@/shared/api";
import type { Tag } from "../model/types";

/** GET /api/v1/tags — public, no auth required */
export const fetchTags = (): Promise<Tag[]> =>
  apiClient.get("/api/v1/tags").then((r) => r.data);
