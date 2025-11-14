import {Role} from './role.enum';
export interface User {
    userId: string;
    email?: string;
    firstName: string;
    lastName: string;
    roles: Role[]; //TODO: implement in backend Set<Role> roles in AuthResponse.java
}