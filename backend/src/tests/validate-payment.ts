const API_URL = "http://localhost:3333";
const ACCOUNT_ID = "c1111111-1111-1111-1111-111111111111";
const TOKEN =
  "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhZTRjMzFkNC03MmFhLTQxODktYTYzNy1iNjg4MjE4YzU5N2IiLCJpYXQiOjE3ODIzMjE0MTYsImV4cCI6MTc4MjkyNjIxNn0.EZ4WmxbMVRTmGkkqDzQkqJYaTHf3XjNIVd5vd0bu_Vs";

async function testPayment() {
  try {
    const res = await fetch(`${API_URL}/credit/installments`, {
      headers: { Authorization: `Bearer ${TOKEN}` },
    });
    const data = await res.json();

    const pendingInstallment = data.installments.find(
      (i: any) => i.status === "pending",
    );

    if (!pendingInstallment) {
      throw new Error("Nenhuma parcela pendente encontrada para processar.");
    }

    console.log(
      `Processando pagamento da parcela: ${pendingInstallment.id} (Valor: R$ ${pendingInstallment.amount})`,
    );

    const payRes = await fetch(
      `${API_URL}/credit/installments/${pendingInstallment.id}/pay`,
      {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${TOKEN}`,
        },
        body: JSON.stringify({ account_id: ACCOUNT_ID }),
      },
    );

    if (payRes.status === 204) {
      console.log(
        `Pagamento processado com sucesso. Parcela ID: ${pendingInstallment.id}`,
      );
    } else {
      const errorText = await payRes.text();
      throw new Error(
        `Falha no pagamento [Status: ${payRes.status}]: ${errorText}`,
      );
    }
  } catch (error: any) {
    console.error("Erro durante a execução do teste:", error.message);
  }
}

testPayment();
