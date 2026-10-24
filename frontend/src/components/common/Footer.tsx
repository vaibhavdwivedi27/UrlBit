import React from 'react';
import { Logo } from './Logo';
import { ShieldCheck, Mail, MapPin } from 'lucide-react';
import { Link } from 'react-router-dom';

export const Footer: React.FC = () => {
  return (
    <footer id="terms" className="border-t border-slate-800 bg-[#030712] py-14 text-slate-400 text-sm">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-10 mb-12">
          {/* Brand Info */}
          <div className="space-y-4">
            <Logo size="md" showTagline forceVariant="dark" />
            <p className="text-xs text-slate-400 leading-relaxed">
              Efficient, secure and accessible URL shortening service. We transform long links into short and powerful experiences.
            </p>
          </div>

          {/* Features Column */}
          <div>
            <h4 className="text-xs font-bold text-white uppercase tracking-wider mb-4">
              Features
            </h4>
            <ul className="space-y-2.5 text-xs">
              <li>
                <a href="#platform" className="hover:text-cyan-400 transition-colors">
                  Link Shortener
                </a>
              </li>
              <li>
                <a href="#platform" className="hover:text-cyan-400 transition-colors">
                  QR Code Generator
                </a>
              </li>
              <li>
                <a href="#features" className="hover:text-cyan-400 transition-colors">
                  Advanced Analytics
                </a>
              </li>
              <li>
                <a href="#plans" className="hover:text-cyan-400 transition-colors">
                  Custom Domains
                </a>
              </li>
              <li>
                <a href="#plans" className="hover:text-cyan-400 transition-colors">
                  Campaign Management
                </a>
              </li>
            </ul>
          </div>

          {/* Security Column */}
          <div>
            <h4 className="text-xs font-bold text-white uppercase tracking-wider mb-4">
              Security
            </h4>
            <ul className="space-y-2.5 text-xs">
              <li className="flex items-center gap-1.5 text-slate-400">
                <ShieldCheck className="w-3.5 h-3.5 text-emerald-400" />
                Google Safe Browsing
              </li>
              <li className="flex items-center gap-1.5 text-slate-400">
                <ShieldCheck className="w-3.5 h-3.5 text-emerald-400" />
                Virus Total Protection
              </li>
              <li className="flex items-center gap-1.5 text-slate-400">
                <ShieldCheck className="w-3.5 h-3.5 text-emerald-400" />
                Norton Safe Web
              </li>
              <li className="flex items-center gap-1.5 text-slate-400">
                <ShieldCheck className="w-3.5 h-3.5 text-emerald-400" />
                SSL / TLS Encryption
              </li>
            </ul>
          </div>

          {/* Contact & Legal Column */}
          <div>
            <h4 className="text-xs font-bold text-white uppercase tracking-wider mb-4">
              Contact & Legal
            </h4>
            <ul className="space-y-2.5 text-xs">
              <li className="flex items-center gap-2 text-slate-400">
                <MapPin className="w-3.5 h-3.5 text-slate-500 shrink-0" />
                <span>Mumbai, India</span>
              </li>
              <li className="flex items-center gap-2">
                <Mail className="w-3.5 h-3.5 text-slate-500 shrink-0" />
                <a
                  href="mailto:support@urlbit.in"
                  className="hover:text-cyan-400 transition-colors"
                >
                  support@urlbit.in
                </a>
              </li>
              <li className="pt-2">
                <Link to="/terms" className="hover:text-cyan-400 transition-colors">
                  Terms of Use
                </Link>
              </li>
              <li>
                <Link to="/privacy" className="hover:text-cyan-400 transition-colors">
                  Privacy Policy
                </Link>
              </li>
              <li>
                <span className="hover:text-cyan-400 cursor-pointer transition-colors">
                  Cookie Policy
                </span>
              </li>
            </ul>
          </div>
        </div>

        {/* Bottom bar */}
        <div className="pt-8 border-t border-slate-800/80 flex flex-col sm:flex-row items-center justify-between text-xs text-slate-500 gap-4">
          <p>© {new Date().getFullYear()} Urlbit. All rights reserved.</p>
          <div className="flex flex-wrap items-center gap-4">
            <span className="flex items-center gap-1.5">
              <span className="w-2 h-2 rounded-full bg-emerald-400" />
              System Operational
            </span>
            <span>•</span>
            <span>&lt; 30ms response</span>
            <span>•</span>
            <span>99.985% uptime</span>
          </div>
        </div>
      </div>
    </footer>
  );
};
