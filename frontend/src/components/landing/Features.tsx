import React from 'react';
import { Cloud, Zap, Shield, BarChart3, Globe, Infinity as InfinityIcon, Cpu } from 'lucide-react';

export const Features: React.FC = () => {
  const mainCards = [
    {
      icon: <Cloud className="w-6 h-6 text-white" />,
      iconBg: 'bg-blue-600',
      gradient:
        'from-blue-50 to-indigo-50 dark:from-blue-950/30 dark:to-indigo-950/30 border-blue-100 dark:border-blue-900/30',
      title: 'Auto-Scalable Cloud Infrastructure',
      description:
        'Built on high-availability cloud architecture that guarantees 99.985% uptime and scales effortlessly with massive traffic surges.',
      pillText: '99.985% guaranteed uptime',
      pillColor:
        'text-blue-700 dark:text-blue-300 bg-white/80 dark:bg-blue-950/60 border-blue-200 dark:border-blue-800/60',
    },
    {
      icon: <Zap className="w-6 h-6 text-white" />,
      iconBg: 'bg-emerald-600',
      gradient:
        'from-green-50 to-emerald-50 dark:from-emerald-950/30 dark:to-teal-950/30 border-green-100 dark:border-emerald-900/30',
      title: 'Blazing-Fast Redirection',
      description:
        'Global low-latency routing delivers instant link redirection with an average response time under 30ms.',
      pillText: '< 30ms response time',
      pillColor:
        'text-emerald-700 dark:text-emerald-300 bg-white/80 dark:bg-emerald-950/60 border-emerald-200 dark:border-emerald-800/60',
    },
    {
      icon: <Shield className="w-6 h-6 text-white" />,
      iconBg: 'bg-purple-600',
      gradient:
        'from-purple-50 to-violet-50 dark:from-purple-950/30 dark:to-violet-950/30 border-purple-100 dark:border-purple-900/30',
      title: 'Advanced Threat Protection',
      description:
        'Automated link scanning protects your visitors and brand reputation against phishing, malware, and suspicious sites.',
      pillText: 'Verified Safe Browsing',
      pillColor:
        'text-purple-700 dark:text-purple-300 bg-white/80 dark:bg-purple-950/60 border-purple-200 dark:border-purple-800/60',
    },
    {
      icon: <BarChart3 className="w-6 h-6 text-white" />,
      iconBg: 'bg-orange-600',
      gradient:
        'from-orange-50 to-amber-50 dark:from-amber-950/30 dark:to-orange-950/30 border-orange-100 dark:border-amber-900/30',
      title: 'Real-Time Click Analytics',
      description:
        'Gain deep insights into your audience with live reports on click counts, visitor locations, and top referring channels.',
      pillText: 'Live Click Intelligence',
      pillColor:
        'text-orange-700 dark:text-orange-300 bg-white/80 dark:bg-amber-950/60 border-orange-200 dark:border-amber-800/60',
    },
  ];

  const secondaryFeatures = [
    {
      icon: <Globe className="w-5 h-5 text-blue-500" />,
      title: 'Global Edge Network',
      description: 'Distributed worldwide edge routing for instant short URL redirection anywhere across the globe.',
    },
    {
      icon: <InfinityIcon className="w-5 h-5 text-purple-500" />,
      title: 'Unlimited Clicks',
      description: 'No monthly limits or visitor throttling on your links. Grow and scale your traffic without boundaries.',
    },
    {
      icon: <Cpu className="w-5 h-5 text-cyan-500" />,
      title: 'Developer API',
      description: 'Modern REST API with full documentation and high reliability to easily automate link workflows.',
    },
  ];

  return (
    <section id="features" className="py-24 bg-white dark:bg-[#070F1E] border-t border-slate-200 dark:border-slate-800/80 transition-colors">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        {/* Section Heading */}
        <div className="text-center max-w-3xl mx-auto mb-16">
          <h2 className="text-3xl sm:text-4xl font-extrabold text-slate-900 dark:text-white tracking-tight">
            Powerful Platform
          </h2>
          <p className="mt-3 text-base sm:text-lg text-slate-600 dark:text-slate-400">
            Cutting-edge technology to ensure your links work reliably, with unmatched speed and security.
          </p>
        </div>

        {/* 2x2 Feature Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-8 max-w-5xl mx-auto mb-16">
          {mainCards.map((card, idx) => (
            <div
              key={idx}
              className={`group bg-gradient-to-br ${card.gradient} rounded-3xl p-8 border hover:shadow-xl transition-all duration-300 hover:-translate-y-1 flex flex-col justify-between space-y-6 shadow-sm`}
            >
              <div className="flex items-start space-x-4">
                <div className="shrink-0">
                  <div
                    className={`w-12 h-12 ${card.iconBg} rounded-2xl flex items-center justify-center shadow-md group-hover:scale-110 transition-transform duration-300`}
                  >
                    {card.icon}
                  </div>
                </div>
                <div className="flex-1">
                  <h3 className="text-xl font-bold text-slate-900 dark:text-white mb-2">
                    {card.title}
                  </h3>
                  <p className="text-sm text-slate-600 dark:text-slate-300 leading-relaxed">
                    {card.description}
                  </p>
                </div>
              </div>

              <div>
                <span
                  className={`inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-full text-xs font-semibold border shadow-xs ${card.pillColor}`}
                >
                  {card.pillText}
                </span>
              </div>
            </div>
          ))}
        </div>

        {/* Additional Features Bar */}
        <div className="bg-slate-50 dark:bg-slate-900/60 rounded-3xl p-8 border border-slate-200 dark:border-slate-800/80 max-w-5xl mx-auto">
          <h3 className="text-xl font-bold text-slate-900 dark:text-white text-center mb-8">
            Additional Platform Highlights
          </h3>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
            {secondaryFeatures.map((feat, idx) => (
              <div key={idx} className="text-center group">
                <div className="w-12 h-12 bg-white dark:bg-slate-800 rounded-2xl flex items-center justify-center mx-auto mb-4 border border-slate-200 dark:border-slate-700 shadow-sm group-hover:scale-110 transition-transform duration-300">
                  {feat.icon}
                </div>
                <h4 className="font-bold text-slate-900 dark:text-white mb-1.5">{feat.title}</h4>
                <p className="text-xs text-slate-600 dark:text-slate-400 leading-relaxed">
                  {feat.description}
                </p>
              </div>
            ))}
          </div>
        </div>
      </div>
    </section>
  );
};
