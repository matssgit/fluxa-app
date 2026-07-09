import { useState } from "react";
import { AlertTriangle } from "lucide-react";

interface CancelPurchaseModalProps {
  isOpen: boolean;
  onClose: () => void;
  onConfirm: () => Promise<void>;
}

export function CancelPurchaseModal({
  isOpen,
  onClose,
  onConfirm,
}: CancelPurchaseModalProps) {
  const [isSubmitting, setIsSubmitting] = useState(false);

  if (!isOpen) return null;

  async function handleCancel() {
    try {
      setIsSubmitting(true);
      await onConfirm();
      onClose();
    } catch (error) {
      console.error(error);
      alert("Erro ao processar o cancelamento da compra.");
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm">
      <div className="bg-white rounded-3xl w-full max-w-md shadow-2xl overflow-hidden animate-in fade-in zoom-in-95 duration-200">
        <div className="p-6 text-center border-b border-slate-100 bg-amber-50/50">
          <div className="w-12 h-12 bg-amber-100 text-amber-600 rounded-full flex items-center justify-center mx-auto mb-3">
            <AlertTriangle size={24} />
          </div>
          <h2 className="text-lg font-bold text-slate-800">Cancelar Compra</h2>
        </div>

        <div className="p-6 space-y-4">
          <p className="text-sm text-slate-600 text-center leading-relaxed">
            Você está cancelando esta compra. <br />
            As parcelas futuras serão canceladas e o limite correspondente será
            liberado. <br />
            <strong className="text-red-600">
              Esta ação não poderá ser desfeita.
            </strong>
          </p>

          <div className="flex gap-3 pt-2">
            <button
              onClick={onClose}
              disabled={isSubmitting}
              className="flex-1 px-4 py-3 text-slate-600 font-medium hover:bg-slate-100 rounded-xl transition-colors"
            >
              Voltar
            </button>
            <button
              onClick={handleCancel}
              disabled={isSubmitting}
              className="flex-1 bg-red-600 hover:bg-red-700 text-white px-4 py-3 rounded-xl font-medium flex items-center justify-center gap-2 transition-colors shadow-sm disabled:opacity-50"
            >
              {isSubmitting ? "Cancelando..." : "Confirmar Cancelamento"}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
