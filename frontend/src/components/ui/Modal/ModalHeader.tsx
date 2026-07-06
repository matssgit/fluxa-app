import { X } from "lucide-react";

export interface ModalHeaderProps {
  title: string;
  description?: string;
  onClose?: () => void;
}

export function ModalHeader({ title, description, onClose }: ModalHeaderProps) {
  return (
    <div className="flex items-start justify-between px-6 py-5 border-b border-subtle">
      <div className="space-y-1">
        <h3 className="text-lg font-semibold text-primary tracking-tight">
          {title}
        </h3>
        {description && (
          <p className="text-xs font-medium text-muted">{description}</p>
        )}
      </div>
      {onClose && (
        <button
          onClick={onClose}
          className="p-1 text-muted hover:text-primary rounded-lg transition-colors cursor-pointer"
          title="Fechar"
        >
          <X size={18} />
        </button>
      )}
    </div>
  );
}
