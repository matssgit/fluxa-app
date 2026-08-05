import type { Step } from "react-joyride";
import { ContextualTour } from "../../components/ui/ProductTour";

const CREDITCARDS_TOUR_KEY = "@fluxa:tour_creditcards_completed";

const DEPENDENCY_KEYS = [
  "@fluxa:tour_completed",
  "@fluxa:tour_dashboard_completed",
  "@fluxa:tour_transactions_completed",
];

export function CreditCardsTour() {
  const steps: Step[] = [
    {
      target: ".tour-credit-add-card",
      title: "Centralize seus cartões",
      content:
        "Cadastre seus cartões de crédito para unificar a visualização de limites e faturas.",
      placement: "bottom",
    },
    {
      target: ".tour-credit-summary",
      title: "Controle seus limites",
      content:
        "Visualize rapidamente o limite disponível e o valor total comprometido em suas faturas abertas.",
      placement: "bottom",
    },
    {
      target: ".tour-credit-add-purchase",
      title: "Controle as parcelas",
      content:
        "Registre compras no crédito e deixe que o Fluxa organize automaticamente suas parcelas futuras.",
      placement: "top",
    },
  ];

  return (
    <ContextualTour
      tourKey={CREDITCARDS_TOUR_KEY}
      steps={steps}
      dependencyKeys={DEPENDENCY_KEYS}
    />
  );
}
