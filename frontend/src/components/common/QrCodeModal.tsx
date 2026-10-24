import React, { useRef } from 'react';
import { QRCodeSVG } from 'qrcode.react';
import { X, Download, Copy, Check, ExternalLink } from 'lucide-react';
import { toast } from 'sonner';

interface QrCodeModalProps {
  isOpen: boolean;
  onClose: () => void;
  url: string;
  code: string;
}

export const QrCodeModal: React.FC<QrCodeModalProps> = ({ isOpen, onClose, url, code }) => {
  const [copied, setCopied] = React.useState(false);
  const qrRef = useRef<SVGSVGElement>(null);

  if (!isOpen) return null;

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(url);
      setCopied(true);
      toast.success('Short link copied to clipboard!');
      setTimeout(() => setCopied(false), 2000);
    } catch {
      toast.error('Failed to copy to clipboard');
    }
  };

  const handleDownloadPng = () => {
    if (!qrRef.current) return;

    const svg = qrRef.current;
    const svgData = new XMLSerializer().serializeToString(svg);
    const canvas = document.createElement('canvas');
    const ctx = canvas.getContext('2d');
    const img = new Image();

    canvas.width = 1024;
    canvas.height = 1024;

    img.onload = () => {
      if (ctx) {
        ctx.fillStyle = '#FFFFFF';
        ctx.fillRect(0, 0, canvas.width, canvas.height);
        ctx.drawImage(img, 64, 64, 896, 896);

        const pngFile = canvas.toDataURL('image/png');
        const downloadLink = document.createElement('a');
        downloadLink.download = `urlbit-${code}-qr.png`;
        downloadLink.href = pngFile;
        downloadLink.click();
        toast.success('High-resolution QR code downloaded!');
      }
    };

    img.src = 'data:image/svg+xml;base64,' + btoa(unescape(encodeURIComponent(svgData)));
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 dark:bg-black/75 backdrop-blur-sm animate-fade-in">
      <div className="relative w-full max-w-sm rounded-3xl bg-white dark:bg-[#0B1528] border border-slate-200 dark:border-cyan-500/20 p-6 shadow-2xl">
        {/* Close Button */}
        <button
          onClick={onClose}
          className="absolute top-4 right-4 p-1 rounded-lg text-slate-400 hover:text-slate-700 dark:hover:text-white hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
          aria-label="Close"
        >
          <X className="w-5 h-5" />
        </button>

        {/* Modal Header */}
        <div className="text-center mb-6">
          <h3 className="text-lg font-bold text-slate-900 dark:text-white">Scan or Download QR</h3>
          <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">
            Short Code: <span className="font-mono text-blue-600 dark:text-cyan-400">/{code}</span>
          </p>
        </div>

        {/* QR Code Container */}
        <div className="flex justify-center my-4">
          <div className="p-4 bg-white rounded-2xl shadow-md border-2 border-slate-200 dark:border-cyan-500/30">
            <QRCodeSVG
              ref={qrRef}
              value={url}
              size={200}
              level="H"
              includeMargin={false}
              fgColor="#050B18"
            />
          </div>
        </div>

        {/* Short URL Box */}
        <div className="mt-5 p-2.5 rounded-xl bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-800 flex items-center justify-between gap-2">
          <span className="text-xs font-mono text-slate-800 dark:text-cyan-300 truncate">{url}</span>
          <button
            onClick={handleCopy}
            className="p-1.5 rounded-md hover:bg-slate-200 dark:hover:bg-slate-800 text-slate-500 dark:text-slate-300 transition-colors"
            title="Copy link"
          >
            {copied ? <Check className="w-4 h-4 text-emerald-500" /> : <Copy className="w-4 h-4" />}
          </button>
        </div>

        {/* Action Buttons */}
        <div className="grid grid-cols-2 gap-3 mt-5">
          <button
            onClick={handleDownloadPng}
            className="w-full flex items-center justify-center gap-2 py-2.5 px-3 rounded-xl text-xs font-bold text-white bg-blue-600 hover:bg-blue-700 dark:bg-urlbit-gradient shadow-md transition-all cursor-pointer"
          >
            <Download className="w-3.5 h-3.5" />
            Download PNG
          </button>
          <a
            href={url}
            target="_blank"
            rel="noopener noreferrer"
            className="w-full flex items-center justify-center gap-2 py-2.5 px-3 rounded-xl text-xs font-semibold text-slate-700 dark:text-slate-300 hover:text-slate-900 dark:hover:text-white bg-slate-100 hover:bg-slate-200 dark:bg-slate-800 border border-slate-200 dark:border-slate-700/60 transition-colors"
          >
            <ExternalLink className="w-3.5 h-3.5" />
            Visit URL
          </a>
        </div>
      </div>
    </div>
  );
};
