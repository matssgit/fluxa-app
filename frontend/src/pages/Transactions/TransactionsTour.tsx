import type { Step } from "react-joyride";
import { ContextualTour } from "../../components/ui/ProductTour";

const TRANSACTIONS_TOUR_KEY = "@fluxa:tour_transactions_completed";

const DEPENDENCY_KEYS = [
  "@fluxa:tour_completed",
  "@fluxa:tour_dashboard_completed",
];

export function TransactionsTour() {
  const steps: Step[] = [
    {
      target: ".tour-transactions-add",
      title: "Registre suas movimentações",
      content:
        "Adicione rapidamente suas receitas e despesas para manter o fluxo de caixa sempre atualizado.",
      placement: "bottom",
    },
    {
      target: ".tour-transactions-filters",
      title: "Encontre o que precisa",
      content:
        "Filtre suas movimentações por período, categoria ou conta para análises mais precisas.",
      placement: "bottom",
    },
    {
      target: ".tour-transactions-list",
      title: "Histórico detalhado",
      content: "Acompanhe o status e os detalhes de cada evento financeiro.",
      placement: "top",
    },
  ];

  return (
    <ContextualTour
      tourKey={TRANSACTIONS_TOUR_KEY}
      steps={steps}
      dependencyKeys={DEPENDENCY_KEYS}
    />
  );
}
