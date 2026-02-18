export interface Meal {
  id: string;
  name: string;
  notes?: string;
}

export type WeekData = Record<string, Meal>; // key: "YYYY-MM-DD"
