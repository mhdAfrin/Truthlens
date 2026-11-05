export type Account = {
  id: string;
  name: string;
  email: string;
  role: "USER" | "ADMIN";
  active: boolean;
};
