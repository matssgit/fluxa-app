import React from "react";
import { X, Check } from "lucide-react";

export interface PickerOption<T> {
  label: string;
  value: T;
  icon?: React.ReactNode;
  subtitle?: string;
  badge?: string;
}

interface PickerModalProps<T> {
  isOpen: boolean;
  onClose: () => void;
  title: string;
  options: PickerOption<T>[];
  selectedValue: T;
  onSelect: (value: T) => void;
}

export function PickerModal<T extends string | number>({
  isOpen,
  onClose,
  title,
  options,
  selectedValue,
  onSelect,
}: PickerModalProps<T>) {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-60 flex items-center justify-center p-4 bg-page/80 backdrop-blur-md animate-fade-in">
      <div className="bg-surface rounded-3xl shadow-2xl w-full max-w-xs sm:max-w-sm max-h-[70vh] flex flex-col overflow-hidden border border-subtle/30 animate-in zoom-in-95 duration-200">
        
        {/* Cabeçalho Compacto */}
        <div className="p-4 sm:p-5 border-b border-subtle/20 flex justify-between items-center shrink-0">
          <h3 className="font-bold text-primary text-sm sm:text-base tracking-tight">{title}</h3>
          <button
            type="button"
            onClick={onClose}
            className="p-1.5 text-muted hover:text-primary hover:bg-elevated rounded-full transition-colors cursor-pointer"
          >
            <X size={16} />
          </button>
        </div>

        {/* Lista de Opções com Scroll Suave */}
        <div className="p-2 overflow-y-auto divide-y divide-subtle/10 flex-1">
          {options.map((option) => {
            const isSelected = selectedValue === option.value;
            return (
              <button
                key={String(option.value)}
                type="button"
                onClick={() => {
                  onSelect(option.value);
                  onClose();
                }}
                className={`w-full p-3.5 rounded-2xl text-left flex items-center justify-between transition-all cursor-pointer group ${
                  isSelected
                    ? "bg-brand/10 text-brand font-extrabold"
                    : "text-primary hover:bg-elevated/60 font-semibold"
                }`}
              >
                <div className="flex items-center gap-3 truncate pr-2">
                  {option.icon && (
                    <div className={`w-8 h-8 rounded-xl flex items-center justify-center shrink-0 transition-transform group-hover:scale-105 ${
                      isSelected ? "bg-brand/15 text-brand" : "bg-elevated text-muted"
                    }`}>
                      {option.icon}
                    </div>
                  )}
                  <div className="truncate">
                    <span className="text-xs sm:text-sm block truncate">{option.label}</span>
                    {option.subtitle && (
                      <span className="text-[10px] text-muted font-medium block mt-0.5 truncate">
                        {option.subtitle}
                      </span>
                    )}
                  </div>
                </div>

                <div className="flex items-center gap-2 shrink-0">
                  {option.badge && (
                    <span className="text-[9px] font-mono font-extrabold uppercase px-2 py-0.5 rounded bg-surface border border-subtle/30 text-secondary">
                      {option.badge}
                    </span>
                  )}
                  {isSelected && <Check size={16} className="text-brand stroke-3 shrink-0" />}
                </div>
              </button>
            );
          })}
        </div>

      </div>
    </div>
  );
}