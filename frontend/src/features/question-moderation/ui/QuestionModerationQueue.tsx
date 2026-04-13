"use client";

import { useEffect, useRef, useState } from "react";
import { useInfiniteQuery } from "@tanstack/react-query";
import { Inbox, MessageCircleQuestion, Search, SortAsc, SortDesc } from "lucide-react";
import { fetchAdminQuestions } from "@/entities/question";
import { QuestionModerationCard } from "./QuestionModerationCard";
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from "@/shared/ui/card";
import { Input } from "@/shared/ui/input";
import { Skeleton } from "@/shared/ui/skeleton";
import { Tabs, TabsList, TabsTrigger } from "@/shared/ui/tabs";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/shared/ui/select";
import { useDebounce } from "@/shared/lib";

type StatusFilter = "PENDING" | "PUBLISHED";
type SortOption = "createdAt_ASC" | "createdAt_DESC" | "answeredAt_DESC";

const SORT_OPTIONS: { value: SortOption; label: string; statusOnly?: StatusFilter }[] = [
  { value: "createdAt_ASC",  label: "Oldest first" },
  { value: "createdAt_DESC", label: "Newest first" },
  { value: "answeredAt_DESC", label: "Recently answered", statusOnly: "PUBLISHED" },
];

export function QuestionModerationQueue() {
  const [activeStatus, setActiveStatus] = useState<StatusFilter>("PENDING");
  const [search, setSearch]             = useState("");
  const debouncedSearch                 = useDebounce(search, 300);
  const [sort, setSort]                 = useState<SortOption>("createdAt_ASC");

  const bottomRef = useRef<HTMLDivElement>(null);

  // Reset sort to a valid option when switching tabs
  function handleTabChange(value: string) {
    setActiveStatus(value as StatusFilter);
    if (value === "PENDING" && sort === "answeredAt_DESC") {
      setSort("createdAt_ASC");
    }
  }

  const [sortBy, sortDir] = sort.split("_") as ["createdAt" | "answeredAt", "ASC" | "DESC"];

  const {
    data,
    fetchNextPage,
    hasNextPage,
    isFetchingNextPage,
    isLoading,
  } = useInfiniteQuery({
    queryKey: ["admin-questions", activeStatus, debouncedSearch, sort],
    queryFn: ({ pageParam }) =>
      fetchAdminQuestions({
        status: activeStatus,
        search: debouncedSearch.trim() || undefined,
        page: pageParam as number,
        size: 20,
        sortBy,
        sortDir,
      }),
    initialPageParam: 1,
    getNextPageParam: (lastPage) =>
      lastPage.last ? undefined : lastPage.number + 2, // number is 0-based; next 1-based page
  });

  const allQuestions = data?.pages.flatMap((p) => p.content) ?? [];
  const totalElements = data?.pages[0]?.totalElements ?? 0;

  // Infinite scroll sentinel
  useEffect(() => {
    const sentinel = bottomRef.current;
    if (!sentinel) return;
    const observer = new IntersectionObserver((entries) => {
      if (entries[0].isIntersecting && hasNextPage && !isFetchingNextPage) {
        fetchNextPage();
      }
    });
    observer.observe(sentinel);
    return () => observer.disconnect();
  }, [hasNextPage, isFetchingNextPage, fetchNextPage]);

  const availableSorts = SORT_OPTIONS.filter(
    (o) => !o.statusOnly || o.statusOnly === activeStatus
  );

  return (
    <Card>
      <CardHeader className="border-b py-4 space-y-4">
        {/* Title + count + tabs */}
        <div className="flex items-center gap-3">
          <MessageCircleQuestion className="h-4 w-4 text-muted-foreground shrink-0" />
          <CardTitle className="text-base font-semibold">Questions</CardTitle>
          {!isLoading && (
            <span className="text-sm text-muted-foreground tabular-nums">
              ({totalElements})
            </span>
          )}
          <Tabs value={activeStatus} onValueChange={handleTabChange} className="ml-auto">
            <TabsList>
              <TabsTrigger value="PENDING">Pending</TabsTrigger>
              <TabsTrigger value="PUBLISHED">Answered</TabsTrigger>
            </TabsList>
          </Tabs>
        </div>

        {/* Search + sort */}
        <div className="flex items-center gap-3">
          <div className="relative flex-1 max-w-sm">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
            <Input
              placeholder="Search questions or products…"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="pl-9"
            />
          </div>
          <Select value={sort} onValueChange={(v) => setSort(v as SortOption)}>
            <SelectTrigger className="w-48">
              <span className="flex items-center gap-2">
                {sortDir === "ASC"
                  ? <SortAsc className="h-4 w-4 text-muted-foreground" />
                  : <SortDesc className="h-4 w-4 text-muted-foreground" />}
                <SelectValue />
              </span>
            </SelectTrigger>
            <SelectContent>
              {availableSorts.map((o) => (
                <SelectItem key={o.value} value={o.value}>
                  {o.label}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
      </CardHeader>

      <CardContent className="p-0">
        {isLoading ? (
          <div className="p-5 space-y-3">
            <Skeleton className="h-24 w-full rounded-xl" />
            <Skeleton className="h-24 w-full rounded-xl" />
            <Skeleton className="h-24 w-full rounded-xl" />
          </div>
        ) : allQuestions.length === 0 ? (
          <div className="m-5 flex flex-col items-center justify-center gap-2 border border-dashed rounded-xl p-12">
            <Inbox className="h-10 w-10 text-muted-foreground/40" />
            <p className="text-sm text-muted-foreground">
              {search
                ? "No questions match your search."
                : activeStatus === "PENDING"
                ? "No questions pending moderation."
                : "No answered questions yet."}
            </p>
          </div>
        ) : (
          <div className="divide-y">
            {allQuestions.map((q) => (
              <QuestionModerationCard key={q.id} question={q} />
            ))}

            {/* Infinite scroll sentinel */}
            <div ref={bottomRef} className="h-1" />

            {isFetchingNextPage && (
              <div className="p-5 space-y-3">
                <Skeleton className="h-24 w-full rounded-xl" />
                <Skeleton className="h-24 w-full rounded-xl" />
              </div>
            )}
          </div>
        )}
      </CardContent>
    </Card>
  );
}
