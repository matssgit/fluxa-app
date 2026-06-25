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
