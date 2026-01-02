import { Role } from "../role.enum";

export interface AuthResponse {
  token?: string;
  accessToken?: string;
  userId?: string;
  email?: string;
  firstName?: string;
  lastName?: string;
  roles?: Role[];
  requiresTwoFa?: boolean;
}