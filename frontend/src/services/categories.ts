import { api } from "../api/client";

export interface Category {
  id: string;
  name: string;
  type: "income" | "expense";
  color?: string;
  icon?: string;
}

interface CreateCategoryData {
  name: string;
  type: "income" | "expense";
  color?: string;
  icon?: string;
}

export async function getCategories(): Promise<Category[]> {
  const response = await api.get("/categories");
  return response.data.categories;
}

export async function createCategory(data: CreateCategoryData): Promise<void> {
  await api.post("/categories", data);
}

export async function updateCategory({
  id,
  data,
}: {
  id: string;
  data: Partial<CreateCategoryData>;
}): Promise<void> {
  await api.put(`/categories/${id}`, data);
}

export async function deleteCategory(id: string): Promise<void> {
  await api.delete(`/categories/${id}`);
}
