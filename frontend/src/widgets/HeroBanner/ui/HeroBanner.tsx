import Link from "next/link";
import { ArrowRight } from "lucide-react";

export default function HeroBanner() {
  return (
    <section className="relative bg-gradient-to-br from-indigo-600 via-indigo-700 to-purple-800 overflow-hidden">
      {/* Subtle pattern overlay */}
      <div className="absolute inset-0 opacity-10">
        <div className="absolute top-0 -left-4 w-72 h-72 bg-purple-300 rounded-full mix-blend-multiply filter blur-3xl" />
        <div className="absolute top-0 -right-4 w-72 h-72 bg-indigo-300 rounded-full mix-blend-multiply filter blur-3xl" />
        <div className="absolute -bottom-8 left-20 w-72 h-72 bg-pink-300 rounded-full mix-blend-multiply filter blur-3xl" />
      </div>

      <div className="relative max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-20 sm:py-28 lg:py-36">
        <div className="max-w-2xl">
          <h1 className="text-4xl sm:text-5xl lg:text-6xl font-bold text-white tracking-tight">
            Quality Products,
            <br />
            <span className="text-indigo-200">Delivered with Care</span>
          </h1>
          <p className="mt-6 text-lg sm:text-xl text-indigo-100 leading-relaxed max-w-xl">
            Discover our curated collection of premium products. Trusted by
            thousands of customers across Tajikistan.
          </p>
          <div className="mt-10 flex flex-col sm:flex-row gap-4">
            <Link
              href="/products"
              className="inline-flex items-center justify-center gap-2 px-8 py-3.5 bg-white text-indigo-700 font-semibold rounded-lg hover:bg-indigo-50 transition-colors text-base"
            >
              Browse Products
              <ArrowRight className="h-5 w-5" />
            </Link>
          </div>
        </div>
      </div>
    </section>
  );
}
