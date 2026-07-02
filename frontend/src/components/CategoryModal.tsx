import { X } from "lucide-react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useCategories } from "../hooks/useCategories";

const categorySchema = z.object({
   name: z.string().min(2, "O nome da categoria é obrigatório"),
   type: z.enum(["income", "expense"]),
});

type CategoryForm = z.infer<typeof categorySchema>;

interface CategoryModalProps {
   isOpen: boolean;
   onClose: () => void;
}

export function CategoryModal({ isOpen, onClose }: CategoryModalProps) {
   const { createCategory, isCreating } = useCategories();

   const {
      register,
      handleSubmit,
      reset,
      formState: { errors },
   } = useForm<CategoryForm>({
      resolver: zodResolver(categorySchema),
      defaultValues: { type: "expense" }, // Já vem marcado como Despesa por padrão (gastamos mais do que ganhamos rs)
   });

   if (!isOpen) return null;

   async function onSubmit(data: CategoryForm) {
      try {
         await createCategory(data);
         reset();
         onClose();
      } catch (error) {
         console.error("Erro ao criar categoria", error);
         alert("Erro ao salvar categoria.");
      }
   }

   return (
      <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-sm p-4">
         <div className="bg-white rounded-3xl shadow-xl w-full max-w-md overflow-hidden animate-in fade-in zoom-in duration-200">
            <div className="flex justify-between items-center p-6 border-b border-slate-100">
               <h2 className="text-xl font-bold text-slate-800">
                  Nova Categoria
               </h2>
               <button
                  onClick={onClose}
                  className="text-slate-400 hover:text-slate-600 hover:bg-slate-100 p-2 rounded-full transition-colors"
               >
                  <X size={20} />
               </button>
            </div>

            <form onSubmit={handleSubmit(onSubmit)} className="p-6 space-y-5">
               {/* Seletor do Tipo (Receita/Despesa) */}
               <div className="flex gap-4 p-1 bg-slate-100 rounded-xl">
                  <label className="flex-1 cursor-pointer">
                     <input
                        type="radio"
                        value="expense"
                        {...register("type")}
                        className="peer sr-only"
                     />
                     <div className="text-center py-2 rounded-lg text-sm font-medium text-slate-500 peer-checked:bg-white peer-checked:text-red-500 peer-checked:shadow-sm transition-all">
                        Saída (Despesa)
                     </div>
                  </label>
                  <label className="flex-1 cursor-pointer">
                     <input
                        type="radio"
                        value="income"
                        {...register("type")}
                        className="peer sr-only"
                     />
                     <div className="text-center py-2 rounded-lg text-sm font-medium text-slate-500 peer-checked:bg-white peer-checked:text-emerald-500 peer-checked:shadow-sm transition-all">
                        Entrada (Receita)
                     </div>
                  </label>
               </div>

               {/* Nome da Categoria */}
               <div>
                  <label className="block text-sm font-medium text-slate-700 mb-1">
                     Nome da Categoria
                  </label>
                  <input
                     type="text"
                     placeholder="Ex: Supermercado, Salário, Lazer..."
                     {...register("name")}
                     className="w-full rounded-xl border border-slate-200 px-4 py-3 focus:border-emerald-500 focus:ring-1 focus:ring-emerald-500 outline-none bg-slate-50 focus:bg-white transition-all"
                  />
                  {errors.name && (
                     <span className="text-red-500 text-xs mt-1 block">
                        {errors.name.message}
                     </span>
                  )}
               </div>

               <div className="pt-2 flex gap-3">
                  <button
                     type="button"
                     onClick={onClose}
                     className="flex-1 px-4 py-3 rounded-xl border border-slate-200 text-slate-700 font-medium hover:bg-slate-50 transition-colors"
                  >
                     Cancelar
                  </button>
                  <button
                     type="submit"
                     disabled={isCreating}
                     className="flex-1 px-4 py-3 rounded-xl bg-emerald-500 text-white font-medium hover:bg-emerald-600 disabled:opacity-70 transition-colors"
                  >
                     {isCreating ? "Salvando..." : "Salvar"}
                  </button>
               </div>
            </form>
         </div>
      </div>
   );
}
