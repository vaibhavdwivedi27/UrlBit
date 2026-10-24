import React from 'react';
import { BarChart2, Link2, Sparkles, Users, ShieldCheck, Clock, Star } from 'lucide-react';

export const ResultsSection: React.FC = () => {
  return (
    <section className="py-24 bg-gradient-to-r from-slate-900 via-[#050B18] to-slate-900 text-white border-t border-slate-800/80 relative overflow-hidden">
      {/* Background radial dot grid & Ambient glows */}
      <div
        className="absolute inset-0 opacity-10 pointer-events-none"
        style={{
          backgroundImage: 'radial-gradient(circle at 2px 2px, white 1px, transparent 0)',
          backgroundSize: '40px 40px',
        }}
      />
      <div className="absolute top-20 left-20 w-72 h-72 bg-blue-500/10 rounded-full blur-3xl animate-pulse pointer-events-none" />
      <div
        className="absolute bottom-32 right-20 w-80 h-80 bg-purple-500/10 rounded-full blur-3xl animate-pulse pointer-events-none"
        style={{ animationDelay: '1s' }}
      />
      <div
        className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-96 h-96 bg-indigo-500/5 rounded-full blur-3xl animate-pulse pointer-events-none"
        style={{ animationDelay: '2s' }}
      />

      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 relative z-10 text-center">
        {/* Top Icon Badge */}
        <div className="inline-flex items-center justify-center w-14 h-14 rounded-2xl bg-gradient-to-tr from-blue-600 to-indigo-600 mb-6 shadow-xl shadow-blue-500/25">
          <BarChart2 className="w-7 h-7 text-white" />
        </div>

        {/* Heading */}
        <h2 className="text-3xl sm:text-5xl font-extrabold tracking-tight mb-3">
          Results that Impress
        </h2>
        <p className="text-base sm:text-lg text-slate-300 max-w-2xl mx-auto mb-16 leading-relaxed">
          Real numbers trusted by thousands of creators, marketers, and businesses worldwide.
        </p>

        {/* 3 Main Result Cards */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-8 max-w-5xl mx-auto mb-16">
          {/* Card 1: Links Shortened */}
          <div className="group relative bg-gradient-to-br from-white/5 to-white/10 backdrop-blur-xl rounded-3xl p-8 border border-white/10 hover:border-white/20 transition-all duration-500 hover:scale-105 hover:-translate-y-2 shadow-2xl text-center">
            <div className="absolute top-6 right-6 w-12 h-12 bg-gradient-to-br from-cyan-400/20 to-blue-500/20 rounded-2xl flex items-center justify-center text-cyan-300">
              <Link2 className="w-6 h-6" />
            </div>

            <div className="pt-4">
              <div className="mb-3">
                <h3 className="text-5xl sm:text-6xl font-black bg-gradient-to-r from-cyan-300 via-blue-400 to-indigo-400 bg-clip-text text-transparent leading-none">
                  10M+
                </h3>
              </div>
              <h4 className="text-lg font-bold text-white mb-1">Links Shortened</h4>
              <p className="text-xs text-slate-400">Processed successfully</p>

              {/* Animated Progress Track */}
              <div className="mt-5 w-full bg-slate-700/60 rounded-full h-2 overflow-hidden">
                <div className="bg-gradient-to-r from-cyan-400 to-blue-500 h-2 rounded-full w-4/5 animate-pulse" />
              </div>
            </div>
          </div>

          {/* Card 2: Clicks Processed */}
          <div className="group relative bg-gradient-to-br from-white/5 to-white/10 backdrop-blur-xl rounded-3xl p-8 border border-white/10 hover:border-white/20 transition-all duration-500 hover:scale-105 hover:-translate-y-2 shadow-2xl text-center">
            <div className="absolute top-6 right-6 w-12 h-12 bg-gradient-to-br from-emerald-400/20 to-green-500/20 rounded-2xl flex items-center justify-center text-emerald-300">
              <Sparkles className="w-6 h-6" />
            </div>

            <div className="pt-4">
              <div className="mb-3">
                <h3 className="text-5xl sm:text-6xl font-black bg-gradient-to-r from-emerald-300 via-green-400 to-teal-400 bg-clip-text text-transparent leading-none">
                  100M+
                </h3>
              </div>
              <h4 className="text-lg font-bold text-white mb-1">Clicks Processed</h4>
              <p className="text-xs text-slate-400">With lightning speed</p>

              {/* Animated Progress Track */}
              <div className="mt-5 w-full bg-slate-700/60 rounded-full h-2 overflow-hidden">
                <div className="bg-gradient-to-r from-emerald-400 to-teal-400 h-2 rounded-full w-full animate-pulse" />
              </div>
            </div>
          </div>

          {/* Card 3: Active Users */}
          <div className="group relative bg-gradient-to-br from-white/5 to-white/10 backdrop-blur-xl rounded-3xl p-8 border border-white/10 hover:border-white/20 transition-all duration-500 hover:scale-105 hover:-translate-y-2 shadow-2xl text-center">
            <div className="absolute top-6 right-6 w-12 h-12 bg-gradient-to-br from-violet-400/20 to-purple-500/20 rounded-2xl flex items-center justify-center text-violet-300">
              <Users className="w-6 h-6" />
            </div>

            <div className="pt-4">
              <div className="mb-3">
                <h3 className="text-5xl sm:text-6xl font-black bg-gradient-to-r from-violet-300 via-purple-400 to-fuchsia-400 bg-clip-text text-transparent leading-none">
                  50K+
                </h3>
              </div>
              <h4 className="text-lg font-bold text-white mb-1">Active Users</h4>
              <p className="text-xs text-slate-400">Trusting our platform</p>

              {/* Animated Progress Track */}
              <div className="mt-5 w-full bg-slate-700/60 rounded-full h-2 overflow-hidden">
                <div className="bg-gradient-to-r from-violet-400 to-fuchsia-400 h-2 rounded-full w-3/4 animate-pulse" />
              </div>
            </div>
          </div>
        </div>

        {/* Quality Guarantees Bar */}
        <div className="bg-gradient-to-r from-white/5 to-white/10 backdrop-blur-xl rounded-3xl p-8 border border-white/10 max-w-4xl mx-auto shadow-2xl">
          <div className="text-center mb-6">
            <h3 className="text-xl font-bold text-white mb-1">Quality Guarantees</h3>
            <p className="text-xs text-slate-400">Metrics that prove our operational excellence</p>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-6">
            <div className="flex flex-col items-center p-6 bg-gradient-to-br from-emerald-500/10 to-green-500/10 rounded-2xl border border-emerald-500/20">
              <div className="w-12 h-12 bg-gradient-to-br from-emerald-400 to-green-500 rounded-2xl flex items-center justify-center mb-3 text-white shadow-md">
                <ShieldCheck className="w-6 h-6" />
              </div>
              <div className="text-3xl font-black text-emerald-300 mb-1">99.985%</div>
              <div className="text-xs text-slate-300 font-semibold">Uptime Guaranteed</div>
            </div>

            <div className="flex flex-col items-center p-6 bg-gradient-to-br from-amber-500/10 to-orange-500/10 rounded-2xl border border-amber-500/20">
              <div className="w-12 h-12 bg-gradient-to-br from-amber-400 to-orange-500 rounded-2xl flex items-center justify-center mb-3 text-white shadow-md">
                <Clock className="w-6 h-6" />
              </div>
              <div className="text-3xl font-black text-amber-300 mb-1">&lt; 30ms</div>
              <div className="text-xs text-slate-300 font-semibold">Response Time</div>
            </div>

            <div className="flex flex-col items-center p-6 bg-gradient-to-br from-rose-500/10 to-pink-500/10 rounded-2xl border border-rose-500/20">
              <div className="w-12 h-12 bg-gradient-to-br from-rose-400 to-pink-500 rounded-2xl flex items-center justify-center mb-3 text-white shadow-md">
                <Star className="w-6 h-6" />
              </div>
              <div className="text-3xl font-black text-rose-300 mb-1">4.8 / 5</div>
              <div className="text-xs text-slate-300 font-semibold">User Satisfaction</div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
};
