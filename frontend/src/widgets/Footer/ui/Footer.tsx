import Link from "next/link";
import { Package, Mail, Phone, MapPin } from "lucide-react";

export default function Footer() {
  return (
    <footer className="bg-gray-900 text-gray-300">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
          {/* Brand */}
          <div>
            <Link href="/" className="flex items-center gap-2 text-white mb-4">
              <Package className="h-6 w-6 text-indigo-400" />
              <span className="text-xl font-bold">Radolfa</span>
            </Link>
            <p className="text-sm leading-relaxed">
              Your trusted marketplace for premium products. Quality guaranteed,
              delivered with care.
            </p>
          </div>

          {/* Quick Links */}
          <div>
            <h3 className="text-white font-semibold mb-4">Quick Links</h3>
            <ul className="space-y-2 text-sm">
              <li>
                <Link href="/" className="hover:text-white transition-colors">
                  Home
                </Link>
              </li>
              <li>
                <Link
                  href="/products"
                  className="hover:text-white transition-colors"
                >
                  Products
                </Link>
              </li>
              <li>
                <Link
                  href="/login"
                  className="hover:text-white transition-colors"
                >
                  Login
                </Link>
              </li>
              <li>
                <Link
                  href="/profile"
                  className="hover:text-white transition-colors"
                >
                  My Account
                </Link>
              </li>
            </ul>
          </div>

          {/* Contact */}
          <div>
            <h3 className="text-white font-semibold mb-4">Contact</h3>
            <ul className="space-y-3 text-sm">
              <li className="flex items-center gap-2">
                <MapPin className="h-4 w-4 text-indigo-400 shrink-0" />
                <span>Dushanbe, Tajikistan</span>
              </li>
              <li className="flex items-center gap-2">
                <Phone className="h-4 w-4 text-indigo-400 shrink-0" />
                <span>+992 000 000 000</span>
              </li>
              <li className="flex items-center gap-2">
                <Mail className="h-4 w-4 text-indigo-400 shrink-0" />
                <span>info@radolfa.tj</span>
              </li>
            </ul>
          </div>
        </div>

        {/* Copyright */}
        <div className="mt-10 pt-6 border-t border-gray-800 text-center text-sm text-gray-500">
          &copy; {new Date().getFullYear()} Radolfa. All rights reserved.
        </div>
      </div>
    </footer>
  );
}
