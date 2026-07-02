export const CARD_COLORS = [
   {
      id: "slate",
      label: "Padrão",
      gradient: "from-slate-900 to-slate-800",
      bgClass: "bg-slate-800",
   },
   {
      id: "purple",
      label: "Nubank",
      gradient: "from-purple-800 to-purple-600",
      bgClass: "bg-purple-600",
   },
   {
      id: "black",
      label: "Black",
      gradient: "from-neutral-900 to-black",
      bgClass: "bg-neutral-900",
   },
   {
      id: "orange",
      label: "Inter",
      gradient: "from-orange-600 to-orange-500",
      bgClass: "bg-orange-500",
   },
   {
      id: "green",
      label: "Verde",
      gradient: "from-emerald-700 to-emerald-500",
      bgClass: "bg-emerald-500",
   },
   {
      id: "blue",
      label: "Azul",
      gradient: "from-blue-800 to-blue-600",
      bgClass: "bg-blue-600",
   },
   {
      id: "red",
      label: "Vermelho",
      gradient: "from-rose-700 to-rose-600",
      bgClass: "bg-rose-600",
   },
];

export function getCardGradient(colorId?: string) {
   const color = CARD_COLORS.find((c) => c.id === colorId);
   return color ? color.gradient : CARD_COLORS[0].gradient;
}
