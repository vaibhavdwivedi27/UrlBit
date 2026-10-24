import React, { useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useLinkDetail } from '@/hooks/useLinks';
import { useLinkAnalytics } from '@/hooks/useAnalytics';
import { AnalyticsCharts } from '@/components/dashboard/AnalyticsCharts';
import { QrCodeModal } from '@/components/common/QrCodeModal';
import { ProblemAlert } from '@/components/common/ProblemAlert';
import {
  ArrowLeft,
  Copy,
  Check,
  QrCode,
  ExternalLink,
  Calendar,
  Loader2,
  RefreshCw,
  Clock,
} from 'lucide-react';
import { toast } from 'sonner';

export const AnalyticsPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const linkId = Number(id);

  const [copied, setCopied] = useState(false);
  const [isQrOpen, setIsQrOpen] = useState(false);

  const {
    data: link,
    isLoading: isLinkLoading,
    error: linkError,
  } = useLinkDetail(linkId);

  const {
    data: analytics,
    isLoading: isAnalyticsLoading,
    refetch,
    isFetching,
  } = useLinkAnalytics(linkId);

  const handleCopy = async () => {
    if (!link) return;
    try {
      await navigator.clipboard.writeText(link.shortUrl);
      setCopied(true);
      toast.success('Short link copied to clipboard!');
      setTimeout(() => setCopied(false), 2000);
    } catch {
      toast.error('Failed to copy to clipboard');
    }
  };

  if (isLinkLoading || isAnalyticsLoading) {
    return (
      <div className="min-h-[70vh] flex flex-col items-center justify-center gap-3">
        <Loader2 className="w-8 h-8 text-blue-500 dark:text-cyan-400 animate-spin" />
        <p className="text-xs text-slate-500 dark:text-slate-400">Loading real-time link analytics...</p>
      </div>
    );
  }

  if (linkError) {
    return (
      <div className="max-w-4xl mx-auto px-4 py-12 space-y-4">
        <Link
          to="/dashboard"
          className="inline-flex items-center gap-2 text-xs font-semibold text-blue-600 dark:text-cyan-400 hover:underline"
        >
          <ArrowLeft className="w-4 h-4" />
          Back to Dashboard
        </Link>
        <ProblemAlert error={linkError} />
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10 space-y-8">
      {/* Back button */}
      <div>
        <Link
          to="/dashboard"
          className="inline-flex items-center gap-2 text-xs font-semibold text-slate-500 hover:text-slate-900 dark:text-slate-400 dark:hover:text-cyan-400 transition-colors"
        >
          <ArrowLeft className="w-4 h-4" />
          Back to Dashboard
        </Link>
      </div>

      {/* Link Header Card */}
      {link && (
        <div className="p-6 rounded-3xl bg-white dark:bg-[#0B1528] border border-slate-200 dark:border-slate-800 shadow-sm space-y-4">
          <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
            <div className="space-y-1">
              <div className="flex items-center gap-3 flex-wrap">
                <h1 className="text-xl sm:text-2xl font-bold font-mono text-blue-600 dark:text-cyan-400">
                  {link.shortUrl}
                </h1>
                <span
                  className={`text-[10px] px-2.5 py-0.5 rounded-full font-semibold border ${
                    link.status === 'ACTIVE'
                      ? 'bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border-emerald-500/25'
                      : 'bg-amber-500/10 text-amber-600 dark:text-amber-400 border-amber-500/25'
                  }`}
                >
                  {link.status}
                </span>
              </div>
              <p className="text-xs text-slate-500 dark:text-slate-400 font-mono truncate max-w-2xl" title={link.longUrl}>
                {link.longUrl}
              </p>
            </div>

            {/* Actions */}
            <div className="flex items-center gap-2 shrink-0">
              <button
                onClick={() => refetch()}
                disabled={isFetching}
                className="p-2 rounded-xl bg-slate-100 hover:bg-slate-200 dark:bg-slate-900 border border-slate-200 dark:border-slate-700/80 text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white transition-colors"
                title="Refresh Analytics"
              >
                <RefreshCw className={`w-4 h-4 ${isFetching ? 'animate-spin text-blue-500 dark:text-cyan-400' : ''}`} />
              </button>

              <button
                onClick={handleCopy}
                className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl text-xs font-semibold text-slate-700 dark:text-slate-300 hover:text-slate-900 dark:hover:text-white bg-slate-100 hover:bg-slate-200 dark:bg-slate-800 dark:hover:bg-slate-700/80 border border-slate-200 dark:border-slate-700/60 transition-colors"
              >
                {copied ? (
                  <>
                    <Check className="w-4 h-4 text-emerald-500" />
                    Copied
                  </>
                ) : (
                  <>
                    <Copy className="w-4 h-4" />
                    Copy Link
                  </>
                )}
              </button>

              <button
                onClick={() => setIsQrOpen(true)}
                className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl text-xs font-semibold text-purple-700 dark:text-cyan-300 hover:text-purple-900 dark:hover:text-white bg-purple-50 hover:bg-purple-100 dark:bg-cyan-950/40 dark:hover:bg-cyan-900/60 border border-purple-200 dark:border-cyan-500/30 transition-colors"
              >
                <QrCode className="w-4 h-4" />
                QR Code
              </button>

              <a
                href={link.shortUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl text-xs font-semibold text-slate-700 dark:text-slate-300 hover:text-slate-900 dark:hover:text-white bg-slate-100 hover:bg-slate-200 dark:bg-slate-800 dark:hover:bg-slate-700/80 border border-slate-200 dark:border-slate-700/60 transition-colors"
              >
                <ExternalLink className="w-4 h-4" />
                Visit
              </a>
            </div>
          </div>

          {/* Dates metadata */}
          <div className="pt-3 border-t border-slate-100 dark:border-slate-800/80 flex flex-wrap items-center gap-4 text-xs text-slate-400 font-mono">
            <span className="flex items-center gap-1">
              <Clock className="w-3.5 h-3.5 text-slate-400" />
              Created: {new Date(link.createdAt).toLocaleDateString()}
            </span>
            {link.expiresAt && (
              <span className="flex items-center gap-1">
                <Calendar className="w-3.5 h-3.5 text-amber-500" />
                Expires: {new Date(link.expiresAt).toLocaleString()}
              </span>
            )}
          </div>
        </div>
      )}

      {/* Analytics Charts */}
      {analytics && <AnalyticsCharts analytics={analytics} />}

      {/* QR Code Modal */}
      {link && isQrOpen && (
        <QrCodeModal
          isOpen={isQrOpen}
          onClose={() => setIsQrOpen(false)}
          url={link.shortUrl}
          code={link.code}
        />
      )}
    </div>
  );
};
