import React from 'react';
import type { UrlAnalyticsResponse } from '@/types/api';
import {
  ResponsiveContainer,
  AreaChart,
  Area,
  XAxis,
  YAxis,
  Tooltip,
  CartesianGrid,
} from 'recharts';
import { MousePointerClick, CalendarDays, Globe, TrendingUp } from 'lucide-react';

interface AnalyticsChartsProps {
  analytics: UrlAnalyticsResponse;
}

export const AnalyticsCharts: React.FC<AnalyticsChartsProps> = ({ analytics }) => {
  const { totalClicks, dailyClicks, topReferrers } = analytics;

  // Format daily clicks for chart
  const chartData = dailyClicks.map((d) => ({
    day: d.day.slice(5), // 'MM-DD'
    fullDay: d.day,
    clicks: d.clicks,
  }));

  const activeDaysCount = dailyClicks.filter((d) => d.clicks > 0).length;
  const topReferrer = topReferrers.length > 0 ? topReferrers[0].referrerHost : 'None';

  return (
    <div className="space-y-6">
      {/* KPI Summary Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        {/* Total Clicks */}
        <div className="p-5 rounded-2xl bg-white dark:bg-[#0B1528] border border-slate-200 dark:border-cyan-500/20 shadow-sm space-y-2">
          <div className="flex items-center justify-between text-slate-500 dark:text-slate-400">
            <span className="text-xs font-semibold uppercase tracking-wider">Total Clicks</span>
            <MousePointerClick className="w-4 h-4 text-blue-500 dark:text-cyan-400" />
          </div>
          <div className="flex items-baseline gap-2">
            <span className="text-3xl font-extrabold text-slate-900 dark:text-white font-mono">
              {totalClicks.toLocaleString()}
            </span>
            <span className="text-xs text-emerald-600 dark:text-emerald-400 flex items-center font-medium">
              <TrendingUp className="w-3 h-3 mr-0.5" />
              Real-time
            </span>
          </div>
          <p className="text-[11px] text-slate-400 dark:text-slate-500">Batched async aggregation via url_click_daily</p>
        </div>

        {/* Active Days */}
        <div className="p-5 rounded-2xl bg-white dark:bg-[#0B1528] border border-slate-200 dark:border-slate-800 shadow-sm space-y-2">
          <div className="flex items-center justify-between text-slate-500 dark:text-slate-400">
            <span className="text-xs font-semibold uppercase tracking-wider">Active Days</span>
            <CalendarDays className="w-4 h-4 text-blue-500 dark:text-blue-400" />
          </div>
          <div className="flex items-baseline gap-2">
            <span className="text-3xl font-extrabold text-slate-900 dark:text-white font-mono">
              {activeDaysCount}
            </span>
            <span className="text-xs text-slate-500 dark:text-slate-400 font-medium">/ 30 Days</span>
          </div>
          <p className="text-[11px] text-slate-400 dark:text-slate-500">Daily click activity recorded in UTC</p>
        </div>

        {/* Top Referrer */}
        <div className="p-5 rounded-2xl bg-white dark:bg-[#0B1528] border border-slate-200 dark:border-slate-800 shadow-sm space-y-2">
          <div className="flex items-center justify-between text-slate-500 dark:text-slate-400">
            <span className="text-xs font-semibold uppercase tracking-wider">Top Referrer</span>
            <Globe className="w-4 h-4 text-emerald-500 dark:text-emerald-400" />
          </div>
          <div className="text-2xl font-bold text-blue-600 dark:text-cyan-400 truncate font-mono" title={topReferrer}>
            {topReferrer}
          </div>
          <p className="text-[11px] text-slate-400 dark:text-slate-500">Sanitized host without sensitive query strings</p>
        </div>
      </div>

      {/* 30-Day Click Trend Chart */}
      <div className="p-6 rounded-2xl bg-white dark:bg-[#0B1528] border border-slate-200 dark:border-slate-800 shadow-sm space-y-4">
        <div className="flex items-center justify-between">
          <div>
            <h3 className="text-sm font-bold text-slate-900 dark:text-white uppercase tracking-wider">
              Click Volume Timeline
            </h3>
            <p className="text-xs text-slate-500 dark:text-slate-400">
              Daily distribution of visitor redirects over time
            </p>
          </div>
          <span className="text-[11px] font-mono text-blue-600 dark:text-cyan-400 px-2.5 py-1 rounded-md bg-blue-50 dark:bg-cyan-950/60 border border-blue-200 dark:border-cyan-500/20">
            Last 30 Days
          </span>
        </div>

        <div className="h-64 w-full pt-4">
          {chartData.length === 0 ? (
            <div className="h-full flex items-center justify-center text-xs text-slate-400">
              No click events recorded yet. Share your short link to view live analytics!
            </div>
          ) : (
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={chartData} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
                <defs>
                  <linearGradient id="clickGradient" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#0052FF" stopOpacity={0.4} />
                    <stop offset="95%" stopColor="#00D2FF" stopOpacity={0.0} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" className="dark:stroke-slate-800" vertical={false} />
                <XAxis dataKey="day" stroke="#94a3b8" fontSize={11} tickLine={false} />
                <YAxis stroke="#94a3b8" fontSize={11} tickLine={false} allowDecimals={false} />
                <Tooltip
                  content={({ active, payload }) => {
                    if (active && payload && payload.length) {
                      const data = payload[0].payload;
                      return (
                        <div className="bg-white dark:bg-[#0B1528] border border-slate-200 dark:border-cyan-500/40 p-2.5 rounded-xl shadow-xl text-xs space-y-1">
                          <p className="font-semibold text-slate-900 dark:text-white">{data.fullDay}</p>
                          <p className="font-mono text-blue-600 dark:text-cyan-400">
                            Clicks: <span className="font-bold">{data.clicks}</span>
                          </p>
                        </div>
                      );
                    }
                    return null;
                  }}
                />
                <Area
                  type="monotone"
                  dataKey="clicks"
                  stroke="#0052FF"
                  strokeWidth={2.5}
                  fillOpacity={1}
                  fill="url(#clickGradient)"
                />
              </AreaChart>
            </ResponsiveContainer>
          )}
        </div>
      </div>

      {/* Top Referring Domains */}
      <div className="p-6 rounded-2xl bg-white dark:bg-[#0B1528] border border-slate-200 dark:border-slate-800 shadow-sm space-y-4">
        <h3 className="text-sm font-bold text-slate-900 dark:text-white uppercase tracking-wider">
          Top Referring Sources
        </h3>
        {topReferrers.length === 0 ? (
          <p className="text-xs text-slate-400 py-4">No referrer sources recorded yet.</p>
        ) : (
          <div className="divide-y divide-slate-100 dark:divide-slate-800/80">
            {topReferrers.map((ref, idx) => {
              const percentage = totalClicks > 0 ? Math.round((ref.clicks / totalClicks) * 100) : 0;
              return (
                <div key={idx} className="py-3 flex items-center justify-between gap-4">
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center justify-between text-xs mb-1.5">
                      <span className="font-mono text-slate-800 dark:text-slate-200 font-medium truncate">
                        {ref.referrerHost}
                      </span>
                      <span className="font-mono text-blue-600 dark:text-cyan-400 font-bold ml-2">
                        {ref.clicks} clicks ({percentage}%)
                      </span>
                    </div>
                    {/* Progress Bar */}
                    <div className="w-full h-1.5 bg-slate-100 dark:bg-slate-900 rounded-full overflow-hidden">
                      <div
                        className="h-full bg-blue-600 dark:bg-urlbit-gradient rounded-full transition-all duration-500"
                        style={{ width: `${Math.max(percentage, 3)}%` }}
                      />
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
};
