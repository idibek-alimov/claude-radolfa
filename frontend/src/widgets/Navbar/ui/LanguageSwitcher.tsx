"use client";

import { useState, useRef, useEffect } from "react";
import { Globe, Check } from "lucide-react";
import { motion, AnimatePresence } from "framer-motion";

type Lang = "EN" | "RU" | "TJ";

const LANGUAGES: { code: Lang; label: string }[] = [
  { code: "EN", label: "English" },
  { code: "RU", label: "Русский" },
  { code: "TJ", label: "Тоҷикӣ" },
];

export default function LanguageSwitcher() {
  const [current, setCurrent] = useState<Lang>("EN");
  const [isOpen, setIsOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) {
        setIsOpen(false);
      }
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const handleSelect = (code: Lang) => {
    setCurrent(code);
    setIsOpen(false);
  };

  return (
    <div ref={ref} className="relative">
      <button
        onClick={() => setIsOpen((prev) => !prev)}
        className="flex items-center gap-1.5 px-2.5 py-1.5 rounded-full text-xs font-medium text-muted-foreground hover:text-foreground hover:bg-accent/60 transition-all duration-200"
        aria-label="Change language"
      >
        <Globe className="h-3.5 w-3.5" />
        <span>{current}</span>
      </button>

      <AnimatePresence>
        {isOpen && (
          <motion.div
            initial={{ opacity: 0, y: -4, scale: 0.96 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: -4, scale: 0.96 }}
            transition={{ duration: 0.15, ease: "easeOut" }}
            className="absolute right-0 mt-2 w-40 rounded-xl border bg-popover shadow-lg overflow-hidden z-50"
          >
            {LANGUAGES.map(({ code, label }) => (
              <button
                key={code}
                onClick={() => handleSelect(code)}
                className={`flex items-center justify-between w-full px-3.5 py-2.5 text-sm transition-colors ${
                  current === code
                    ? "text-foreground bg-accent/50"
                    : "text-muted-foreground hover:text-foreground hover:bg-accent/30"
                }`}
              >
                <span className="flex items-center gap-2.5">
                  <span className="text-xs font-semibold w-5 text-center opacity-60">
                    {code}
                  </span>
                  <span>{label}</span>
                </span>
                {current === code && (
                  <motion.span
                    initial={{ scale: 0 }}
                    animate={{ scale: 1 }}
                    transition={{ type: "spring", stiffness: 500, damping: 25 }}
                  >
                    <Check className="h-3.5 w-3.5 text-primary" />
                  </motion.span>
                )}
              </button>
            ))}
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
