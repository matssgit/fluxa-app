import { useState } from "react";
import { AlertTriangle, Trash2 } from "lucide-react";

interface DeleteActionModalProps {
   isOpen: boolean;
   onClose: () => void;
   onConfirm: () => Promise<void>;
   title: string;
   description: string;
   warningText?: string;
}

export function DeleteActionModal({
   isOpen,
   onClose,
   onConfirm,
   title,
   description,
   warningText,
}: DeleteActionModalProps) {
   const [confirmText, setConfirmText] = useState("");
   const [isSubmitting, setIsSubmitting] = useState(false);

   if (!isOpen) return null;

   // Valida se o usuário digitou exatamente a palavra de confirmação
   const isTypingCorrect = confirmText === "EXCLUIR";

   async function handleConfirm() {
      if (!isTypingCorrect) return;
      try {
         setIsSubmitting(true);
         await onConfirm();
         setConfirmText(""); // Limpa o input
         onClose();
      } catch (error) {
         const err = error as { response?: { data?: { message?: string } } };
         alert(err.response?.data?.message || "Ocorreu um erro ao excluir.");
      } finally {
         setIsSubmitting(false);
      }
   }

   return (
      <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/70 backdrop-blur-sm">
         <div className="bg-white rounded-3xl w-full max-w-md shadow-2xl overflow-hidden animate-in fade-in zoom-in-95 duration-200">
            {/* Topo do Modal com Alerta */}
            <div className="p-6 text-center border-b border-slate-100 bg-red-50/50">
               <div className="w-16 h-16 bg-red-100 text-red-600 rounded-full flex items-center justify-center mx-auto mb-4">
                  <AlertTriangle size={32} />
               </div>
               <h2 className="text-xl font-bold text-slate-800">{title}</h2>
               <p className="text-sm text-slate-600 mt-2">{description}</p>
            </div>

            <div className="p-6">
               {/* Texto de aviso extra (se houver) */}
               {warningText && (
                  <div className="bg-amber-50 text-amber-700 text-xs p-4 rounded-xl font-medium mb-6">
                     {warningText}
                  </div>
               )}

               {/* Campo de confirmação estilo GitHub */}
               <label className="block text-sm font-medium text-slate-700 mb-2">
                  Para confirmar, digite{" "}
                  <strong className="text-red-600 select-none">EXCLUIR</strong>{" "}
                  abaixo:
               </label>
               <input
                  type="text"
                  placeholder="EXCLUIR"
                  value={confirmText}
                  onChange={(e) => setConfirmText(e.target.value)}
                  className="w-full px-4 py-3 bg-slate-50 border border-slate-200 rounded-xl focus:ring-2 focus:ring-red-600/20 focus:border-red-600 outline-none text-center font-bold uppercase tracking-widest transition-all"
               />

               {/* Botões de Ação */}
               <div className="flex gap-3 mt-6">
                  <button
                     onClick={() => {
                        setConfirmText("");
                        onClose();
                     }}
                     className="flex-1 px-4 py-3 text-slate-600 font-medium hover:bg-slate-100 rounded-xl transition-colors"
                  >
                     Cancelar
                  </button>
                  <button
                     onClick={handleConfirm}
                     disabled={!isTypingCorrect || isSubmitting}
                     className="flex-1 bg-red-600 hover:bg-red-700 text-white px-4 py-3 rounded-xl font-medium flex items-center justify-center gap-2 transition-colors disabled:opacity-50 disabled:cursor-not-allowed shadow-sm"
                  >
                     {isSubmitting ? (
                        <span className="animate-pulse">Excluindo...</span>
                     ) : (
                        <>
                           <Trash2 size={18} /> Confirmar
                        </>
                     )}
                  </button>
               </div>
            </div>
         </div>
      </div>
   );
}
