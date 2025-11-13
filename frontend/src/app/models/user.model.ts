import {Role} from './role.enum';
export interface User {
    userId: number;
    username: string;
    email?: string;
    role: Role;
}