import type { ReactNode } from "react"; // Correção: adicionado o 'type'
import { formatCurrency } from "../utils/currency"; // Certifique-se de que este arquivo existe!
import clsx from "clsx";

interface SummaryCardProps {
   title: string;
   amount: number;
   icon: ReactNode;
   variant?: "default" | "success";
}

export function SummaryCard({
   title,
   amount,
   icon,
   variant = "default",
}: SummaryCardProps) {
   return (
      <div
         className={clsx(
            "p-6 rounded-2xl shadow-sm border",
            variant === "default"
               ? "bg-white border-slate-100"
               : "bg-emerald-500 border-emerald-600 text-white",
         )}
      >
         <header className="flex items-center justify-between">
            <span
               className={clsx(
                  "text-sm font-medium",
                  variant === "default" ? "text-slate-600" : "text-emerald-50",
               )}
            >
               {title}
            </span>
            {icon}
         </header>
         <strong
            className={clsx(
               "block mt-4 text-3xl font-bold",
               variant === "default" && "text-slate-800",
            )}
         >
            {formatCurrency(amount)}
         </strong>
      </div>
   );
}
