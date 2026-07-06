export interface Card {
  id: string;
  name: string;
  brand: string;
  limit?: number;
  color?: string;
  is_default?: boolean;
}
