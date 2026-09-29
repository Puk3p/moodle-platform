import { Role } from '../role.enum';

/** Who is signed in. Never carries a token: the session is an HttpOnly cookie. */
export interface AuthResponse {
  userId?: string;
  email?: string;
  firstName?: string;
  lastName?: string;
  roles?: Role[];
  requiresTwoFa?: boolean;
}
