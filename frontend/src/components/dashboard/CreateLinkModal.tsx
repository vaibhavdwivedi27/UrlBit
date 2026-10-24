import React, { useState } from 'react';
import { useCreateLink } from '@/hooks/useLinks';
import { ProblemAlert } from '@/components/common/ProblemAlert';
import { X, Link2, Sparkles, Calendar, Loader2 } from 'lucide-react';
import { toast } from 'sonner';

interface CreateLinkModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export const CreateLinkModal: React.FC<CreateLinkModalProps> = ({ isOpen, onClose }) => {
  const [longUrl, setLongUrl] = useState('');
  const [customAlias, setCustomAlias] = useState('');
  const [expiresAt, setExpiresAt] = useState('');
  const [error, setError] = useState<any>(null);

  const createMutation = useCreateLink();

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!longUrl.trim()) return;

    setError(null);

    let destination = longUrl.trim();
    if (!destination.startsWith('http://') && !destination.startsWith('https://')) {
      destination = 'https://' + destination;
    }

    try {
      await createMutation.mutateAsync({
        longUrl: destination,
        customAlias: customAlias.trim() ? customAlias.trim() : null,
        expiresAt: expiresAt ? new Date(expiresAt).toISOString() : null,
      });

      toast.success('Short link created successfully!');
      onClose();
      setLongUrl('');
      setCustomAlias('');
      setExpiresAt('');
    } catch (err) {
      setError(err);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 dark:bg-black/75 backdrop-blur-sm animate-fade-in">
      <div className="relative w-full max-w-lg rounded-3xl bg-white dark:bg-[#0B1528] border border-slate-200 dark:border-cyan-500/20 p-6 sm:p-7 shadow-2xl">
        <button
          onClick={onClose}
          className="absolute top-4 right-4 p-1.5 rounded-lg text-slate-400 hover:text-slate-700 dark:hover:text-white hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
          aria-label="Close"
        >
          <X className="w-5 h-5" />
        </button>

        <div className="mb-6">
          <h3 className="text-xl font-bold text-slate-900 dark:text-white">Create New Short Link</h3>
          <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">
            Shorten URLs with custom branded vanity aliases and expiration dates.
          </p>
        </div>

        {error && <ProblemAlert error={error} className="mb-4" onDismiss={() => setError(null)} />}

        <form onSubmit={handleSubmit} className="space-y-4">
          {/* Destination URL */}
          <div>
            <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 uppercase tracking-wider mb-1.5">
              Destination URL <span className="text-rose-500">*</span>
            </label>
            <div className="relative">
              <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-slate-400 dark:text-slate-500">
                <Link2 className="w-4 h-4 text-blue-500 dark:text-cyan-400" />
              </div>
              <input
                type="text"
                value={longUrl}
                onChange={(e) => setLongUrl(e.target.value)}
                placeholder="https://example.com/my-long-target-link"
                required
                className="w-full pl-9 pr-3 py-2.5 rounded-xl bg-slate-50 dark:bg-slate-900 border border-slate-300 dark:border-slate-700/80 text-slate-900 dark:text-white placeholder-slate-400 text-sm focus:outline-none focus:border-blue-500 dark:focus:border-cyan-400 focus:ring-1 focus:ring-blue-500 dark:focus:ring-cyan-400 font-mono"
              />
            </div>
          </div>

          {/* Custom Alias */}
          <div>
            <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 uppercase tracking-wider mb-1.5">
              Custom Alias <span className="text-slate-400 font-normal">(Optional)</span>
            </label>
            <div className="flex rounded-xl bg-slate-50 dark:bg-slate-900 border border-slate-300 dark:border-slate-700/80 overflow-hidden focus-within:border-blue-500 dark:focus-within:border-cyan-400 focus-within:ring-1 focus-within:ring-blue-500 dark:focus-within:ring-cyan-400">
              <span className="inline-flex items-center px-3 text-xs font-mono text-slate-400 bg-slate-100 dark:bg-slate-950 border-r border-slate-200 dark:border-slate-800 select-none">
                urlbit.in/
              </span>
              <input
                type="text"
                value={customAlias}
                onChange={(e) => setCustomAlias(e.target.value)}
                placeholder="my-launch-link"
                className="flex-1 px-3 py-2.5 bg-transparent text-slate-900 dark:text-white placeholder-slate-400 text-sm focus:outline-none font-mono"
              />
            </div>
            <p className="text-[11px] text-slate-400 mt-1">4-32 characters: letters, numbers, hyphens, and underscores.</p>
          </div>

          {/* Expiration Date */}
          <div>
            <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 uppercase tracking-wider mb-1.5">
              Expiration Date <span className="text-slate-400 font-normal">(Optional)</span>
            </label>
            <div className="relative">
              <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-slate-400">
                <Calendar className="w-4 h-4 text-slate-400" />
              </div>
              <input
                type="datetime-local"
                value={expiresAt}
                onChange={(e) => setExpiresAt(e.target.value)}
                min={new Date().toISOString().slice(0, 16)}
                className="w-full pl-9 pr-3 py-2.5 rounded-xl bg-slate-50 dark:bg-slate-900 border border-slate-300 dark:border-slate-700/80 text-slate-900 dark:text-white placeholder-slate-400 text-sm focus:outline-none focus:border-blue-500 dark:focus:border-cyan-400 focus:ring-1 focus:ring-blue-500 dark:focus:ring-cyan-400"
              />
            </div>
            <p className="text-[11px] text-slate-400 mt-1">Link will return 404 once expired.</p>
          </div>

          {/* Action Buttons */}
          <div className="flex items-center justify-end gap-3 pt-4 border-t border-slate-200 dark:border-slate-800">
            <button
              type="button"
              onClick={onClose}
              disabled={createMutation.isPending}
              className="px-4 py-2 rounded-xl text-xs font-semibold text-slate-500 hover:text-slate-800 dark:hover:text-white hover:bg-slate-100 dark:hover:bg-slate-800/80 transition-colors"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={createMutation.isPending || !longUrl.trim()}
              className="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl text-xs font-bold text-white bg-blue-600 hover:bg-blue-700 dark:bg-urlbit-gradient shadow-md shadow-blue-500/20 dark:shadow-cyan-500/35 hover:scale-[1.02] active:scale-[0.98] disabled:opacity-50 disabled:pointer-events-none transition-all cursor-pointer"
            >
              {createMutation.isPending ? (
                <>
                  <Loader2 className="w-4 h-4 animate-spin" />
                  Creating...
                </>
              ) : (
                <>
                  <Sparkles className="w-4 h-4" />
                  Create Short Link
                </>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
