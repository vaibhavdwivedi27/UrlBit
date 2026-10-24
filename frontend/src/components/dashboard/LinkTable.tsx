import React, { useState } from 'react';
import type { ShortUrlResponse } from '@/types/api';
import { useUpdateLink, useDeleteLink } from '@/hooks/useLinks';
import { QrCodeModal } from '@/components/common/QrCodeModal';
import {
  Copy,
  Check,
  QrCode,
  BarChart2,
  ExternalLink,
  Power,
  Trash2,
  Search,
  Calendar,
  AlertCircle,
  Loader2,
} from 'lucide-react';
import { Link } from 'react-router-dom';
import { toast } from 'sonner';

interface LinkTableProps {
  links: ShortUrlResponse[];
  isLoading: boolean;
}

export const LinkTable: React.FC<LinkTableProps> = ({ links, isLoading }) => {
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState<'ALL' | 'ACTIVE' | 'DISABLED'>('ALL');
  const [copiedCode, setCopiedCode] = useState<string | null>(null);
  const [activeQrLink, setActiveQrLink] = useState<ShortUrlResponse | null>(null);
  const [deletingId, setDeletingId] = useState<number | null>(null);

  const updateMutation = useUpdateLink();
  const deleteMutation = useDeleteLink();

  const filteredLinks = links.filter((link) => {
    const matchesSearch =
      link.code.toLowerCase().includes(search.toLowerCase()) ||
      link.longUrl.toLowerCase().includes(search.toLowerCase());
    const matchesStatus =
      statusFilter === 'ALL' || link.status === statusFilter;
    return matchesSearch && matchesStatus;
  });

  const handleCopy = async (link: ShortUrlResponse) => {
    try {
      await navigator.clipboard.writeText(link.shortUrl);
      setCopiedCode(link.code);
      toast.success('Short link copied to clipboard!');
      setTimeout(() => setCopiedCode(null), 2000);
    } catch {
      toast.error('Failed to copy to clipboard');
    }
  };

  const handleToggleStatus = async (link: ShortUrlResponse) => {
    const nextStatus = link.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE';
    try {
      await updateMutation.mutateAsync({
        id: link.id,
        request: { status: nextStatus },
      });
      toast.success(`Link status updated to ${nextStatus}`);
    } catch {
      toast.error('Failed to update status');
    }
  };

  const handleDelete = async (id: number) => {
    if (!window.confirm('Are you sure you want to delete this short link? This cannot be undone.')) {
      return;
    }
    setDeletingId(id);
    try {
      await deleteMutation.mutateAsync(id);
      toast.success('Short link deleted');
    } catch {
      toast.error('Failed to delete link');
    } finally {
      setDeletingId(null);
    }
  };

  if (isLoading) {
    return (
      <div className="py-16 flex flex-col items-center justify-center gap-3">
        <Loader2 className="w-8 h-8 text-blue-500 dark:text-cyan-400 animate-spin" />
        <p className="text-xs text-slate-500 dark:text-slate-400">Loading your short links...</p>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      {/* Search and Filters Bar */}
      <div className="flex flex-col sm:flex-row items-center justify-between gap-3">
        <div className="relative w-full sm:w-72">
          <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none" />
          <input
            type="text"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search by code or destination..."
            className="w-full pl-9 pr-3 py-2 rounded-xl bg-white dark:bg-slate-900 border border-slate-300 dark:border-slate-700/80 text-xs text-slate-900 dark:text-white placeholder-slate-400 focus:outline-none focus:border-blue-500 dark:focus:border-cyan-400"
          />
        </div>

        <div className="flex items-center gap-1.5 self-start sm:self-auto bg-slate-100 dark:bg-slate-900 p-1 rounded-xl border border-slate-200 dark:border-slate-800">
          {(['ALL', 'ACTIVE', 'DISABLED'] as const).map((s) => (
            <button
              key={s}
              onClick={() => setStatusFilter(s)}
              className={`px-3 py-1 rounded-lg text-xs font-semibold transition-all ${
                statusFilter === s
                  ? 'bg-blue-600 text-white dark:bg-cyan-500 dark:text-slate-950 shadow-sm'
                  : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white'
              }`}
            >
              {s}
            </button>
          ))}
        </div>
      </div>

      {/* Empty State */}
      {filteredLinks.length === 0 ? (
        <div className="py-16 text-center rounded-2xl bg-white dark:bg-[#0B1528] border border-slate-200 dark:border-slate-800 p-8 space-y-3">
          <AlertCircle className="w-8 h-8 text-slate-400 mx-auto" />
          <h4 className="text-sm font-semibold text-slate-900 dark:text-white">No short links found</h4>
          <p className="text-xs text-slate-500 dark:text-slate-400 max-w-sm mx-auto">
            {search || statusFilter !== 'ALL'
              ? 'Try adjusting your search query or filter to see more results.'
              : 'You have not created any short links yet. Click "Create Short Link" above to start!'}
          </p>
        </div>
      ) : (
        /* Links List */
        <div className="rounded-2xl bg-white dark:bg-[#0B1528] border border-slate-200 dark:border-slate-800 divide-y divide-slate-100 dark:divide-slate-800/80 overflow-hidden shadow-sm">
          {filteredLinks.map((link) => (
            <div
              key={link.id}
              className="p-4 sm:p-5 flex flex-col lg:flex-row items-start lg:items-center justify-between gap-4 transition-colors hover:bg-slate-50 dark:hover:bg-slate-800/30"
            >
              {/* Link Details */}
              <div className="space-y-1.5 min-w-0 flex-1">
                <div className="flex items-center gap-2.5 flex-wrap">
                  <a
                    href={link.shortUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="font-mono font-bold text-sm text-blue-600 dark:text-cyan-400 hover:underline flex items-center gap-1"
                  >
                    {link.shortUrl}
                    <ExternalLink className="w-3.5 h-3.5 text-blue-400 dark:text-cyan-500/70" />
                  </a>

                  <span
                    className={`text-[10px] px-2 py-0.5 rounded-full font-semibold border ${
                      link.status === 'ACTIVE'
                        ? 'bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border-emerald-500/25'
                        : 'bg-amber-500/10 text-amber-600 dark:text-amber-400 border-amber-500/25'
                    }`}
                  >
                    {link.status}
                  </span>

                  {link.expiresAt && (
                    <span className="text-[11px] text-slate-500 flex items-center gap-1 font-mono">
                      <Calendar className="w-3 h-3" />
                      Expires: {new Date(link.expiresAt).toLocaleDateString()}
                    </span>
                  )}
                </div>

                <p className="text-xs text-slate-500 dark:text-slate-400 truncate max-w-xl font-mono" title={link.longUrl}>
                  {link.longUrl}
                </p>

                <p className="text-[11px] text-slate-400 dark:text-slate-500">
                  Created {new Date(link.createdAt).toLocaleDateString()} at{' '}
                  {new Date(link.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                </p>
              </div>

              {/* Actions Toolbar */}
              <div className="flex items-center gap-2 self-end lg:self-center shrink-0">
                {/* Copy */}
                <button
                  onClick={() => handleCopy(link)}
                  className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold text-slate-700 dark:text-slate-300 hover:text-slate-900 dark:hover:text-white bg-slate-100 hover:bg-slate-200 dark:bg-slate-800 dark:hover:bg-slate-700/80 border border-slate-200 dark:border-slate-700/60 transition-colors"
                  title="Copy Short Link"
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

                {/* QR Code */}
                <button
                  onClick={() => setActiveQrLink(link)}
                  className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold text-slate-700 dark:text-slate-300 hover:text-slate-900 dark:hover:text-white bg-slate-100 hover:bg-slate-200 dark:bg-slate-800 dark:hover:bg-slate-700/80 border border-slate-200 dark:border-slate-700/60 transition-colors"
                  title="Generate QR Code"
                >
                  <QrCode className="w-3.5 h-3.5 text-purple-600 dark:text-cyan-400" />
                  QR
                </button>

                {/* Analytics */}
                <Link
                  to={`/dashboard/links/${link.id}`}
                  className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold text-blue-700 dark:text-cyan-300 hover:text-blue-900 dark:hover:text-white bg-blue-50 hover:bg-blue-100 dark:bg-cyan-950/40 dark:hover:bg-cyan-900/60 border border-blue-200 dark:border-cyan-500/30 transition-colors"
                  title="View Analytics"
                >
                  <BarChart2 className="w-3.5 h-3.5" />
                  Analytics
                </Link>

                {/* Status Toggle */}
                <button
                  onClick={() => handleToggleStatus(link)}
                  disabled={updateMutation.isPending}
                  className={`p-2 rounded-lg text-xs font-medium border transition-colors ${
                    link.status === 'ACTIVE'
                      ? 'text-amber-600 dark:text-amber-400 hover:bg-amber-50 dark:hover:bg-amber-950/30 border-amber-300 dark:border-amber-500/20'
                      : 'text-emerald-600 dark:text-emerald-400 hover:bg-emerald-50 dark:hover:bg-emerald-950/30 border-emerald-300 dark:border-emerald-500/20'
                  }`}
                  title={link.status === 'ACTIVE' ? 'Disable Link' : 'Enable Link'}
                >
                  <Power className="w-3.5 h-3.5" />
                </button>

                {/* Delete */}
                <button
                  onClick={() => handleDelete(link.id)}
                  disabled={deletingId === link.id}
                  className="p-2 rounded-lg text-xs font-medium text-rose-600 dark:text-rose-400 hover:bg-rose-50 dark:hover:bg-rose-950/30 border border-rose-300 dark:border-rose-500/20 transition-colors"
                  title="Delete Link"
                >
                  {deletingId === link.id ? (
                    <Loader2 className="w-3.5 h-3.5 animate-spin" />
                  ) : (
                    <Trash2 className="w-3.5 h-3.5" />
                  )}
                </button>
              </div>
            </div>
          ))}
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
