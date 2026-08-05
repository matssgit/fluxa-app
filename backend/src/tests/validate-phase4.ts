const API_URL = "http://localhost:3333";

const CATEGORY_ID = "550e8400-e29b-41d4-a716-446655440000";
const TOKEN =
  "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhZTRjMzFkNC03MmFhLTQxODktYTYzNy1iNjg4MjE4YzU5N2IiLCJpYXQiOjE3ODIzMjE0MTYsImV4cCI6MTc4MjkyNjIxNn0.EZ4WmxbMVRTmGkkqDzQkqJYaTHf3XjNIVd5vd0bu_Vs";

async function runValidation() {
  let cardId = "";

  try {
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

    if (cardRes.status !== 201) {
      throw new Error(`Falha ao criar cartão [Status: ${cardRes.status}]`);
    }

    const cardData = await cardRes.json();
    cardId = cardData.id;

    if (!cardId) {
      throw new Error("Payload de resposta inválido: ID do cartão ausente.");
    }

    const getCardsRes = await fetch(`${API_URL}/credit/cards`, {
      headers: { Authorization: `Bearer ${TOKEN}` },
    });
    const getCardsData = await getCardsRes.json();
    const cardExists = getCardsData.cards?.some((c: any) => c.id === cardId);

    if (!cardExists) {
      throw new Error(
        "Falha de persistência: Cartão recém-criado não encontrado na listagem.",
      );
    }

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

    if (purchaseRes.status !== 201) {
      throw new Error(
        `Falha ao processar compra [Status: ${purchaseRes.status}]`,
      );
    }

    const instRes = await fetch(`${API_URL}/credit/installments`, {
      headers: { Authorization: `Bearer ${TOKEN}` },
    });
    const instData = await instRes.json();

    const installments = instData.installments.slice(-3);

    if (installments.length !== 3) {
      throw new Error(
        `Inconsistência na geração de parcelas [Esperado: 3, Obtido: ${installments.length}]`,
      );
    }

    const amounts = installments.map((i: any) => Number(i.amount));
    const expectedAmounts = [33.33, 33.33, 33.34];

    if (JSON.stringify(amounts) !== JSON.stringify(expectedAmounts)) {
      throw new Error(
        `Falha no motor de parcelamento (dízima/centavos). Valores: ${amounts}`,
      );
    }

    const dates = installments.map((i: any) => i.expected_date.split("T")[0]);
    const expectedDates = ["2026-07-10", "2026-08-10", "2026-09-10"];

    if (JSON.stringify(dates) !== JSON.stringify(expectedDates)) {
      throw new Error(
        `Falha na projeção de vencimentos. Datas geradas: ${dates}`,
      );
    }

    console.log(
      "Auditoria da Fase 4 (PostgreSQL e Ecossistema de Crédito) concluída com sucesso.",
    );
  } catch (error: any) {
    console.error("Erro na auditoria da Fase 4:", error.message);
  }
}

runValidation();
