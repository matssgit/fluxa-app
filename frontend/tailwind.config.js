/** @type {import('tailwindcss').Config} */
export default {
   content: ["./index.html", "./src/**/*.{js,ts,jsx,tsx}"],
   theme: {
      extend: {
         colors: {
            finance: {
               bg: "#051f20",
               sidebar: "#0b2b26",
               card: "#163832",
               active: "#235347",
               highlight: "#8eb69b",
               gold: "#C6A969",
               text: "#daf1de",
               entrada: "#8eb69b",
               saida: "#c45c5c",
               pendencia: "#d8b65b",
               projecao: "#6ca6ff",
            },
         },
         boxShadow: {
            premium: "0 8px 24px rgba(0,0,0,.28)",
            innerGlow: "inset 0 1px rgba(255,255,255,.08)",
            hibrida:
               "inset 0 1px rgba(255,255,255,.08), 0 8px 24px rgba(0,0,0,.28)",
         },
         borderColor: {
            subtle: "rgba(255,255,255,.05)",
         },
      },
   },
   plugins: [],
};
