import React from 'react';
import { GuestShortener } from './GuestShortener';
import {
  ArrowRight,
  CheckCircle2,
  Sparkles,
  Link2,
  QrCode,
  ShieldCheck,
  Lock,
  Calendar,
  BarChart2,
} from 'lucide-react';
import { Link } from 'react-router-dom';

export const Hero: React.FC = () => {
  return (
    <section
      id="platform"
      className="relative pt-12 pb-20 md:pt-20 md:pb-28 overflow-hidden bg-gradient-to-br from-blue-50/70 via-white to-indigo-50/50 dark:from-slate-950 dark:via-[#050B18] dark:to-slate-950 transition-colors"
    >
      {/* Background Ambient Glows & Grid */}
      <div className="absolute inset-0 bg-[radial-gradient(#cbd5e1_1px,transparent_1px)] dark:bg-[radial-gradient(#1e293b_1px,transparent_1px)] [background-size:24px_24px] opacity-35 pointer-events-none" />
      <div className="absolute top-1/4 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[700px] h-[400px] bg-blue-500/10 dark:bg-cyan-500/15 rounded-full blur-[140px] pointer-events-none" />

      {/* Floating Animated Ambient Icons */}
      <div className="hidden lg:block absolute inset-0 overflow-hidden pointer-events-none select-none z-0">
        <div
          className="absolute top-16 left-12 animate-bounce opacity-20"
          style={{ animationDuration: '6s', animationDelay: '0s' }}
        >
          <Link2 className="w-6 h-6 text-blue-500" />
        </div>
        <div
          className="absolute top-28 left-1/4 animate-bounce opacity-15"
          style={{ animationDuration: '7s', animationDelay: '2s' }}
        >
          <Sparkles className="w-5 h-5 text-amber-400" />
        </div>
        <div
          className="absolute top-20 right-20 animate-bounce opacity-20"
          style={{ animationDuration: '8s', animationDelay: '1s' }}
        >
          <QrCode className="w-7 h-7 text-purple-500" />
        </div>
        <div
          className="absolute top-44 right-1/4 animate-bounce opacity-15"
          style={{ animationDuration: '6.5s', animationDelay: '3s' }}
        >
          <ShieldCheck className="w-5 h-5 text-emerald-400" />
        </div>
        <div
          className="absolute top-72 left-16 animate-bounce opacity-20"
          style={{ animationDuration: '7.5s', animationDelay: '1.5s' }}
        >
          <Lock className="w-5 h-5 text-indigo-400" />
        </div>
        <div
          className="absolute top-80 right-16 animate-bounce opacity-20"
          style={{ animationDuration: '8.5s', animationDelay: '2.5s' }}
        >
          <Calendar className="w-6 h-6 text-rose-400" />
        </div>
        <div
          className="absolute bottom-40 left-24 animate-bounce opacity-15"
          style={{ animationDuration: '7s', animationDelay: '4s' }}
        >
          <BarChart2 className="w-6 h-6 text-cyan-400" />
        </div>
        <div
          className="absolute bottom-32 right-32 animate-bounce opacity-20"
          style={{ animationDuration: '6.8s', animationDelay: '3.5s' }}
        >
          <Link2 className="w-5 h-5 text-emerald-400" />
        </div>
        <div
          className="absolute bottom-60 right-1/4 animate-bounce opacity-15"
          style={{ animationDuration: '9s', animationDelay: '5s' }}
        >
          <Sparkles className="w-5 h-5 text-pink-400" />
        </div>
        <div
          className="absolute bottom-72 left-1/3 animate-bounce opacity-15"
          style={{ animationDuration: '8s', animationDelay: '4.5s' }}
        >
          <CheckCircle2 className="w-5 h-5 text-teal-400" />
        </div>
      </div>

      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 relative z-10 text-center">
        {/* Hero Headline */}
        <h1 className="text-4xl sm:text-6xl lg:text-7xl font-extrabold tracking-tight text-slate-900 dark:text-white max-w-4xl mx-auto leading-tight mb-4">
          Transform your{' '}
          <span className="bg-clip-text text-transparent bg-gradient-to-r from-blue-600 via-indigo-600 to-cyan-500 font-black">
            long links
          </span>{' '}
          into{' '}
          <span className="text-slate-900 dark:text-white font-black">smart, powerful URLs</span>
        </h1>

        {/* Subhead */}
        <p className="text-base sm:text-lg text-slate-600 dark:text-slate-400 max-w-2xl mx-auto mb-10 leading-relaxed font-normal">
          Urlbit helps you shorten links, generate custom QR codes, and track audience engagement with real-time analytics.
        </p>

        {/* Embedded Interactive Shortener Box */}
        <GuestShortener />

        {/* Secondary Action Row */}
        <div className="mt-8 flex flex-col sm:flex-row items-center justify-center gap-3">
          <Link
            to="/signup"
            className="inline-flex items-center gap-2 px-6 py-3 rounded-xl text-sm font-bold text-white bg-blue-600 hover:bg-blue-700 dark:bg-urlbit-gradient shadow-md shadow-blue-500/20 dark:shadow-cyan-500/25 hover:scale-[1.02] active:scale-[0.98] transition-all cursor-pointer"
          >
            Start Free
            <ArrowRight className="w-4 h-4" />
          </Link>
          <a
            href="#plans"
            className="inline-flex items-center gap-2 px-6 py-3 rounded-xl text-sm font-semibold border border-slate-300 dark:border-slate-700 text-slate-700 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800/80 transition-colors cursor-pointer"
          >
            <span className="text-[10px] px-2 py-0.5 rounded-full bg-purple-500/10 text-purple-600 dark:text-purple-400 font-bold uppercase">
              NEW
            </span>
            Discover Plans
            <ArrowRight className="w-3.5 h-3.5" />
          </a>
        </div>

        {/* Trust Row */}
        <div className="flex flex-wrap items-center justify-center gap-6 text-xs font-medium text-slate-500 dark:text-slate-400 mt-6">
          <span className="flex items-center gap-1.5">
            <CheckCircle2 className="w-4 h-4 text-emerald-500" />
            No credit card required
          </span>
          <span>•</span>
          <span className="flex items-center gap-1.5">
            <CheckCircle2 className="w-4 h-4 text-emerald-500" />
            30-second instant setup
          </span>
          <span>•</span>
          <span className="flex items-center gap-1.5">
            <CheckCircle2 className="w-4 h-4 text-emerald-500" />
            99.985% Uptime guaranteed
          </span>
        </div>

        {/* Key Platform Metrics Strip */}
        <div className="grid grid-cols-2 md:grid-cols-4 gap-6 max-w-4xl mx-auto mt-12 py-8 border-y border-slate-200 dark:border-slate-800/80">
          <div>
            <div className="text-3xl sm:text-4xl font-extrabold text-slate-900 dark:text-white">
              10M+
            </div>
            <div className="text-xs text-slate-500 dark:text-slate-400 mt-1">Links Shortened</div>
          </div>
          <div>
            <div className="text-3xl sm:text-4xl font-extrabold text-slate-900 dark:text-white">
              100M+
            </div>
            <div className="text-xs text-slate-500 dark:text-slate-400 mt-1">Clicks Tracked</div>
          </div>
          <div>
            <div className="text-3xl sm:text-4xl font-extrabold text-slate-900 dark:text-white">
              99.985%
            </div>
            <div className="text-xs text-slate-500 dark:text-slate-400 mt-1">Uptime</div>
          </div>
          <div>
            <div className="text-3xl sm:text-4xl font-extrabold text-slate-900 dark:text-white">
              &lt; 30ms
            </div>
            <div className="text-xs text-slate-500 dark:text-slate-400 mt-1">Response Time</div>
          </div>
        </div>
      </div>
    </section>
  );
};
