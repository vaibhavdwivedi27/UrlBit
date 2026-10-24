import React, { useState } from 'react';
import { Check, X, Sparkles, Rocket, Star, Crown, Gem, ArrowRight } from 'lucide-react';
import { Link } from 'react-router-dom';

type Currency = 'USD' | 'INR';

export const PricingSection: React.FC = () => {
  const [isAnnual, setIsAnnual] = useState(true);
  const [currency, setCurrency] = useState<Currency>('USD');

  const plans = [
    {
      name: 'Free',
      icon: <Rocket className="w-5 h-5 text-blue-500" />,
      pricing: {
        USD: { monthly: 0, annual: 0, symbol: '$' },
        INR: { monthly: 0, annual: 0, symbol: '₹' },
      },
      periodLabel: 'Forever',
      popular: false,
      buttonText: 'Start Free',
      buttonClass:
        'bg-slate-100 dark:bg-slate-800 text-slate-800 dark:text-slate-200 hover:bg-slate-200 dark:hover:bg-slate-700',
      features: [
        { text: '100 short links', included: true },
        { text: '50 dynamic QR Codes', included: true },
        { text: '7-day analytics history', included: true },
        { text: 'Unlimited redirect clicks', included: true },
        { text: 'Custom vanity domain', included: false },
        { text: 'Password protection', included: false },
        { text: 'Link expiration control', included: false },
        { text: 'Priority 24/7 support', included: false },
      ],
    },
    {
      name: 'Basic',
      icon: <Star className="w-5 h-5 text-amber-500" />,
      pricing: {
        USD: { monthly: 5, annual: 4, symbol: '$' },
        INR: { monthly: 399, annual: 339, symbol: '₹' },
      },
      periodLabel: 'per month',
      popular: false,
      buttonText: 'Choose Basic',
      buttonClass:
        'bg-blue-600 hover:bg-blue-700 text-white shadow-md shadow-blue-500/20',
      features: [
        { text: '1,000 short links', included: true },
        { text: '500 dynamic QR Codes', included: true },
        { text: '30-day analytics history', included: true },
        { text: 'Unlimited redirect clicks', included: true },
        { text: '1 custom vanity domain', included: true },
        { text: 'UTM campaign builder', included: true },
        { text: 'Password protection', included: false },
        { text: 'Priority 24/7 support', included: false },
      ],
    },
    {
      name: 'Pro',
      icon: <Gem className="w-5 h-5 text-purple-400" />,
      pricing: {
        USD: { monthly: 12, annual: 10, symbol: '$' },
        INR: { monthly: 999, annual: 849, symbol: '₹' },
      },
      periodLabel: 'per month',
      popular: true,
      buttonText: 'Choose Pro',
      buttonClass:
        'bg-gradient-to-r from-purple-600 to-indigo-600 hover:from-purple-500 hover:to-indigo-500 text-white shadow-lg shadow-purple-500/30 scale-[1.02]',
      features: [
        { text: '5,000 short links', included: true },
        { text: '2,500 dynamic QR Codes', included: true },
        { text: '1-year analytics history', included: true },
        { text: 'Unlimited redirect clicks', included: true },
        { text: '5 custom vanity domains', included: true },
        { text: 'Password protection', included: true },
        { text: 'Link expiration control', included: true },
        { text: 'Priority 24/7 support', included: true },
      ],
    },
    {
      name: 'Enterprise',
      icon: <Crown className="w-5 h-5 text-amber-500" />,
      pricing: {
        USD: { monthly: 39, annual: 32, symbol: '$' },
        INR: { monthly: 2999, annual: 2499, symbol: '₹' },
      },
      periodLabel: 'per month',
      popular: false,
      buttonText: 'Choose Enterprise',
      buttonClass:
        'bg-gradient-to-r from-amber-500 to-orange-600 hover:from-amber-400 hover:to-orange-500 text-white shadow-md shadow-amber-500/20',
      features: [
        { text: '25,000+ short links', included: true },
        { text: 'Unlimited dynamic QR Codes', included: true },
        { text: 'Lifetime analytics history', included: true },
        { text: 'Unlimited redirect clicks', included: true },
        { text: '20 custom vanity domains', included: true },
        { text: 'Dedicated account manager', included: true },
        { text: 'Custom SLA guarantees', included: true },
        { text: '99.985% uptime guarantee', included: true },
      ],
    },
  ];

  return (
    <section id="plans" className="py-24 bg-slate-50 dark:bg-[#070F1E] border-t border-slate-200 dark:border-slate-800/80 transition-colors">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
        {/* Section Header */}
        <h2 className="text-3xl sm:text-5xl font-extrabold text-slate-900 dark:text-white tracking-tight mb-3">
          Choose your Plan
        </h2>
        <p className="text-sm sm:text-base text-slate-600 dark:text-slate-400 max-w-2xl mx-auto mb-10">
          Flexible plans for all needs, from personal projects to large enterprises.
        </p>

        {/* Currency & Billing Frequency Controls */}
        <div className="flex flex-wrap items-center justify-center gap-4 mb-14">
          {/* Currency Toggle: USD ($) vs INR (₹) */}
          <div className="inline-flex items-center p-1 rounded-2xl bg-slate-200 dark:bg-slate-900 border border-slate-300 dark:border-slate-800">
            <button
              type="button"
              onClick={() => setCurrency('USD')}
              className={`px-4 py-1.5 rounded-xl text-xs sm:text-sm font-bold transition-all cursor-pointer ${
                currency === 'USD'
                  ? 'bg-blue-600 text-white shadow-sm'
                  : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white'
              }`}
            >
              USD ($)
            </button>
            <button
              type="button"
              onClick={() => setCurrency('INR')}
              className={`px-4 py-1.5 rounded-xl text-xs sm:text-sm font-bold transition-all cursor-pointer ${
                currency === 'INR'
                  ? 'bg-blue-600 text-white shadow-sm'
                  : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white'
              }`}
            >
              INR (₹)
            </button>
          </div>

          {/* Annual / Monthly Billing Switcher */}
          <div className="inline-flex items-center p-1 rounded-2xl bg-slate-200 dark:bg-slate-900 border border-slate-300 dark:border-slate-800">
            <button
              type="button"
              onClick={() => setIsAnnual(true)}
              className={`flex items-center gap-2 px-5 py-1.5 rounded-xl text-xs sm:text-sm font-bold transition-all cursor-pointer ${
                isAnnual
                  ? 'bg-blue-600 text-white shadow-sm'
                  : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white'
              }`}
            >
              <span>Annual</span>
              <span className="px-2 py-0.5 rounded-full text-[10px] font-extrabold bg-amber-400 text-slate-950">
                Save 15%
              </span>
            </button>

            <button
              type="button"
              onClick={() => setIsAnnual(false)}
              className={`px-5 py-1.5 rounded-xl text-xs sm:text-sm font-bold transition-all cursor-pointer ${
                !isAnnual
                  ? 'bg-blue-600 text-white shadow-sm'
                  : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white'
              }`}
            >
              Monthly
            </button>
          </div>
        </div>

        {/* 4 Pricing Cards Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 max-w-7xl mx-auto mb-16">
          {plans.map((plan) => {
            const planPricing = plan.pricing[currency];
            const price = isAnnual ? planPricing.annual : planPricing.monthly;
            const formattedPrice = price === 0 ? '0' : price.toLocaleString();

            return (
              <div
                key={plan.name}
                className={`relative rounded-3xl p-6 sm:p-7 text-left flex flex-col justify-between transition-all bg-white dark:bg-slate-900 border ${
                  plan.popular
                    ? 'border-purple-500 dark:border-purple-500/80 shadow-2xl shadow-purple-500/10 ring-2 ring-purple-500/20'
                    : 'border-slate-200 dark:border-slate-800 shadow-lg hover:border-slate-300 dark:hover:border-slate-700'
                }`}
              >
                {/* Popular Pill */}
                {plan.popular && (
                  <div className="absolute -top-3.5 left-1/2 -translate-x-1/2 px-3 py-1 rounded-full bg-gradient-to-r from-purple-600 to-indigo-600 text-[10px] font-black uppercase tracking-wider text-white shadow-md flex items-center gap-1">
                    <Sparkles className="w-3 h-3" />
                    POPULAR
                  </div>
                )}

                <div>
                  {/* Card Header */}
                  <div className="w-10 h-10 rounded-xl bg-slate-100 dark:bg-slate-800 flex items-center justify-center mb-4">
                    {plan.icon}
                  </div>
                  <h3 className="text-lg font-bold text-slate-900 dark:text-white">{plan.name}</h3>

                  {/* Price */}
                  <div className="my-4">
                    <div className="flex items-baseline gap-1">
                      <span className="text-4xl font-black text-slate-900 dark:text-white">
                        {planPricing.symbol}{formattedPrice}
                      </span>
                      <span className="text-xs text-slate-500 dark:text-slate-400">
                        {plan.pricing.USD.monthly === 0 ? plan.periodLabel : `/${plan.periodLabel}`}
                      </span>
                    </div>
                    {isAnnual && plan.pricing.USD.monthly > 0 && (
                      <span className="text-[11px] font-semibold text-emerald-600 dark:text-emerald-400">
                        Billed annually (Save 15%)
                      </span>
                    )}
                  </div>

                  {/* Feature Checklist */}
                  <ul className="space-y-2.5 text-xs text-slate-600 dark:text-slate-300 pt-3 border-t border-slate-100 dark:border-slate-800">
                    {plan.features.map((feat, idx) => (
                      <li key={idx} className="flex items-start gap-2">
                        {feat.included ? (
                          <Check className="w-4 h-4 text-emerald-500 shrink-0 mt-0.5" />
                        ) : (
                          <X className="w-4 h-4 text-slate-300 dark:text-slate-600 shrink-0 mt-0.5" />
                        )}
                        <span className={feat.included ? '' : 'text-slate-400 dark:text-slate-500 line-through'}>
                          {feat.text}
                        </span>
                      </li>
                    ))}
                  </ul>
                </div>

                {/* Plan Action CTA */}
                <div className="pt-6 mt-6 border-t border-slate-100 dark:border-slate-800">
                  <Link
                    to="/signup"
                    className={`w-full py-3 rounded-xl text-xs font-bold transition-all flex items-center justify-center gap-1.5 cursor-pointer ${plan.buttonClass}`}
                  >
                    {plan.buttonText}
                    <ArrowRight className="w-3.5 h-3.5" />
                  </Link>
                </div>
              </div>
            );
          })}
        </div>

        {/* Bottom Banner */}
        <div className="p-6 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 max-w-4xl mx-auto flex flex-col sm:flex-row items-center justify-between gap-4 shadow-sm">
          <div className="text-left">
            <h4 className="text-sm font-bold text-slate-900 dark:text-white">
              Not sure which plan to choose?
            </h4>
            <p className="text-xs text-slate-500 dark:text-slate-400">
              Start free and upgrade anytime as your traffic and link volume grows.
            </p>
          </div>

          <div className="flex items-center gap-3 shrink-0">
            <Link
              to="/signup"
              className="px-5 py-2.5 rounded-xl text-xs font-bold text-white bg-blue-600 hover:bg-blue-700 dark:bg-urlbit-gradient shadow-md transition-colors"
            >
              Start Free Now
            </Link>
            <a
              href="mailto:support@urlbit.in"
              className="px-5 py-2.5 rounded-xl text-xs font-semibold border border-slate-300 dark:border-slate-700 text-slate-700 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
            >
              Contact Sales
            </a>
          </div>
        </div>
      </div>
    </section>
  );
};
