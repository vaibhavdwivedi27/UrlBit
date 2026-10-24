import React from 'react';
import { Hero } from '@/components/landing/Hero';
import { Features } from '@/components/landing/Features';
import { ResultsSection } from '@/components/landing/ResultsSection';
import { PricingSection } from '@/components/landing/PricingSection';

export const LandingPage: React.FC = () => {
  return (
    <div className="space-y-0">
      {/* 1. Hero with Embedded Shortener & Trust Badges */}
      <Hero />

      {/* 2. Powerful Platform Features Grid */}
      <Features />

      {/* 3. Results that Impress Dark Section */}
      <ResultsSection />

      {/* 4. Choose your Plan Interactive Pricing Grid */}
      <PricingSection />
    </div>
  );
};
