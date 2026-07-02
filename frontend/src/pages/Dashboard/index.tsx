import { useState } from "react";
import {
   ArrowUpCircle,
   ArrowDownCircle,
   DollarSign,
   TrendingUp,
   Clock,
   CreditCard,
   Receipt,
   Calendar,
} from "lucide-react";
import { useDashboard } from "../../hooks/useDashBoard";
import { PayInstallmentModal } from "../../components/PayInstallmentModal";
import { PaySubscriptionModal } from "../../components/PaySubscriptionModal";

export function Dashboard() {
   const [isPayModalOpen, setIsPayModalOpen] = useState(false);
   const [selectedInstallmentId, setSelectedInstallmentId] = useState<
      string | null
   >(null);

   const [isSubModalOpen, setIsSubModalOpen] = useState(false);
   const [selectedSubId, setSelectedSubId] = useState<string | null>(null);

   function handleOpenSubModal(subId: string) {
      setSelectedSubId(subId);
      setIsSubModalOpen(true);
   }

   // Puxando os dados reais da API (Mantido intacto)
   const { data, isLoading, isError } = useDashboard();

   if (isLoading) {
      return (
         <div className="min-h-[60vh] flex flex-col items-center justify-center gap-4">
            <div className="w-6 h-6 border-2 border-finance-secondary border-t-transparent rounded-full animate-spin"></div>
            <p className="text-sm font-medium text-finance-primary/60">
               Consolidando o seu painel financeiro...
            </p>
         </div>
      );
   }

   if (isError || !data) {
      return (
         <div className="min-h-[60vh] flex items-center justify-center p-6">
            <div className="text-finance-saida font-medium bg-finance-saida/5 px-6 py-4 rounded-2xl border border-finance-saida/10 text-sm max-w-md text-center">
               Ocorreu um erro ao carregar o painel financeiro. Por favor,
               atualize a página.
            </div>
         </div>
      );
   }

   function handleOpenPayModal(installmentId: string) {
      setSelectedInstallmentId(installmentId);
      setIsPayModalOpen(true);
   }

   return (
      <div className="w-full pb-16">
         {/* Espaçamento superior py-8 corrigido para evitar cortes no topo */}
         <main className="max-w-6xl mx-auto px-6 py-8 space-y-8">
            {/* 1. SEÇÃO DE CARDS: RESUMO + PROJEÇÃO (TEXTURA ALUMÍNIO ESCOVADO) */}
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
               {/* Card Entradas */}
               <div className="bg-premium-card p-6 rounded-2xl flex items-center justify-between transition-all duration-200 hover:-translate-y-0.5">
                  <div className="space-y-1">
                     <p className="text-[11px] font-bold text-finance-primary/50 uppercase tracking-wider">
                        Entradas do Mês
                     </p>
                     <p className="text-2xl font-bold text-finance-primary tracking-tight">
                        {new Intl.NumberFormat("pt-BR", {
                           style: "currency",
                           currency: "BRL",
                        }).format(data.summary?.totalIncome ?? 0)}
                     </p>
                  </div>
                  <div className="text-finance-entrada bg-finance-entrada/5 p-2.5 rounded-xl border border-finance-entrada/10">
                     <ArrowUpCircle size={20} />
                  </div>
               </div>

               {/* Card Saídas */}
               <div className="bg-premium-card p-6 rounded-2xl flex items-center justify-between transition-all duration-200 hover:-translate-y-0.5">
                  <div className="space-y-1">
                     <p className="text-[11px] font-bold text-finance-primary/50 uppercase tracking-wider">
                        Saídas do Mês
                     </p>
                     <p className="text-2xl font-bold text-finance-primary tracking-tight">
                        {new Intl.NumberFormat("pt-BR", {
                           style: "currency",
                           currency: "BRL",
                        }).format(data.summary?.totalExpenses ?? 0)}
                     </p>
                  </div>
                  <div className="text-finance-saida bg-finance-saida/5 p-2.5 rounded-xl border border-finance-saida/10">
                     <ArrowDownCircle size={20} />
                  </div>
               </div>

               {/* Card Saldo Atual */}
               <div className="bg-premium-card p-6 rounded-2xl flex items-center justify-between transition-all duration-200 hover:-translate-y-0.5">
                  <div className="space-y-1">
                     <p className="text-[11px] font-bold text-finance-primary/50 uppercase tracking-wider">
                        Saldo Disponível
                     </p>
                     <p className="text-2xl font-bold text-finance-primary tracking-tight">
                        {new Intl.NumberFormat("pt-BR", {
                           style: "currency",
                           currency: "BRL",
                        }).format(data.summary?.currentBalance ?? 0)}
                     </p>
                  </div>
                  <div className="text-finance-secondary bg-finance-secondary/5 p-2.5 rounded-xl border border-finance-secondary/10">
                     <DollarSign size={20} />
                  </div>
               </div>

               {/* Card Projeção Inteligente */}
               <div className="bg-premium-card p-6 rounded-2xl flex items-center justify-between transition-all duration-200 hover:-translate-y-0.5 relative overflow-hidden">
                  <div className="space-y-1 z-10">
                     <p className="text-[11px] font-bold text-finance-primary/50 uppercase tracking-wider">
                        Projeção Fim do Mês
                     </p>
                     <p className="text-2xl font-bold text-finance-primary tracking-tight">
                        {new Intl.NumberFormat("pt-BR", {
                           style: "currency",
                           currency: "BRL",
                        }).format(data.projection?.projectedBalance ?? 0)}
                     </p>
                     <p className="text-[10px] text-finance-projecao font-medium flex items-center gap-1 mt-0.5">
                        <TrendingUp size={12} /> Matemática preditiva ativa
                     </p>
                  </div>
                  <div className="text-finance-projecao bg-finance-projecao/5 p-2.5 rounded-xl border border-finance-projecao/10 z-10">
                     <Calendar size={20} />
                  </div>
               </div>
            </div>

            {/* 2. GRID CENTRAL DA TELA */}
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
               {/* COLUNA ESQUERDA: HISTÓRICO RECENTE (ESTILO MINIMALISTA NOTION/LINEAR) */}
               <div className="lg:col-span-2 bg-premium-card rounded-2xl p-6 flex flex-col">
                  <div className="mb-6">
                     <h3 className="text-base font-bold text-finance-primary">
                        Histórico Recente
                     </h3>
                     <p className="text-xs text-finance-primary/50 mt-0.5">
                        Visão unificada das movimentações de caixa e crédito
                     </p>
                  </div>

                  {/* Lista Cronológica Unificada */}
                  <div className="space-y-2 flex-1">
                     {data.timeline?.length === 0 ? (
                        <div className="text-center text-finance-primary/40 py-12 text-sm font-medium">
                           Nenhuma movimentação recente encontrada.
                        </div>
                     ) : (
                        data.timeline?.map((item) => (
                           <div
                              key={item.id}
                              className="flex items-center justify-between p-3.5 hover:bg-finance-primary/[0.015] rounded-xl border border-transparent hover:border-finance-primary/[0.03] transition-all duration-150"
                           >
                              <div className="flex items-center gap-4 min-w-0">
                                 <div
                                    className={`p-2 rounded-lg border ${
                                       item.type === "credit"
                                          ? "bg-finance-primary/5 text-finance-primary border-finance-primary/10"
                                          : "bg-finance-secondary/5 text-finance-secondary border-finance-secondary/10"
                                    }`}
                                 >
                                    {item.type === "credit" ? (
                                       <CreditCard size={16} />
                                    ) : (
                                       <Receipt size={16} />
                                    )}
                                 </div>
                                 <div className="min-w-0">
                                    <h4 className="font-semibold text-sm text-finance-primary truncate">
                                       {item.title}
                                    </h4>
                                    <p className="text-xs text-finance-primary/50 flex items-center gap-1.5 mt-0.5 truncate">
                                       <span>{item.description}</span>
                                       <span className="text-finance-primary/20">
                                          •
                                       </span>
                                       <span>
                                          {new Date(
                                             item.date?.includes("T")
                                                ? item.date
                                                : item.date + "T12:00:00",
                                          ).toLocaleDateString("pt-BR")}
                                       </span>
                                    </p>
                                 </div>
                              </div>
                              <div className="text-right pl-4">
                                 <span
                                    className={`font-bold text-sm tracking-tight ${
                                       item.type === "credit"
                                          ? "text-finance-primary"
                                          : item.cashType === "entrada"
                                            ? "text-finance-entrada"
                                            : "text-finance-saida"
                                    }`}
                                 >
                                    {item.type === "cash" &&
                                    item.cashType === "entrada"
                                       ? "+"
                                       : "-"}
                                    {new Intl.NumberFormat("pt-BR", {
                                       style: "currency",
                                       currency: "BRL",
                                    }).format(item.amount ?? 0)}
                                 </span>
                              </div>
                           </div>
                        ))
                     )}
                  </div>
               </div>

               {/* COLUNA DIREITA: COMPROMISSOS DO MÊS */}
               <div className="bg-premium-card rounded-2xl p-6 flex flex-col justify-between">
                  <div>
                     <div className="mb-6">
                        <h3 className="text-base font-bold text-finance-primary flex items-center gap-2">
                           <Clock
                              size={16}
                              className="text-finance-pendencia"
                           />{" "}
                           Compromissos do Mês
                        </h3>
                        <p className="text-xs text-finance-primary/50 mt-0.5">
                           Controle o que ainda precisa ser quitado
                        </p>
                     </div>

                     <div className="space-y-3">
                        {data.pendencies?.length === 0 ? (
                           <div className="text-center text-finance-primary/40 py-12 text-sm font-medium">
                              Tudo limpo! Nenhuma pendência para este mês. 🎉
                           </div>
                        ) : (
                           data.pendencies?.map((pend) => (
                              <div
                                 key={pend.id}
                                 className="p-4 bg-finance-primary/[0.015] rounded-xl border border-finance-primary/[0.04] flex flex-col justify-between gap-3.5"
                              >
                                 <div>
                                    <h4 className="font-semibold text-sm text-finance-primary">
                                       {pend.title}
                                    </h4>
                                    <p className="text-xs text-finance-primary/50 mt-0.5">
                                       {pend.info}
                                    </p>
                                 </div>
                                 <div className="flex items-center justify-between pt-2.5 border-t border-finance-primary/[0.06]">
                                    <span className="font-bold text-sm text-finance-primary tracking-tight">
                                       {new Intl.NumberFormat("pt-BR", {
                                          style: "currency",
                                          currency: "BRL",
                                       }).format(pend.amount ?? 0)}
                                    </span>

                                    {/* Botões usinados com efeito de clique físico metálico */}
                                    {pend.type === "installment" ? (
                                       <button
                                          onClick={() =>
                                             handleOpenPayModal(pend.id)
                                          }
                                          className="text-xs font-medium px-3 py-1.5 rounded-lg btn-premium-primary"
                                       >
                                          Pagar Parcela
                                       </button>
                                    ) : pend.type === "subscription" ? (
                                       <button
                                          onClick={() =>
                                             handleOpenSubModal(pend.id)
                                          }
                                          className="text-xs font-medium px-3 py-1.5 rounded-lg btn-premium-primary"
                                       >
                                          Baixar
                                       </button>
                                    ) : (
                                       <button className="text-xs font-medium px-3 py-1.5 rounded-lg bg-finance-primary/5 hover:bg-finance-primary/10 text-finance-primary transition-colors border border-finance-primary/10">
                                          Baixar
                                       </button>
                                    )}
                                 </div>
                              </div>
                           ))
                        )}
                     </div>
                  </div>
               </div>
            </div>
         </main>

         {/* Estrutura de Modais e gerenciadores de estado preservados */}
         <PayInstallmentModal
            isOpen={isPayModalOpen}
            onClose={() => {
               setIsPayModalOpen(false);
               setSelectedInstallmentId(null);
            }}
            installmentId={selectedInstallmentId}
         />

         <PaySubscriptionModal
            isOpen={isSubModalOpen}
            onClose={() => {
               setIsSubModalOpen(false);
               setSelectedSubId(null);
            }}
            subscriptionId={selectedSubId}
         />
      </div>
   );
}
