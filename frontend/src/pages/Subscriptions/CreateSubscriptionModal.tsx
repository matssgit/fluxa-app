import { X } from "lucide-react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { useCreateSubscription } from "../../hooks/useSubscriptions";
import { useAccounts } from "../../hooks/useAccounts";
import { useCategories } from "../../hooks/useCategories";
import { useCards } from "../../hooks/useCredit";

// Interface para garantir a tipagem dos dados vindos dos hooks
interface Category {
   id: string;
   name: string;
   type: "income" | "expense";
}

interface Account {
   id: string;
   name: string;
}

interface Card {
   id: string;
   name: string;
}

const subscriptionSchema = z.object({
   title: z.string().min(1, "O nome é obrigatório"),
   amount: z.number().min(0.01, "O valor deve ser maior que zero"),
   due_day: z.number().min(1, "Dia inválido").max(31, "Dia inválido"),
   frequency: z.enum(["monthly", "yearly"]),
   category_id: z.string().uuid("Selecione uma categoria"),
   payment_method: z.enum(["account", "card"]),
   account_id: z.string().optional(),
   card_id: z.string().optional(),
});

type SubscriptionForm = z.infer<typeof subscriptionSchema>;

interface Props {
   isOpen: boolean;
   onClose: () => void;
}

export function CreateSubscriptionModal({ isOpen, onClose }: Props) {
   const { mutateAsync: createSub, isPending } = useCreateSubscription();
   const { accounts } = useAccounts();
   const { categories } = useCategories();
   const { data: cards } = useCards();

   const {
      register,
      handleSubmit,
      watch,
      reset,
      formState: { errors },
   } = useForm<SubscriptionForm>({
      resolver: zodResolver(subscriptionSchema),
      defaultValues: { frequency: "monthly", payment_method: "account" },
   });

   const paymentMethod = watch("payment_method");

   // Tipagem explícita aqui elimina o 'any'
   const filteredCategories =
      (categories as Category[])?.filter((c) => c.type === "expense") || [];

   if (!isOpen) return null;

   async function onSubmit(data: SubscriptionForm) {
      try {
         await createSub({
            title: data.title,
            amount: data.amount,
            due_day: data.due_day,
            frequency: data.frequency,
            category_id: data.category_id,
            account_id:
               data.payment_method === "account" ? data.account_id : undefined,
            card_id: data.payment_method === "card" ? data.card_id : undefined,
         });
         reset();
         onClose();
      } catch (error) {
         console.error("Erro ao criar assinatura", error);
      }
   }

   return (
      <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
         <div
            className="absolute inset-0 bg-finance-primary/20 backdrop-blur-sm"
            onClick={onClose}
         />

         <div className="bg-premium-card w-full max-w-lg rounded-3xl shadow-premium border border-finance-primary/10 overflow-hidden z-10">
            <div className="p-6 border-b border-finance-primary/5 flex justify-between items-center">
               <h2 className="text-lg font-bold text-finance-primary">
                  Nova Assinatura
               </h2>
               <button
                  onClick={onClose}
                  className="p-2 text-finance-primary/40 hover:text-finance-primary transition-colors"
               >
                  <X size={20} />
               </button>
            </div>

            <form onSubmit={handleSubmit(onSubmit)} className="p-6 space-y-5">
               <div>
                  <label className="block text-[11px] font-bold text-finance-primary/60 uppercase tracking-widest mb-1.5">
                     Nome do Serviço
                  </label>
                  <input
                     {...register("title")}
                     className="input-premium w-full px-4 py-3 rounded-xl outline-none"
                  />
                  {/* Uso explícito do erro para silenciar o linter */}
                  {errors.title && (
                     <span className="text-finance-saida text-[10px] mt-1 block font-bold">
                        {errors.title.message}
                     </span>
                  )}
               </div>

               <div className="grid grid-cols-2 gap-4">
                  <div>
                     <label className="block text-[11px] font-bold text-finance-primary/60 uppercase tracking-widest mb-1.5">
                        Valor
                     </label>
                     <input
                        type="number"
                        {...register("amount", { valueAsNumber: true })}
                        className="input-premium w-full px-4 py-3 rounded-xl outline-none"
                     />
                  </div>
                  <div>
                     <label className="block text-[11px] font-bold text-finance-primary/60 uppercase tracking-widest mb-1.5">
                        Vencimento (Dia)
                     </label>
                     <input
                        type="number"
                        {...register("due_day", { valueAsNumber: true })}
                        className="input-premium w-full px-4 py-3 rounded-xl outline-none"
                     />
                  </div>
               </div>

               <div>
                  <label className="block text-[11px] font-bold text-finance-primary/60 uppercase tracking-widest mb-1.5">
                     Categoria
                  </label>
                  <select
                     {...register("category_id")}
                     className="input-premium w-full px-4 py-3 rounded-xl outline-none cursor-pointer"
                  >
                     <option value="">Selecione...</option>
                     {filteredCategories.map((c: Category) => (
                        <option key={c.id} value={c.id}>
                           {c.name}
                        </option>
                     ))}
                  </select>
               </div>

               <div className="pt-2 border-t border-finance-primary/5">
                  <label className="block text-[11px] font-bold text-finance-primary/60 uppercase tracking-widest mb-3">
                     Cobrança
                  </label>
                  <div className="grid grid-cols-2 gap-4">
                     <label className="flex items-center gap-2 cursor-pointer input-premium px-4 py-3 rounded-xl">
                        <input
                           type="radio"
                           value="account"
                           {...register("payment_method")}
                           className="accent-finance-secondary"
                        />
                        <span className="text-sm font-medium text-finance-primary">
                           Conta
                        </span>
                     </label>
                     <label className="flex items-center gap-2 cursor-pointer input-premium px-4 py-3 rounded-xl">
                        <input
                           type="radio"
                           value="card"
                           {...register("payment_method")}
                           className="accent-finance-secondary"
                        />
                        <span className="text-sm font-medium text-finance-primary">
                           Cartão
                        </span>
                     </label>
                  </div>

                  <div className="mt-3">
                     {paymentMethod === "account" ? (
                        <select
                           {...register("account_id")}
                           className="input-premium w-full px-4 py-3 rounded-xl outline-none"
                        >
                           <option value="">Selecione a conta...</option>
                           {(accounts as Account[])?.map((acc) => (
                              <option key={acc.id} value={acc.id}>
                                 {acc.name}
                              </option>
                           ))}
                        </select>
                     ) : (
                        <select
                           {...register("card_id")}
                           className="input-premium w-full px-4 py-3 rounded-xl outline-none"
                        >
                           <option value="">Selecione o cartão...</option>
                           {(cards as Card[])?.map((card) => (
                              <option key={card.id} value={card.id}>
                                 {card.name}
                              </option>
                           ))}
                        </select>
                     )}
                  </div>
               </div>

               <button
                  type="submit"
                  disabled={isPending}
                  className="btn-premium-primary w-full py-3 rounded-xl text-sm mt-2"
               >
                  {isPending ? "Salvando..." : "Salvar Assinatura"}
               </button>
            </form>
         </div>
      </div>
   );
}
