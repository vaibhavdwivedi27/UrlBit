import React, { useState } from 'react';
import { linksApi } from '@/api/links';
import type { ShortUrlResponse } from '@/types/api';
import { ProblemAlert } from '@/components/common/ProblemAlert';
import { QrCodeModal } from '@/components/common/QrCodeModal';
import {
  Link2,
  ArrowRight,
  Copy,
  Check,
  QrCode,
  ExternalLink,
  Loader2,
  Sparkles,
  Clipboard,
  Sliders,
  Calendar,
  Lock,
  X,
} from 'lucide-react';
import { toast } from 'sonner';
import { Link } from 'react-router-dom';
import { useAuth } from '@/hooks/useAuth';

export const GuestShortener: React.FC = () => {
  const { isAuthenticated } = useAuth();
  const [url, setUrl] = useState('');
  const [customAlias, setCustomAlias] = useState('');
  const [expiresAt, setExpiresAt] = useState('');
  const [showCustomAlias, setShowCustomAlias] = useState(false);
  const [showExpiration, setShowExpiration] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<any>(null);

  const [recentLinks, setRecentLinks] = useState<ShortUrlResponse[]>(() => {
    try {
      const saved = localStorage.getItem('urlbit_guest_recent');
      return saved ? JSON.parse(saved) : [];
    } catch {
      return [];
    }
  });

  const [copiedCode, setCopiedCode] = useState<string | null>(null);
  const [activeQrLink, setActiveQrLink] = useState<ShortUrlResponse | null>(null);

  const guestQuotaCount = Math.min(recentLinks.length, 5);

  const handlePaste = async () => {
    try {
      const text = await navigator.clipboard.readText();
      if (text) {
        setUrl(text.trim());
        toast.success('Pasted from clipboard');
      }
    } catch {
      toast.error('Unable to access clipboard. Please paste manually.');
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!url.trim()) return;

    setError(null);
    setIsLoading(true);

    try {
      let destination = url.trim();
      if (!destination.startsWith('http://') && !destination.startsWith('https://')) {
        destination = 'https://' + destination;
      }

      const payload: { longUrl: string; customAlias?: string; expiresAt?: string } = {
        longUrl: destination,
      };

      if (showCustomAlias && customAlias.trim()) {
        payload.customAlias = customAlias.trim();
      }

      if (showExpiration && expiresAt) {
        payload.expiresAt = new Date(expiresAt).toISOString();
      }

      const response = await linksApi.create(payload);

      const updated = [response, ...recentLinks.filter((l) => l.id !== response.id)].slice(0, 5);
      setRecentLinks(updated);
      try {
        localStorage.setItem('urlbit_guest_recent', JSON.stringify(updated));
      } catch {}

      setUrl('');
      setCustomAlias('');
      setExpiresAt('');
      toast.success('Short link generated successfully!');
    } catch (err) {
      setError(err);
    } finally {
      setIsLoading(false);
    }
  };

  const handleCopy = async (link: ShortUrlResponse) => {
    try {
      await navigator.clipboard.writeText(link.shortUrl);
      setCopiedCode(link.code);
      toast.success('Copied short link to clipboard!');
      setTimeout(() => setCopiedCode(null), 2000);
    } catch {
      toast.error('Failed to copy to clipboard');
    }
  };

  return (
    <div className="w-full max-w-3xl mx-auto space-y-6">
      {/* Main Shortener Card */}
      <div className="rounded-3xl bg-white dark:bg-slate-900/90 border border-slate-200 dark:border-slate-800 shadow-2xl dark:shadow-cyan-950/40 p-4 sm:p-7 transition-colors">
        <form onSubmit={handleSubmit} className="space-y-4">
          {/* Main Input Row */}
          <div className="flex flex-col sm:flex-row items-stretch gap-2.5 p-1.5 rounded-2xl border border-slate-300 dark:border-slate-700 bg-slate-50 dark:bg-slate-950/80 focus-within:border-blue-500 dark:focus-within:border-cyan-400 focus-within:ring-2 focus-within:ring-blue-500/20 dark:focus-within:ring-cyan-500/20 transition-all">
            <div className="relative flex-1 flex items-center min-w-0">
              <Link2 className="w-5 h-5 text-slate-400 dark:text-cyan-400 ml-3 shrink-0" />
              <input
                type="text"
                value={url}
                onChange={(e) => setUrl(e.target.value)}
                placeholder="Paste your URL here... (e.g. https://mysite.com/very-long-url)"
                disabled={isLoading}
                className="w-full px-3 py-3 text-sm bg-transparent text-slate-900 dark:text-white placeholder-slate-400 focus:outline-none"
                required
              />

              {/* Clear (X) Button */}
              {url && (
                <button
                  type="button"
                  onClick={() => setUrl('')}
                  className="mr-1.5 p-1 rounded-md text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 hover:bg-slate-200 dark:hover:bg-slate-800 transition-colors cursor-pointer"
                  title="Clear input"
                >
                  <X className="w-4 h-4" />
                </button>
              )}

              {/* Paste Button */}
              <button
                type="button"
                onClick={handlePaste}
                className="mr-2 px-2.5 py-1.5 rounded-lg text-xs font-semibold text-slate-600 dark:text-slate-300 hover:text-slate-900 dark:hover:text-white bg-slate-200/80 hover:bg-slate-300 dark:bg-slate-800 dark:hover:bg-slate-700 transition-colors flex items-center gap-1 shrink-0 cursor-pointer"
                title="Paste from clipboard"
              >
                <Clipboard className="w-3.5 h-3.5" />
                <span>Paste</span>
              </button>
            </div>

            {/* Shorten Now Button */}
            <button
              type="submit"
              disabled={isLoading || !url.trim()}
              className="px-6 py-3.5 rounded-xl text-sm font-bold text-white bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-500 hover:to-indigo-500 dark:from-[#0052FF] dark:to-[#00D2FF] shadow-lg shadow-blue-500/25 dark:shadow-cyan-500/25 hover:scale-[1.01] active:scale-[0.99] disabled:opacity-50 disabled:pointer-events-none transition-all flex items-center justify-center gap-2 cursor-pointer shrink-0"
            >
              {isLoading ? (
                <>
                  <Loader2 className="w-4 h-4 animate-spin" />
                  Shortening...
                </>
              ) : (
                <>
                  <Sparkles className="w-4 h-4 text-amber-300" />
                  Shorten Now
                  <ArrowRight className="w-4 h-4" />
                </>
              )}
            </button>
          </div>

          {/* 4 Feature Selector Tiles (Matching urlshort.dev) */}
          <div className="grid grid-cols-2 lg:grid-cols-4 gap-3 pt-2">
            {/* Tile 1: Custom Link */}
            <button
              type="button"
              onClick={() => setShowCustomAlias(!showCustomAlias)}
              className={`group flex items-center gap-3 p-3 rounded-2xl border text-left transition-all duration-200 hover:-translate-y-0.5 hover:shadow-md cursor-pointer ${
                showCustomAlias
                  ? 'border-blue-500 bg-blue-50/80 dark:border-cyan-400 dark:bg-cyan-950/30 shadow-sm'
                  : 'border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900/60 hover:border-blue-300 dark:hover:border-slate-700'
              }`}
            >
              <div
                className={`w-10 h-10 rounded-xl flex items-center justify-center shrink-0 transition-colors ${
                  showCustomAlias
                    ? 'bg-blue-600 text-white dark:bg-cyan-500 dark:text-slate-950'
                    : 'bg-blue-100 text-blue-600 dark:bg-blue-900/40 dark:text-cyan-400 group-hover:bg-blue-200 dark:group-hover:bg-blue-900/60'
                }`}
              >
                <Sliders className="w-5 h-5" />
              </div>
              <div className="min-w-0">
                <span className="block text-xs font-bold text-slate-800 dark:text-slate-100 truncate">
                  Custom Link
                </span>
                <span className="block text-[10px] text-slate-500 dark:text-slate-400 truncate">
                  {showCustomAlias ? 'Active' : 'Personalize alias'}
                </span>
              </div>
            </button>

            {/* Tile 2: Password Protection (PRO) */}
            <button
              type="button"
              onClick={() => toast.info('Password protection is available on the Pro & Enterprise plans')}
              className="group flex items-center gap-3 p-3 rounded-2xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900/60 text-left hover:border-amber-300 dark:hover:border-slate-700 hover:-translate-y-0.5 hover:shadow-md transition-all duration-200 cursor-pointer"
            >
              <div className="w-10 h-10 rounded-xl bg-amber-100 dark:bg-amber-900/40 text-amber-600 dark:text-amber-400 flex items-center justify-center shrink-0 group-hover:bg-amber-200 dark:group-hover:bg-amber-900/60 transition-colors">
                <Lock className="w-5 h-5" />
              </div>
              <div className="min-w-0">
                <div className="flex items-center gap-1.5">
                  <span className="text-xs font-bold text-slate-800 dark:text-slate-100 truncate">
                    Password
                  </span>
                  <span className="text-[9px] px-1.5 py-0.2 rounded font-bold uppercase bg-amber-500/10 text-amber-600 dark:text-amber-400">
                    PRO
                  </span>
                </div>
                <span className="block text-[10px] text-slate-500 dark:text-slate-400 truncate">
                  Protect link
                </span>
              </div>
            </button>

            {/* Tile 3: Set Expiration */}
            <button
              type="button"
              onClick={() => setShowExpiration(!showExpiration)}
              className={`group flex items-center gap-3 p-3 rounded-2xl border text-left transition-all duration-200 hover:-translate-y-0.5 hover:shadow-md cursor-pointer ${
                showExpiration
                  ? 'border-emerald-500 bg-emerald-50/80 dark:border-emerald-400 dark:bg-emerald-950/30 shadow-sm'
                  : 'border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900/60 hover:border-emerald-300 dark:hover:border-slate-700'
              }`}
            >
              <div
                className={`w-10 h-10 rounded-xl flex items-center justify-center shrink-0 transition-colors ${
                  showExpiration
                    ? 'bg-emerald-600 text-white dark:bg-emerald-500 dark:text-slate-950'
                    : 'bg-emerald-100 text-emerald-600 dark:bg-emerald-900/40 dark:text-emerald-400 group-hover:bg-emerald-200 dark:group-hover:bg-emerald-900/60'
                }`}
              >
                <Calendar className="w-5 h-5" />
              </div>
              <div className="min-w-0">
                <span className="block text-xs font-bold text-slate-800 dark:text-slate-100 truncate">
                  Expiration
                </span>
                <span className="block text-[10px] text-slate-500 dark:text-slate-400 truncate">
                  {showExpiration ? 'Active' : 'Set TTL / date'}
                </span>
              </div>
            </button>

            {/* Tile 4: Generate QR Code */}
            <button
              type="button"
              onClick={() => {
                if (recentLinks.length > 0) {
                  setActiveQrLink(recentLinks[0]);
                } else {
                  toast.info('Create a short link first to generate its QR Code!');
                }
              }}
              className="group flex items-center gap-3 p-3 rounded-2xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900/60 text-left hover:border-purple-300 dark:hover:border-slate-700 hover:-translate-y-0.5 hover:shadow-md transition-all duration-200 cursor-pointer"
            >
              <div className="w-10 h-10 rounded-xl bg-purple-100 dark:bg-purple-900/40 text-purple-600 dark:text-purple-400 flex items-center justify-center shrink-0 group-hover:bg-purple-200 dark:group-hover:bg-purple-900/60 transition-colors">
                <QrCode className="w-5 h-5" />
              </div>
              <div className="min-w-0">
                <span className="block text-xs font-bold text-slate-800 dark:text-slate-100 truncate">
                  QR Code
                </span>
                <span className="block text-[10px] text-slate-500 dark:text-slate-400 truncate">
                  Instant scan
                </span>
              </div>
            </button>
          </div>

          {/* Expandable Options: Custom Alias */}
          {showCustomAlias && (
            <div className="pt-2 border-t border-slate-200 dark:border-slate-800/80 animate-in fade-in duration-200">
              <label className="block text-xs font-medium text-slate-600 dark:text-slate-300 mb-1">
                Custom Vanity Alias
              </label>
              <div className="flex items-center rounded-xl border border-slate-300 dark:border-slate-700 bg-slate-50 dark:bg-slate-950/60 px-3 py-2 text-xs">
                <span className="text-slate-400 select-none font-mono">urlbit.in/</span>
                <input
                  type="text"
                  value={customAlias}
                  onChange={(e) => setCustomAlias(e.target.value)}
                  placeholder="my-campaign-2026"
                  className="flex-1 bg-transparent text-slate-900 dark:text-white placeholder-slate-400 focus:outline-none font-mono ml-1"
                />
              </div>
              {!isAuthenticated && (
                <p className="text-[11px] text-amber-600 dark:text-amber-400 mt-1">
                  Note: Custom vanity aliases require an authenticated account.
                </p>
              )}
            </div>
          )}

          {/* Expandable Options: Expiration Date */}
          {showExpiration && (
            <div className="pt-2 border-t border-slate-200 dark:border-slate-800/80 animate-in fade-in duration-200">
              <label className="block text-xs font-medium text-slate-600 dark:text-slate-300 mb-1">
                Link Expiration (UTC)
              </label>
              <input
                type="datetime-local"
                value={expiresAt}
                onChange={(e) => setExpiresAt(e.target.value)}
                className="w-full rounded-xl border border-slate-300 dark:border-slate-700 bg-slate-50 dark:bg-slate-950/60 px-3 py-2 text-xs text-slate-900 dark:text-white focus:outline-none"
              />
            </div>
          )}
        </form>

        {/* Guest Quota Bar */}
        {!isAuthenticated && (
          <div className="mt-4 pt-4 border-t border-slate-200 dark:border-slate-800/80 flex flex-col sm:flex-row items-center justify-between text-xs text-slate-500 dark:text-slate-400 gap-2">
            <div className="flex items-center gap-2">
              <span>Anonymous Quota:</span>
              <span className="font-mono font-bold text-blue-600 dark:text-cyan-400">
                {guestQuotaCount} / 5
              </span>
              <span>• 7-day expiration</span>
            </div>
            <Link
              to="/signup"
              className="inline-flex items-center gap-1 text-blue-600 dark:text-cyan-400 hover:underline font-medium"
            >
              <Sparkles className="w-3.5 h-3.5" />
              Sign up for unlimited links & custom aliases
            </Link>
          </div>
        )}
      </div>

      {/* Error Alert */}
      {error && <ProblemAlert error={error} onDismiss={() => setError(null)} />}

      {/* Generated Link Result Card */}
      {recentLinks.length > 0 && (
        <div className="rounded-2xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900/80 shadow-lg p-5 space-y-3">
          <div className="flex items-center justify-between">
            <h4 className="text-xs font-bold text-slate-700 dark:text-slate-300 uppercase tracking-wider">
              Generated Short Links
            </h4>
            <span className="text-[11px] text-slate-400">Active session</span>
          </div>

          <div className="divide-y divide-slate-100 dark:divide-slate-800">
            {recentLinks.map((link) => (
              <div
                key={link.id}
                className="py-3 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3"
              >
                <div className="space-y-1 min-w-0 flex-1">
                  <div className="flex items-center gap-2">
                    <a
                      href={link.shortUrl}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="font-mono font-bold text-sm text-blue-600 dark:text-cyan-400 hover:underline flex items-center gap-1"
                    >
                      {link.shortUrl}
                      <ExternalLink className="w-3 h-3 text-blue-400 dark:text-cyan-500/70" />
                    </a>
                    <span className="text-[10px] px-2 py-0.5 rounded-full bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border border-emerald-500/20 font-medium">
                      ACTIVE
                    </span>
                  </div>
                  <p className="text-xs text-slate-500 dark:text-slate-400 truncate max-w-md font-mono">
                    {link.longUrl}
                  </p>
                </div>

                <div className="flex items-center gap-2 self-end sm:self-center">
                  <button
                    onClick={() => handleCopy(link)}
                    className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium text-slate-700 dark:text-slate-300 hover:text-slate-900 dark:hover:text-white bg-slate-100 hover:bg-slate-200 dark:bg-slate-800 dark:hover:bg-slate-700 border border-slate-200 dark:border-slate-700/60 transition-colors cursor-pointer"
                  >
                    {copiedCode === link.code ? (
                      <>
                        <Check className="w-3.5 h-3.5 text-emerald-500" />
                        Copied
                      </>
                    ) : (
                      <>
                        <Copy className="w-3.5 h-3.5" />
                        Copy
                      </>
                    )}
                  </button>

                  <button
                    onClick={() => setActiveQrLink(link)}
                    className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium text-slate-700 dark:text-slate-300 hover:text-slate-900 dark:hover:text-white bg-slate-100 hover:bg-slate-200 dark:bg-slate-800 dark:hover:bg-slate-700 border border-slate-200 dark:border-slate-700/60 transition-colors cursor-pointer"
                    title="View QR Code"
                  >
                    <QrCode className="w-3.5 h-3.5 text-purple-500 dark:text-cyan-400" />
                    QR
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* QR Code Modal */}
      {activeQrLink && (
        <QrCodeModal
          isOpen={!!activeQrLink}
          onClose={() => setActiveQrLink(null)}
          url={activeQrLink.shortUrl}
          code={activeQrLink.code}
        />
      )}
    </div>
  );
};
