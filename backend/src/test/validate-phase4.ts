// Comando para executar: npx tsx test/validate-phase4.ts
// O servidor (npm run dev) precisa estar rodando em outro terminal.

const API_URL = "http://localhost:3333";

// === ATENÇÃO: PREENCHER ESTES DOIS DADOS ANTES DE RODAR ===
const TOKEN =
   "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhZTRjMzFkNC03MmFhLTQxODktYTYzNy1iNjg4MjE4YzU5N2IiLCJpYXQiOjE3ODIzMjE0MTYsImV4cCI6MTc4MjkyNjIxNn0.EZ4WmxbMVRTmGkkqDzQkqJYaTHf3XjNIVd5vd0bu_Vs";
const CATEGORY_ID = "550e8400-e29b-41d4-a716-446655440000";

async function runValidation() {
   console.log("INICIANDO AUDITORIA DA FASE 4 (POSTGRESQL)...\n");
   let cardId = "";
   let purchaseId = "";

   try {
      // 1. VALIDAR CRIAÇÃO DE CARTÃO
      console.log("[1/5] Testando POST /credit/cards...");
      const cardRes = await fetch(`${API_URL}/credit/cards`, {
         method: "POST",
         headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${TOKEN}`,
         },
         body: JSON.stringify({
            name: "Cartão Auditoria",
            brand: "Mastercard",
            limit_amount: 5000,
            due_day: 10,
         }),
      });

      if (cardRes.status !== 201)
         throw new Error(`Falha ao criar cartão. Status: ${cardRes.status}`);
      const cardData = await cardRes.json();
      cardId = cardData.id;
      if (!cardId)
         throw new Error("JSON do cartão não retornou o ID no Postman/Fetch.");
      console.log("✅ Cartão criado e persistido. ID:", cardId);

      // 2. VALIDAR LISTAGEM DE CARTÕES
      console.log("\n[2/5] Testando GET /credit/cards...");
      const getCardsRes = await fetch(`${API_URL}/credit/cards`, {
         headers: { Authorization: `Bearer ${TOKEN}` },
      });
      const getCardsData = await getCardsRes.json();
      const cardExists = getCardsData.cards?.some((c: any) => c.id === cardId);
      if (!cardExists)
         throw new Error("Cartão criado não apareceu na listagem.");
      console.log("✅ Listagem de cartões validada.");

      // 3. VALIDAR COMPRA PARCELADA (O CÉREBRO)
      console.log(
         "\n[3/5] Testando POST /credit/purchases (R$ 100,00 em 3x)...",
      );
      const purchaseRes = await fetch(`${API_URL}/credit/purchases`, {
         method: "POST",
         headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${TOKEN}`,
         },
         body: JSON.stringify({
            card_id: cardId,
            category_id: CATEGORY_ID,
            title: "Auditoria 100 em 3x",
            store: "Test Store",
            total_amount: 100.0,
            total_installments: 3,
            purchase_date: "2026-06-24",
         }),
      });

      if (purchaseRes.status !== 201)
         throw new Error(`Falha na compra. Status: ${purchaseRes.status}`);
      console.log("✅ Transação ACID da compra retornou 201 Created.");

      // 4. VALIDAR PARCELAS GERADAS (MATEMÁTICA E DATAS)
      console.log(
         "\n[4/5] Testando GET /installments e validando matemática...",
      );
      const instRes = await fetch(`${API_URL}/credit/installments`, {
         headers: { Authorization: `Bearer ${TOKEN}` },
      });
      const instData = await instRes.json();

      // Filtra as parcelas dessa compra específica pelo nome da loja/título (já que não salvei purchaseId no retorno padrão)
      // Como o DB está isolado para o user, peguei as 3 últimas criadas
      const installments = instData.installments.slice(-3);

      if (installments.length !== 3)
         throw new Error(
            `Foram geradas ${installments.length} parcelas. Esperado: 3.`,
         );
      console.log("✅ Quantidade de parcelas geradas: 3");

      const amounts = installments.map((i: any) => Number(i.amount));
      const expectedAmounts = [33.33, 33.33, 33.34];
      const matchAmounts =
         JSON.stringify(amounts) === JSON.stringify(expectedAmounts);

      if (!matchAmounts) {
         throw new Error(
            `Erro na matemática. Valores gerados: ${amounts}. Esperado: ${expectedAmounts}`,
         );
      }
      console.log(
         "✅ Tratamento de dízimas e centavos residual validado (33.33, 33.33, 33.34).",
      );

      const dates = installments.map((i: any) => i.expected_date.split("T")[0]);
      const expectedDates = ["2026-07-10", "2026-08-10", "2026-09-10"];
      const matchDates =
         JSON.stringify(dates) === JSON.stringify(expectedDates);

      if (!matchDates) {
         throw new Error(
            `Erro de projeção de datas. Datas geradas: ${dates}. Esperado: ${expectedDates}`,
         );
      }
      console.log(
         "✅ Projeção de due_day validada. Vencimentos cravados no dia 10.",
      );

      console.log("\n[5/5] VALIDAÇÃO CONCLUÍDA COM SUCESSO.");
      console.log(
         "O núcleo do PostgreSQL está íntegro para seguir com as alterações de pagamento.",
      );
   } catch (error: any) {
      console.error("\n❌ FALHA NA VALIDAÇÃO:", error.message);
      console.log("Interrompendo testes. Forneça este log para análise.");
   }
}

runValidation();
