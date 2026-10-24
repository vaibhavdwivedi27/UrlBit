import React from 'react';
import { ApiError } from '@/api/client';
import { AlertCircle, AlertTriangle, ShieldAlert, Sparkles } from 'lucide-react';
import { Link } from 'react-router-dom';

interface ProblemAlertProps {
  error: unknown;
  className?: string;
  onDismiss?: () => void;
}

export const ProblemAlert: React.FC<ProblemAlertProps> = ({ error, className = '', onDismiss }) => {
  if (!error) return null;

  let title = 'An error occurred';
  let detail = 'Please check your input and try again.';
  let errors: string[] | undefined;
  let isQuota = false;
  let isAlias = false;

  if (error instanceof ApiError) {
    title = error.problem?.title || error.message;
    detail = error.problem?.detail || error.message;
    errors = error.problem?.errors;
    isQuota = error.isQuotaExceeded;
    isAlias = error.isAliasTaken;
  } else if (error instanceof Error) {
    detail = error.message;
  }

  return (
    <div
      className={`rounded-xl border p-4 text-sm transition-all ${
        isQuota
          ? 'bg-amber-950/40 border-amber-500/40 text-amber-200'
          : 'bg-rose-950/40 border-rose-500/30 text-rose-200'
      } ${className}`}
    >
      <div className="flex items-start gap-3">
        {isQuota ? (
          <AlertTriangle className="w-5 h-5 text-amber-400 shrink-0 mt-0.5" />
        ) : isAlias ? (
          <ShieldAlert className="w-5 h-5 text-rose-400 shrink-0 mt-0.5" />
        ) : (
          <AlertCircle className="w-5 h-5 text-rose-400 shrink-0 mt-0.5" />
        )}

        <div className="flex-1 space-y-1">
          <h4 className="font-semibold text-white">{title}</h4>
          <p className="text-xs leading-relaxed opacity-90">{detail}</p>

          {/* Detailed field errors if validation failed */}
          {errors && errors.length > 0 && (
            <ul className="list-disc list-inside mt-2 space-y-0.5 text-xs text-rose-300">
              {errors.map((err, i) => (
                <li key={i}>{err}</li>
              ))}
            </ul>
          )}

          {/* Quick CTA if guest quota was exceeded */}
          {isQuota && (
            <div className="pt-2">
              <Link
                to="/signup"
                className="inline-flex items-center gap-1.5 px-3 py-1 rounded-md text-xs font-semibold bg-amber-400 hover:bg-amber-300 text-slate-950 transition-colors"
              >
                <Sparkles className="w-3.5 h-3.5" />
                Create Free Account for Unlimited Links
              </Link>
            </div>
          )}
        </div>

        {onDismiss && (
          <button
            onClick={onDismiss}
            className="text-xs opacity-60 hover:opacity-100 transition-opacity"
          >
            ✕
          </button>
        )}
      </div>
    </div>
  );
};
