export const locales = ["en", "ru", "tj"] as const;
export type Locale = (typeof locales)[number];
export const defaultLocale: Locale = "ru";
