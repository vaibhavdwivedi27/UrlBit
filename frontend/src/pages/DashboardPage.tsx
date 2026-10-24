import React, { useState } from 'react';
import { useAuth } from '@/hooks/useAuth';
import { useLinksList } from '@/hooks/useLinks';
import { LinkTable } from '@/components/dashboard/LinkTable';
import { CreateLinkModal } from '@/components/dashboard/CreateLinkModal';
import { Plus, Link2, CheckCircle, PowerOff, Sparkles, RefreshCw } from 'lucide-react';

export const DashboardPage: React.FC = () => {
  const { user } = useAuth();
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [page] = useState(0);

  const { data: pageData, isLoading, refetch, isFetching } = useLinksList(page, 50);

  const links = pageData?.content || [];
  const totalElements = pageData?.totalElements || 0;
  const activeCount = links.filter((l) => l.status === 'ACTIVE').length;
  const disabledCount = links.filter((l) => l.status === 'DISABLED').length;

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10 space-y-8">
      {/* Top Welcome & Actions Header */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-900 dark:text-white tracking-tight">
              Links Dashboard
            </h1>
            <span className="text-xs px-2.5 py-0.5 rounded-full bg-blue-50 dark:bg-cyan-950/60 text-blue-700 dark:text-cyan-300 border border-blue-200 dark:border-cyan-500/30 font-medium">
              {user?.role || 'USER'}
            </span>
          </div>
          <p className="text-xs sm:text-sm text-slate-500 dark:text-slate-400 mt-1">
            Manage your branded short URLs, toggle statuses, and inspect real-time click metrics.
          </p>
        </div>

        <div className="flex items-center gap-2.5">
          <button
            onClick={() => refetch()}
            disabled={isFetching}
            className="p-2.5 rounded-xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-700/80 text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white transition-colors"
            title="Refresh links"
          >
            <RefreshCw className={`w-4 h-4 ${isFetching ? 'animate-spin text-blue-500 dark:text-cyan-400' : ''}`} />
          </button>

          <button
            onClick={() => setIsCreateOpen(true)}
            className="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl text-xs sm:text-sm font-bold text-white bg-blue-600 hover:bg-blue-700 dark:bg-urlbit-gradient shadow-md shadow-blue-500/20 dark:shadow-cyan-500/35 hover:scale-[1.02] active:scale-[0.98] transition-all cursor-pointer"
          >
            <Plus className="w-4 h-4" />
            Create Short Link
          </button>
        </div>
      </div>

      {/* KPI Stats Bar */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        {/* Total Links */}
        <div className="p-4 sm:p-5 rounded-2xl bg-white dark:bg-[#0B1528] border border-slate-200 dark:border-slate-800 shadow-sm flex items-center justify-between">
          <div>
            <p className="text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider">
              Total Links
            </p>
            <p className="text-2xl sm:text-3xl font-extrabold text-slate-900 dark:text-white font-mono mt-1">
              {totalElements}
            </p>
          </div>
          <div className="w-10 h-10 rounded-xl bg-blue-500/10 border border-blue-500/20 flex items-center justify-center">
            <Link2 className="w-5 h-5 text-blue-500 dark:text-blue-400" />
          </div>
        </div>

        {/* Active Links */}
        <div className="p-4 sm:p-5 rounded-2xl bg-white dark:bg-[#0B1528] border border-slate-200 dark:border-emerald-500/20 shadow-sm flex items-center justify-between">
          <div>
            <p className="text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider">
              Active Links
            </p>
            <p className="text-2xl sm:text-3xl font-extrabold text-emerald-600 dark:text-emerald-400 font-mono mt-1">
              {activeCount}
            </p>
          </div>
          <div className="w-10 h-10 rounded-xl bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center">
            <CheckCircle className="w-5 h-5 text-emerald-600 dark:text-emerald-400" />
          </div>
        </div>

        {/* Disabled Links */}
        <div className="p-4 sm:p-5 rounded-2xl bg-white dark:bg-[#0B1528] border border-slate-200 dark:border-amber-500/20 shadow-sm flex items-center justify-between">
          <div>
            <p className="text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider">
              Disabled Links
            </p>
            <p className="text-2xl sm:text-3xl font-extrabold text-amber-600 dark:text-amber-400 font-mono mt-1">
              {disabledCount}
            </p>
          </div>
          <div className="w-10 h-10 rounded-xl bg-amber-500/10 border border-amber-500/20 flex items-center justify-center">
            <PowerOff className="w-5 h-5 text-amber-600 dark:text-amber-400" />
          </div>
        </div>
      </div>

      {/* Links List Table */}
      <div className="space-y-4">
        <h2 className="text-base font-bold text-slate-900 dark:text-white flex items-center gap-2">
          <Sparkles className="w-4 h-4 text-blue-500 dark:text-cyan-400" />
          All Created Short Links
        </h2>
        <LinkTable links={links} isLoading={isLoading} />
      </div>

      {/* Create Link Modal */}
      <CreateLinkModal isOpen={isCreateOpen} onClose={() => setIsCreateOpen(false)} />
    </div>
  );
};
